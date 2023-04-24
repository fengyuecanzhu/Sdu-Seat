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
import me.fycz.sduseat.constant.Const.logger
import me.fycz.sduseat.http.*
import me.fycz.sduseat.utils.centerString
import org.jsoup.Jsoup

/**
 * @author fengyue
 * @date 2022/2/15 11:07
 */
class AuthWebVpn(
    userid: String,
    password: String,
    retry: Int = 0
) : IAuth(userid, password, retry) {
    override val authUrl: String = "https://webvpn.sdu.edu.cn/https/77726476706e69737468656265737421e0f6528f69236c45300d8db9d6562d/cas/login?service=http%3A%2F%2Fseat.lib.sdu.edu.cn%2Fcas%2Findex.php%3Fcallback%3Dhttp%3A%2F%2Fseat.lib.sdu.edu.cn%2Fhome%2Fweb%2Ff_second"
    private val libAuthUrl =
        "https://webvpn.sdu.edu.cn/http/77726476706e69737468656265737421e3f24088693c6152301b8db9d6502720e38b79/cas/index.php?callback=http://seat.lib.sdu.edu.cn/home/web/f_second"

    companion object {
        var lastSuccessLogin: Long = 0
    }

    override fun login() {
        //从统一身份认证界面获取必要信息
        gatherInfo()
        //获得rsa
        rsa = getRsa(userid, password, lt)
        //统一身份认证、图书馆认证
        auth()
        //获取access_token
        access_token = getAccessToken()
        lastSuccessLogin = System.currentTimeMillis()
    }

    private fun gatherInfo() {
        val res = getProxyClient().newCallResponseBody(retry) {
            url(libAuthUrl)
        }.text()
        gatherInfo(res)
    }

    private fun auth() {
        //登录webvpn
        var res = getProxyClient().newCallResponse(retry) {
            url(authUrl)
            postForm(HashMap<String, Any>().apply {
                put("rsa", rsa)
                put("ul", userid.length)
                put("pl", password.length)
                put("lt", lt)
                put("execution", execution)
                put("_eventId", _eventId)
            })
        }
        if (res.code != 200) {
            throw AuthException("阶段1/3：响应状态码为${res.code}, 信息化门户认证失败")
        }
        var resText = res.body?.text() ?: throw AuthException("阶段1：信息化门户认证失败")
        var doc = Jsoup.parse(resText)
        var title = doc.title()
        if (title == "山东大学信息化公共服务平台") {
            name = doc.selectFirst("#user-btn-01")?.text() ?: ""
            logger.info { "阶段1/3：信息化门户认证成功，欢迎$name" }
        } else {
            throw AuthException("阶段1/3：响应页面为$title, 信息化门户认证失败")
        }

        //登录图书馆
        res = getProxyClient().newCallResponse(retry) {
            url(libAuthUrl)
        }
        if (res.code != 200) {
            throw AuthException("阶段2/3：响应状态码为${res.code}, 图书馆认证失败")
        }
        resText = res.body?.text() ?: throw AuthException("阶段2/3：图书馆认证失败")
        doc = Jsoup.parse(resText)
        title = doc.title()
        if (title == "跳转提示") {
            logger.info { "阶段2/3：图书馆认证成功" }
        } else {
            throw AuthException("阶段2/3：响应页面为$title, 图书馆认证失败")
        }
    }

    private fun getAccessToken(): String {
        val res = getProxyClient().newCallResponse(retry) {
            url(Const.LIB_FIRST_URL)
        }
        if (res.code != 200) {
            throw AuthException("阶段3/3：响应状态码为${res.code}, access_token获取失败")
        }
        val resText = res.body?.text() ?: throw AuthException("阶段3：access_token获取失败")
        val accessToken = resText.centerString("'access_token':\"", "\"")
        if (accessToken.isEmpty()) {
            throw AuthException("阶段3/3：access_token获取失败")
        } else {
            logger.info { "阶段3/3：access_token获取成功，access_token=$accessToken" }
        }
        return accessToken
    }

    override fun isExpire(): Boolean {
        return System.currentTimeMillis() - lastSuccessLogin > 30 * 60 * 1000
    }
}