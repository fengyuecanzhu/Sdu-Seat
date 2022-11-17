package me.fycz.sduseat.bean

/**
 * @author fengyue
 * @date 2022/4/1 21:54
 */
data class UpdateConfig(
    val newVersion: Int,
    val url: String,
    val updateLog: String,
    val isWhiteList: Boolean,
    val whiteListRegex: String,
    val webVpn: Boolean
)
