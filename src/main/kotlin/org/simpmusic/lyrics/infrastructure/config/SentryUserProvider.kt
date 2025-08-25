package org.simpmusic.lyrics.infrastructure.config

import io.sentry.protocol.User
import io.sentry.spring.jakarta.SentryUserProvider
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class SentryUserProvider : SentryUserProvider {
    override fun provideUser(): User? {
        val user = User()
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        if (requestAttributes is ServletRequestAttributes) {
            val request = requestAttributes.request
            user.ipAddress = request.getHeader("X-Forwarded-For")
                ?: request.getHeader("X-Real-IP")
                ?: request.remoteAddr
        }
        return user
    }
}
