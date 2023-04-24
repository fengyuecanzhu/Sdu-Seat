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

@Suppress("unused")
object Utf8BomUtils {
    private val UTF8_BOM_BYTES = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun removeUTF8BOM(xmlText: String): String {
        val bytes = xmlText.toByteArray()
        val containsBOM = (bytes.size > 3
            && bytes[0] == UTF8_BOM_BYTES[0]
            && bytes[1] == UTF8_BOM_BYTES[1]
            && bytes[2] == UTF8_BOM_BYTES[2])
        if (containsBOM) {
            return String(bytes, 3, bytes.size - 3)
        }
        return xmlText
    }

    fun removeUTF8BOM(bytes: ByteArray): ByteArray {
        val containsBOM = (bytes.size > 3
            && bytes[0] == UTF8_BOM_BYTES[0]
            && bytes[1] == UTF8_BOM_BYTES[1]
            && bytes[2] == UTF8_BOM_BYTES[2])
        if (containsBOM) {
            val copy = ByteArray(bytes.size - 3)
            System.arraycopy(bytes, 3, copy, 0, bytes.size - 3)
            return copy
        }
        return bytes
    }

    fun hasBom(bytes: ByteArray): Boolean {
        return (bytes.size > 3
            && bytes[0] == UTF8_BOM_BYTES[0]
            && bytes[1] == UTF8_BOM_BYTES[1]
            && bytes[2] == UTF8_BOM_BYTES[2])
    }
}