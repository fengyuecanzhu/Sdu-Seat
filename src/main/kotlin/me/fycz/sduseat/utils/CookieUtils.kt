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

package me.fycz.sduseat.utils

import okhttp3.Cookie

/**
 * @author fengyue
 * @date 2022/2/15 14:46
 */
object CookieUtils {
    fun cookieToMap(cookie: String): MutableMap<String, String> {
        val cookieMap = mutableMapOf<String, String>()
        if (cookie.isBlank()) {
            return cookieMap
        }
        val pairArray = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pair in pairArray) {
            val pairs = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (pairs.size == 1) {
                continue
            }
            val key = pairs[0].trim { it <= ' ' }
            val value = pairs[1]
            if (value.isNotBlank() || value.trim { it <= ' ' } == "null") {
                cookieMap[key] = value.trim { it <= ' ' }
            }
        }
        return cookieMap
    }

    fun mapToCookie(cookieMap: Map<String, String>?): String? {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return null
        }
        val builder = StringBuilder()
        for (key in cookieMap.keys) {
            val value = cookieMap[key]
            if (value?.isNotBlank() == true) {
                builder.append(key)
                    .append("=")
                    .append(value)
                    .append(";")
            }
        }
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }

    fun collection2Str(cookies: Collection<Cookie>?): String {
        if (cookies == null || cookies.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()
        for (cookie in cookies) {
            builder.append(cookie.name)
                .append("=")
                .append(cookie.value)
                .append(";")
        }
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }
}