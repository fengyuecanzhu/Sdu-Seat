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

/**
 * @author fengyue
 * @date 2022/4/9 12:23
 */
data class CurBookBean(
    override val id: String,
    override val title: String = "",
    override val startTime: String = "",
    override val endTime: String = "",
    override val status: String = "",
    val curStatus: String = "",
    val signTime: String = "",
    val lastSignTime: String = "",
    val needBackTime: String = "",
) : IBookBean
