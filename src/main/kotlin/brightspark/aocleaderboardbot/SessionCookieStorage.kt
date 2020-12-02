package brightspark.aocleaderboardbot

import io.ktor.client.features.cookies.*
import io.ktor.http.*
import org.springframework.stereotype.Component

@Component
class SessionCookieStorage : CookiesStorage {
    private var sessionCookie = Cookie("session", "")

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = Unit

    override suspend fun get(requestUrl: Url): List<Cookie> = listOf(sessionCookie)

    override fun close() = Unit

    fun set(session: String) {
        sessionCookie = Cookie("session", session)
    }
}
