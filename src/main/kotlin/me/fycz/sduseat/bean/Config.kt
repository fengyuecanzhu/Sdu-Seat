package me.fycz.sduseat.bean

import me.fycz.sduseat.AppException
import me.fycz.sduseat.CCWebVpn
import me.fycz.sduseat.config
import me.fycz.sduseat.constant.Const
import me.fycz.sduseat.constant.Const.logger
import me.fycz.sduseat.utils.GSON
import me.fycz.sduseat.utils.fromJsonObject
import java.io.File

/**
 * @author fengyue
 * @date 2022/2/22 18:29
 */
data class Config(
    var userid: String,
    var passwd: String,
    var area: String,
    var seats: LinkedHashMap<String, ArrayList<String>> = LinkedHashMap(),
    var filterRule: String = "",
    var only: Boolean = false,
    var time: String = "06:02",
    var period: String = "08:00-22:30",
    var retry: Int = 10,
    var retryInterval: Int = 2,
    var delta: Int = 0,
    var bookOnce: Boolean = false,
    var webVpn: Boolean = false,
) {
    constructor(webVpn: Boolean) : this("", "", "", webVpn = webVpn)

    companion object {
        fun initConfig(args: Array<String>) {
            val configPath = if (args.isEmpty()) {
                Const.defaultConfig
            } else {
                args[0]
            }
            val configFile = File(configPath)
            if (!configFile.exists()) {
                throw AppException("$configPath 配置文件不存在")
            } else {
                config = GSON.fromJsonObject<Config>(configFile.readText())?.apply {
                    if (time.isEmpty()) time = "12:32"
                    if (period.isEmpty()) period = "08:00-22:30"
                    if (!period.matches(Const.periodFormat)) {
                        period = "08:00-22:30"
                        logger.warn { "预约时间段格式错误，默认设置为：${config!!.period}" }
                    }
                    if (retryInterval <= 0) retryInterval = 30
                    if (filterRule.isNotEmpty()) {
                        filterRule = if (filterRule.startsWith("@js:")) {
                            filterRule.substring(4)
                        } else {
                            try {
                                File(filterRule).readText()
                            } catch (e: Exception) {
                                logger.error(e) { "过滤规则文件读取失败" }
                                ""
                            }
                        }
                    }
                } ?: throw AppException("配置文件格式错误")
            }
        }
    }
}