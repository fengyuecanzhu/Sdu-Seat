@file:Suppress("unused")

package me.fycz.sduseat

class AppException(msg: String) : Exception(msg)

/**
 * 身份验证异常
 */
class AuthException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

class SpiderException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

class LibException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}