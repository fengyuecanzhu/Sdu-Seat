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