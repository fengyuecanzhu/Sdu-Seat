package me.fycz.sduseat.bean

/**
 * @author fengyue
 * @date 2022/2/21 15:15
 */
data class AreaBean(
    val id: Int?,
    val name: String?,
    val unusedSeats: Int = 0,
    val allSeats: Int = 0,
    val periods: List<PeriodBean>? = null
)
