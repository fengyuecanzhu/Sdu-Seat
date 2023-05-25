/*
 * This file is part of Sdu-Seat
 * Sdu-Seat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sdu-Seat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sdu-Seat.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C)  2022~2023 fengyuecanzhu
 */

@file:JvmName("Main")

package me.fycz.sduseat

import me.fycz.sduseat.api.*
import me.fycz.sduseat.bean.AreaBean
import me.fycz.sduseat.bean.Config
import me.fycz.sduseat.bean.PeriodBean
import me.fycz.sduseat.bean.SeatBean
import me.fycz.sduseat.constant.Const
import me.fycz.sduseat.constant.Const.ONE_DAY
import me.fycz.sduseat.constant.Const.dateFormat
import me.fycz.sduseat.constant.Const.logger
import me.fycz.sduseat.http.cookieCathe
import me.fycz.sduseat.utils.JsUtils
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.log


/**
 * @author fengyue
 * @date 2022/2/22 17:54
 */
var config: Config? = null
var date: String = ""
var area: AreaBean? = null
var auth: IAuth? = null
val allSeats = LinkedHashMap<Int, List<SeatBean>>()
val querySeats = LinkedHashMap<Int, List<SeatBean>>()
var periods = LinkedHashMap<Int, PeriodBean>()
var success = LinkedHashMap<Int, Boolean>()
var needReLogin = false // 是否需要重新登录

val threadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)

val spiderRunnable = Runnable {
    getAllSeats()
}

val authRunnable = Runnable {
    auth = if (config!!.webVpn) AuthWebVpn(config!!.userid!!, config!!.passwd!!, config!!.deviceId!!, config!!.retry)
    else Auth(config!!.userid!!, config!!.passwd!!, config!!.deviceId!!, config!!.retry)
    auth!!.login()
    needReLogin = false
}

fun main(args: Array<String>) {
    printInfo()
    Config.initConfig(args)
    date = dateFormat.format(System.currentTimeMillis() + ONE_DAY * config!!.delta)
    if (config!!.bookOnce) {
        startBook()
    } else {
        try {
            loginAndGetSeats()
        } catch (ignored: Exception) {
        }
    }
    val sdf = SimpleDateFormat("yyyy-MM-dd " + config!!.time)
    var startTime = if (config!!.time!!.length == 8) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sdf.format(Date()))
    } else {
        SimpleDateFormat("yyyy-MM-dd HH:mm").parse(sdf.format(Date()))
    }
    // 如果已过当天设置时间，修改首次运行时间为明天
    if (System.currentTimeMillis() > startTime.time) {
        startTime = Date(startTime.time + ONE_DAY)
    }
    logger.info { "请等待到${sdf.format(startTime)}" }
    val time = Timer()
    val task = object : TimerTask() {
        override fun run() {
            startBook()
        }
    }
    // 以每24小时执行一次
    time.scheduleAtFixedRate(task, startTime, ONE_DAY)
}

fun loginAndGetSeats(judgeExpire: Boolean = true) {
    var spiderRes: Future<*>? = null
    if (!config!!.webVpn) {
        spiderRes = threadPool.submit(spiderRunnable)
    }
    if (auth == null || (judgeExpire && auth!!.isExpire()) || needReLogin) {
        cookieCathe.clear()
        val authRes = threadPool.submit(authRunnable)
        try {
            authRes.get()
        } catch (e: Exception) {
            if (e.cause is SocketTimeoutException) {
                logger.error() { "登录失败：网络请求超时" }
            } else {
                logger.error(e) { }
            }
            throw e
        }
    }
    if (config!!.webVpn) {
        spiderRes = threadPool.submit(spiderRunnable)
    }
    try {
        spiderRes?.get()
    } catch (e: Exception) {
        if (e.cause is SocketTimeoutException) {
            logger.error() { "获取座位信息失败：网络请求超时" }
        } else {
            logger.error(e) { }
        }
        throw e
    }
}

fun startBook() {
    date = dateFormat.format(System.currentTimeMillis() + ONE_DAY * config!!.delta)
    success.clear()
    for (i in 0..config!!.retry) {
        try {
            bookTask()
            break
        } catch (ignored: Exception) {
        }
        if (i < config!!.retry)
            logger.info { "尝试预约${i + 1}/${config!!.retry}失败，将在${config!!.retryInterval}秒后重试..." }
        Thread.sleep((config!!.retryInterval * 1000).toLong())
    }
}

fun bookTask() {
    loginAndGetSeats()
    val bookSeatTasks = mutableListOf<Future<*>>()
    periods.keys.forEach {
        if (!success[it]!!) {
            bookSeatTasks.add(threadPool.submit {
                val periodTime = "${periods[it]!!.startTime}-${periods[it]!!.endTime}"
                var curSuccess = bookSeat(querySeats[it]!!, it, needFilter = false)
                if (curSuccess) return@submit
                if (!config!!.only) {
                    logger.info { "预约${periodTime}时间段座位：预设座位均无法预约，将预约预设区域的空闲座位" }
                    curSuccess = bookSeat(allSeats[it]!!, it)
                }
                success[it] = curSuccess
                if (!curSuccess) {
                    throw LibException("预约${periodTime}时间段座位：所有座位均无法预约，预约失败")
                }
            })
        }
    }
    var hasFailed = false
    bookSeatTasks.forEach {
        try {
            it.get()
        } catch (e: Exception) {
            if (e.cause is LibException) {
                logger.error() { e.message }
            } else {
                logger.error(e) { }
            }
            hasFailed = true
        }
    }
    if (hasFailed) throw LibException("")
    clear()
}

fun bookSeat(
    seats: List<SeatBean>,
    periodIndex: Int = 0,
    periodTime: String = "08:00-22:30",
    needFilter: Boolean = true
): Boolean {
    var success = false
    var mySeats = seats
    if (needFilter && !config!!.filterRule.isNullOrEmpty()) {
        try {
            mySeats = JsUtils.filterSeats(config!!.filterRule!!, seats)
        } catch (e: Exception) {
            logger.error(e) { "过滤座位时出错，将使用原座位进行预约" }
        }
    }
    for (seat in mySeats) {
        if (seat.status == 1) {
            val res = Lib.book(seat, date, auth!!, periodIndex, config!!.retry)
            if (res == 3)
                throw LibException("预约${periodTime}时间段座位：当前状态无法预约")
            else if (res == 2)
                needReLogin = true
            success = res == 1
        }
        if (success) return success
    }
    return success
}

fun getAllSeats() {
    periods.clear()
    allSeats.clear()
    querySeats.clear()
    if (area == null) {
        val libName = config!!.area!!.substringBefore("-")
        val subLibName = config!!.area!!.substringAfter("-")
        //获取图书馆信息
        val lib = Spider.getAreas(Spider.getLibs()[libName], date, config!!.retry)
        area = lib[subLibName]
    }
    //获取子区域信息
    val subLib = Spider.getAreas(area, date, config!!.retry)
    //val subLib = Spider.getAreas(AreaBean(208, "主楼(3-12)", 0, 0), date, config!!.retry)
    /*
        AreaBean(id=209, name=图东环(3-4), unusedSeats=7, allSeats=110, periods=null)
        AreaBean(id=210, name=电子阅览室, unusedSeats=0, allSeats=0, periods=null)
        AreaBean(id=208, name=主楼(3-12), unusedSeats=7, allSeats=1130, periods=null)
     */
    if (subLib.values.isNotEmpty()) {
        val curPeriods = subLib.values.first().periods
        val getSeatTasks = mutableListOf<Future<*>>()
        if (curPeriods.isNullOrEmpty()) {
            logger.warn { "未获取到可预约时间段，将无法进行预约" }
            getSeatTasks.add(threadPool.submit { getSeats(subLib) })
        } else {
            curPeriods.forEachIndexed { i, p ->
                if (isInPeriod(p)) {
                    periods[i] = p
                    if (!success.containsKey(i)) success[i] = false
                    getSeatTasks.add(threadPool.submit {
                        getSeats(subLib, i, "${p.startTime}-${p.endTime}")
                    })
                }
            }
        }
        getSeatTasks.forEach {
            it.get()
        }
    }
}

fun getSeats(subLib: Map<String, AreaBean>, periodIndex: Int = 0, periodTime: String = "08:00-22:30") {
    var log = "\n-------------获取$date ${periodTime}时间段座位-------------\n"
    val curQuerySeats = mutableListOf<SeatBean>()
    val curAllSeats = mutableListOf<SeatBean>()
    config!!.seats!!.forEach { (k, v) ->
        if (subLib.keys.contains(k)) {
            //获取座位信息
            val curSeats = Spider.getSeats(subLib[k], date, periodIndex, config!!.retry)
            curAllSeats.addAll(curSeats.values)
            v.forEach {
                if (curSeats.containsKey(it)) {
                    curQuerySeats.add(curSeats[it]!!)
                } else {
                    log += "无法查找到座位[$k-$it]，请检查提供的区域信息\n"
                }
            }
        } else {
            log += "无法查找到区域[$k]，请检查提供的座位信息\n"
        }
    }
    querySeats[periodIndex] = curQuerySeats
    allSeats[periodIndex] = curAllSeats
    if (allSeats[periodIndex].isNullOrEmpty()) {
        throw SpiderException("获取${periodTime}时间段座位：未查找到任何预设区域，请检查提供的区域信息")
    }
    if (!querySeats[periodIndex].isNullOrEmpty()) {
        var seatsInfo = "["
        querySeats[periodIndex]!!.forEach {
            seatsInfo += "${it.area.name}-${it.name},"
        }
        seatsInfo = seatsInfo.substring(0, seatsInfo.length - 1)
        seatsInfo += "]"
        log += "成功获取到${querySeats[periodIndex]!!.size}个预设座位信息：\n${seatsInfo}"
    } else {
        log += "未获取到预设座位信息，将预约预设区域的空闲座位"
    }
    logger.info { log }
}

fun isInPeriod(periodBean: PeriodBean): Boolean {
    val start = config!!.period!!.substringBefore("-")
    val end = config!!.period!!.substringAfter("-")
    val left = if (start < periodBean.startTime) periodBean.startTime else start
    val right = if (end > periodBean.endTime) periodBean.endTime else end
    return left < right
}

fun printInfo() {
    println(Const.javaClass.getResource("/banner.txt")?.readText())
}

fun clear() {
    allSeats.clear()
    querySeats.clear()
    periods.clear()
    success.clear()
}