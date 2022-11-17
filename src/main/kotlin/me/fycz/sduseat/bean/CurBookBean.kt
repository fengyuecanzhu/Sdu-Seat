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
