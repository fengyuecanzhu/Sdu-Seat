package me.fycz.sduseat.bean

/**
 * @author fengyue
 * @date 2022/2/21 19:34
 */
data class SeatBean(
    val id: Int,
    val name: String,
    val status: Int,
    val statusName: String,
    val area: AreaBean
)
