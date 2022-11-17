package me.fycz.sduseat.constant

import me.fycz.sduseat.config
import mu.KotlinLogging
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.script.ScriptEngineManager


/**
 * @author fengyue
 * @date 2022/2/15 10:51
 */
object Const {
    const val UA_NAME = "User-Agent"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36"
    val LIB_URL: String
        get() {
            return if (config?.webVpn != true) {
                "http://seat.lib.sdu.edu.cn"
                //"http://seatwx.lib.sdu.edu.cn:85"
            } else {
                "https://webvpn.sdu.edu.cn/http/77726476706e69737468656265737421e3f24088693c6152301b8db9d6502720e38b79"
            }
        }

    val LIB_FIRST_URL: String
        get() = "$LIB_URL/home/web/f_second"

    val periodFormat = "[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}".toRegex()
    val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd") }
    val timeDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()) }

    val logger = KotlinLogging.logger {}

    const val ONE_DAY = 86400000L

    private val defaultPath = "." + File.separator + "config" + File.separator

    val defaultConfig = if (File(defaultPath + "config.json").exists()) {
        defaultPath + "config.json"
    } else {
        defaultPath + "config.example.json"
    }

    val statusMap = mapOf(
        Pair(1, "等待审核"),
        Pair(2, "预约成功"),
        Pair(3, "使用中"),
        Pair(4, "已使用"),
        Pair(5, "审核未通过"),
        Pair(6, "用户取消"),
        Pair(7, "已超时"),
        Pair(8, "已关闭"),
        Pair(9, "预约开始提醒"),
        Pair(10, "迟到提醒"),
        Pair(11, "预约结束提醒")
    )
    val SCRIPT_ENGINE = ScriptEngineManager().getEngineByName("rhino")
}