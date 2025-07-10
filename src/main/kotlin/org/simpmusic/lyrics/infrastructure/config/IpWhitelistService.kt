package org.simpmusic.lyrics.infrastructure.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service to manage IP whitelist for rate limiting exemption
 */
@Service
class IpWhitelistService {
    private val logger = LoggerFactory.getLogger(IpWhitelistService::class.java)
    
    private val adminIps = mutableSetOf<String>()
    
    @Value("\${security.ip-whitelist:127.0.0.1,192.168.1.100}")
    private lateinit var whitelistConfig: String
    
    @PostConstruct
    fun init() {
        // Đọc danh sách IP từ cấu hình và thêm vào whitelist
        whitelistConfig.split(",").forEach { ip ->
            val trimmedIp = ip.trim()
            if (trimmedIp.isNotEmpty()) {
                adminIps.add(trimmedIp)
                logger.info("IpWhitelistService --> Added admin IP to whitelist: $trimmedIp")
            }
        }
    }
    
    /**
     * Check if the given IP is in the admin whitelist
     *
     * @param ip IP address to check
     * @return true if IP is in the admin whitelist, false otherwise
     */
    fun isAdminIp(ip: String): Boolean {
        return adminIps.contains(ip)
    }
} 