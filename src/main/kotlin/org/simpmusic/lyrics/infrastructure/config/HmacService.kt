package org.simpmusic.lyrics.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service responsible for HMAC token generation and validation
 */
@Service
class HmacService {
    private val logger = LoggerFactory.getLogger(HmacService::class.java)
    
    @Value("\${security.hmac.secret:default-secret-key-please-change-in-production}")
    private lateinit var secretKey: String
    
    @Value("\${security.hmac.token-ttl:300000}")
    private var tokenTtl: Long = 300000 // 5 minutes in milliseconds
    
    private val algorithm = "HmacSHA256"
    
    // Lazy initialization of Mac to ensure secretKey has been injected
    private val mac: Mac by lazy {
        try {
            Mac.getInstance(algorithm).apply {
                init(SecretKeySpec(secretKey.toByteArray(), algorithm))
            }
        } catch (e: Exception) {
            logger.error("HmacService --> Failed to initialize HMAC: ${e.message}", e)
            throw RuntimeException("Failed to initialize HMAC", e)
        }
    }
    
    /**
     * Generate HMAC token for given data
     *
     * @param data The data to generate HMAC for
     * @return Base64 encoded HMAC token
     */
    fun generateHmac(data: String): String {
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))
    }
    
    /**
     * Validate HMAC token for given data
     *
     * @param data The data that was used to generate HMAC
     * @param hmac The HMAC token to validate
     * @return True if HMAC is valid, false otherwise
     */
    fun validateHmac(data: String, hmac: String): Boolean {
        val calculatedHmac = generateHmac(data)
        return calculatedHmac == hmac
    }
    
    /**
     * Validate timestamp to prevent replay attacks
     *
     * @param timestamp The timestamp to validate (in milliseconds)
     * @return True if timestamp is within allowed time window
     */
    fun isValidTimestamp(timestamp: String): Boolean {
        val requestTime = timestamp.toLongOrNull() ?: return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - requestTime) < tokenTtl
    }
} 