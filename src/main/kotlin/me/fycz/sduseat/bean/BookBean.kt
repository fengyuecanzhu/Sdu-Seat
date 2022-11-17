package me.fycz.sduseat.bean

/**
 * @author fengyue
 * @date 2022/4/6 21:15
 */
data class BookBean(
    override val id: String,
    override val title: String = "",
    override val startTime: String = "",
    override val endTime: String = "",
    override val status: String = ""
) : IBookBean
