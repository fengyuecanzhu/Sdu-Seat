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

package me.fycz.sduseat.bean

import me.fycz.sduseat.AppException
import me.fycz.sduseat.config
import me.fycz.sduseat.constant.Const
import me.fycz.sduseat.constant.Const.logger
import me.fycz.sduseat.utils.GSON
import me.fycz.sduseat.utils.fromJsonObject
import java.io.File

/**
 * @author fengyue
 * @date 2022/2/22 18:29
 */
data class Config(
    var userid: String? = null,
    var passwd: String? = null,
    var deviceId: String? = null,
    var area: String? = null,
    var seats: LinkedHashMap<String, ArrayList<String>>? = LinkedHashMap(),
    var filterRule: String? = "",
    var only: Boolean = false,
    var time: String? = "06:02",
    var period: String? = "08:00-22:30",
    var retry: Int = 10,
    var retryInterval: Int = 2,
    var delta: Int = 0,
    var bookOnce: Boolean = false,
    var webVpn: Boolean = false,
) {
    constructor(webVpn: Boolean) : this("", "", "", "", webVpn = webVpn)

    companion object {
        fun initConfig(args: Array<String>) {
            val configPath = if (args.isEmpty()) {
                Const.defaultConfig
            } else {
                args[0]
            }
            val configFile = File(configPath)
            if (!configFile.exists()) {
                throw AppException("$configPath 配置文件不存在")
            } else {
                config = GSON.fromJsonObject<Config>(configFile.readText())?.apply {
                    if (userid.isNullOrEmpty()) throw AppException("userid：用户名/学号不能为空")
                    if (passwd.isNullOrEmpty()) throw AppException("passwd：密码不能为空")
                    if (deviceId.isNullOrEmpty()) throw AppException("deviceId：设备ID不能为空，否则无法登录，请查看设备ID获取说明")
                    if (area.isNullOrEmpty()) throw AppException("area：选座区域不能为空")

                    if (webVpn) {
                        logger.warn { "webVpn：由于学校暂未开放图书馆新域名的访问权限，webVpn暂时无法使用" }
                        webVpn = false
                    }

                    if (time.isNullOrEmpty()) time = "12:32"
                    if (period.isNullOrEmpty()) period = "08:00-22:30"
                    if (!period!!.matches(Const.periodFormat)) {
                        period = "08:00-22:30"
                        logger.warn { "预约时间段格式错误，默认设置为：${config!!.period}" }
                    }
                    if (retryInterval <= 0) retryInterval = 30
                    if (!filterRule.isNullOrEmpty()) {
                        filterRule = if (filterRule!!.startsWith("@js:")) {
                            filterRule!!.substring(4)
                        } else {
                            try {
                                File(filterRule!!).readText()
                            } catch (e: Exception) {
                                logger.error(e) { "过滤规则文件读取失败" }
                                ""
                            }
                        }
                    }
                } ?: throw AppException("配置文件格式错误")
            }
        }
    }
}