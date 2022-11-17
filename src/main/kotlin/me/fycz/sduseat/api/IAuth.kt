package me.fycz.sduseat.api

import me.fycz.sduseat.AuthException
import me.fycz.sduseat.constant.Const
import me.fycz.sduseat.constant.Const.SCRIPT_ENGINE
import org.jsoup.Jsoup
import java.io.File
import javax.script.Invocable
import javax.script.ScriptEngineManager

/**
 * @author fengyue
 * @date 2022/2/15 11:10
 */
abstract class IAuth(
    val userid: String,
    val password: String,
    val retry: Int = 0
) {
    abstract val authUrl: String
    protected var rsa: String = ""
    protected var lt: String = ""
    protected var execution: String = ""
    protected var _eventId: String = ""

    var access_token: String = ""
    var name: String = ""
    protected var expire: String? = null

    fun getRsa(user: String, pwd: String, lt: String): String {
        val js = String(javaClass.getResourceAsStream("/des.js").readBytes())
        SCRIPT_ENGINE.eval(js)
        if (SCRIPT_ENGINE is Invocable) {
            return SCRIPT_ENGINE.invokeFunction(
                "strEnc",
                user + pwd + lt, "1", "2", "3"
            ) as String
        }
        return ""
    }

    fun gatherInfo(html: String) {
        try {
            val doc = Jsoup.parse(html)
            lt = doc.selectFirst("#lt")?.`val`() ?: ""
            execution = doc.selectFirst("[name=execution]")?.`val`() ?: ""
            _eventId = doc.selectFirst("[name=_eventId]")?.`val`() ?: ""
        } catch (e: Exception) {
            Const.logger.error(e) {}
        }
        if (lt.isEmpty() || execution.isEmpty() || _eventId.isEmpty()) {
            throw AuthException("未获取到用户登陆所需的所有信息")
        }
    }

    abstract fun login()
    abstract fun isExpire(): Boolean
}