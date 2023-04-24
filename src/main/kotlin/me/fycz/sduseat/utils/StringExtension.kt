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

/**
 * @author fengyue
 * @date 2022/2/15 16:21
 */
/**
 * 取两个文本之间的文本值
 * @param left 文本前面
 * @param right 后面文本
 * @return 返回 String
 */
fun String.centerString(left: String?, right: String?): String {
    val result: String
    var zLen: Int
    if (left == null || left.isEmpty()) {
        zLen = 0
    } else {
        zLen = indexOf(left)
        if (zLen > -1) {
            zLen += left.length
        } else {
            zLen = 0
        }
    }
    var yLen = indexOf(right!!, zLen)
    if (yLen < 0 || right.isEmpty()) {
        yLen = length
    }
    result = substring(zLen, yLen)
    return result
}