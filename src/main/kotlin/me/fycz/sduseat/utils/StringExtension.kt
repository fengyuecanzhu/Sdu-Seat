package me.fycz.sduseat.utils

/**
 * @author fengyue
 * @date 2022/2/15 16:21
 */
/**
 * 取两个文本之间的文本值
 * @param left 文本前面
 * @param right 后面文本
 * @return 返回 String
 */
fun String.centerString(left: String?, right: String?): String {
    val result: String
    var zLen: Int
    if (left == null || left.isEmpty()) {
        zLen = 0
    } else {
        zLen = indexOf(left)
        if (zLen > -1) {
            zLen += left.length
        } else {
            zLen = 0
        }
    }
    var yLen = indexOf(right!!, zLen)
    if (yLen < 0 || right.isEmpty()) {
        yLen = length
    }
    result = substring(zLen, yLen)
    return result
}