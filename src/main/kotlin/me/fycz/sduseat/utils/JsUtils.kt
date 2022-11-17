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

package me.fycz.sduseat.utils

import me.fycz.sduseat.bean.SeatBean
import me.fycz.sduseat.constant.Const.SCRIPT_ENGINE
import me.fycz.sduseat.constant.Const.logger
import javax.script.SimpleBindings
import org.mozilla.javascript.NativeArray

/**
 * @author fengyue
 * @date 2022/11/16 20:52
 */
object JsUtils {

    fun filterSeats(jsStr: String, seats: List<SeatBean>): List<SeatBean> {
        val sb = SimpleBindings()
        sb["seats"] = seats
        sb["utils"] = this
        val result = SCRIPT_ENGINE.eval(jsStr, sb)
        return if (result is List<*>) {
            result as List<SeatBean>
        }else if(result is Array<*>){
            result.toList() as List<SeatBean>
        } else {
            (result as NativeArray).toArray().toList() as List<SeatBean>
        }
    }

    fun log(msg: String) {
        logger.info { msg }
    }
}