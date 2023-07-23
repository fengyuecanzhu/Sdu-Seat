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

package me.fycz.sduseat.api

import me.fycz.sduseat.AuthException
import me.fycz.sduseat.constant.Const
import me.fycz.sduseat.constant.Const.DEVICE_URL
import me.fycz.sduseat.constant.Const.SCRIPT_ENGINE
import me.fycz.sduseat.http.*
import me.fycz.sduseat.utils.GSON
import me.fycz.sduseat.utils.parseString
import org.jsoup.Jsoup
import javax.script.Invocable

/**
 * @author fengyue
 * @date 2022/2/15 11:10
 */
abstract class IAuth(
    val userid: String,
    val password: String,
    val deviceId: String,
    val retry: Int = 0
) {
    abstract val authUrl: String
    protected var rsa: String = ""
    protected var lt: String = ""
    protected var execution: String = ""
    protected var _eventId: String = ""

    var access_token: String = ""
    var name: String = ""
    protected var expire: String? = null

    fun getRsa(user: String, pwd: String, lt: String): String {
        return strEnc(user + pwd + lt)
    }

    fun strEnc(str: String): String {
        val js = String(javaClass.getResourceAsStream("/des.js").readBytes())
        SCRIPT_ENGINE.eval(js)
        if (SCRIPT_ENGINE is Invocable) {
            return SCRIPT_ENGINE.invokeFunction(
                "strEnc",
                str, "1", "2", "3"
            ) as String
        }
        return ""
    }

    fun gatherInfo(html: String) {
        try {
            val doc = Jsoup.parse(html)
            lt = doc.selectFirst("#lt")?.`val`() ?: ""
            execution = doc.selectFirst("[name=execution]")?.`val`() ?: ""
            _eventId = doc.selectFirst("[name=_eventId]")?.`val`() ?: ""
        } catch (e: Exception) {
            Const.logger.error(e) {}
        }
        if (lt.isEmpty() || execution.isEmpty() || _eventId.isEmpty()) {
            throw AuthException("未获取到用户登陆所需的所有信息")
        }
    }

    fun device(user: String, password: String) {
        val res = getProxyClient().newCallResponse(retry) {
            url(DEVICE_URL)
            postForm(
                mapOf(
                    "d" to deviceId,
                    "d_s" to deviceId,
                    "d_md5" to deviceId,
                    "m" to 1,
                    "u" to strEnc(user),
                    "p" to strEnc(password),
                )
            )
        }
        val resBody = res.body?.text()!!
        val resInfo = try {
            GSON.parseString(resBody).asJsonObject?.get("info")?.asString?.trim()
        } catch (e: Throwable) {
            //{"info":"binded"}
            val rIndex = resBody.lastIndexOf("\"")
            val lIndex = resBody.lastIndexOf("\"", rIndex - 1)
            resBody.substring(lIndex + 1, rIndex).trim()
        }
        val info = when (resInfo) {
            "validErr" -> "用户名密码有误"
            "notFound" -> "用户名密码有误"
            "bind" -> "需要进行二次验证"
            "mobileErr" -> "尚未绑定手机"
            "binded" -> ""
            "pass" -> ""
            else -> "未知错误"
        }
        if (info.isNotEmpty()) throw AuthException("设备验证失败：$info")
    }

    abstract fun login()
    abstract fun isExpire(): Boolean
}