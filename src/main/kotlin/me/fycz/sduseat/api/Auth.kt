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
 * Copyright (C)  2022 fengyuecanzhu
 */

package me.fycz.sduseat.api

import me.fycz.sduseat.AuthException
import me.fycz.sduseat.constant.Const.LIB_URL
import me.fycz.sduseat.constant.Const.timeDateFormat
import me.fycz.sduseat.constant.Const.logger
import me.fycz.sduseat.http.*
import okhttp3.HttpUrl
import java.net.URLDecoder
import kotlin.collections.HashMap

/**
 * @author fengyue
 * @date 2022/2/15 11:07
 */
class Auth(
    userid: String,
    password: String,
    retry: Int = 0
) : IAuth(userid, password, retry) {
    override val authUrl: String = "$LIB_URL/cas/index.php"

    override fun login() {
        //GET 图书馆认证界面 302 -> 统一身份认证界面
        val res = getProxyClient().newCallResponse(retry) {
            url(authUrl)
        }
        //从统一身份认证界面获取必要信息
        gatherInfo(res.body?.text() ?: "")
        //获得rsa
        rsa = getRsa(userid, password, lt)
        //统一身份认证、图书馆认证
        auth(res.request.url)
        //如果最终获取到这几个必要cookie则说明登陆成功
        val cookies = cookieCathe["seat.lib.sdu.edu.cn"]!!
        if (cookies.contains("userid") && cookies.contains("user_name")
            && cookies.contains("access_token")
        ) {
            name = URLDecoder.decode(cookies["user_name"]?.value!!, "UTF-8")
            access_token = cookies["access_token"]?.value!!
            expire = cookies["expire"]?.value
            logger.info { "登录成功，欢迎$name" }
        } else {
            throw AuthException("登录失败")
        }
    }

    private fun auth(url: HttpUrl) {
        //POST 统一身份认证 发送认证信息
        var res = getProxyClient(allowRedirect = false).newCallResponse(retry) {
            url(url)
            postForm(HashMap<String, Any>().apply {
                put("rsa", rsa)
                put("ul", userid.length)
                put("pl", password.length)
                put("lt", lt)
                put("execution", execution)
                put("_eventId", _eventId)
            })
        }
        logger.debug { "Status code for auth-1-response is ${res.code}" }
        if (res.code != 302) {
            throw AuthException("阶段1：响应状态码为${res.code}, 认证失败")
        }
        var newUrl = res.headers["Location"]?.replace(" ", "") ?: ""
        if (newUrl.startsWith("/cas/login?service="))
            newUrl = newUrl.replace("/cas/login?service=", "")

        // 切换HEADER绕过检查再进行重定向
        res = getProxyClient().newCallResponse(retry) {
            url(URLDecoder.decode(newUrl, "UTF-8"))
            header("Host", "seat.lib.sdu.edu.cn")
        }
        logger.debug { "Status code for auth-2-response is ${res.code}" }
        if (res.code != 200) {
            throw AuthException("阶段2：响应状态码为${res.code}, 认证失败")
        }
    }

    override fun isExpire(): Boolean {
        cookieCathe["seat.lib.sdu.edu.cn"]?.get("expire")?.value?.let {
            //2022-03-19 18:36:51
            kotlin.runCatching {
                if (timeDateFormat.parse(URLDecoder.decode(it)).time > System.currentTimeMillis()) {
                    return false
                }
            }
        }
        return true
    }
}