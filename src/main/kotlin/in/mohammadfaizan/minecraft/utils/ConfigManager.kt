package `in`.mohammadfaizan.minecraft.utils

import `in`.mohammadfaizan.minecraft.FindBuddy

/**
 * Manages all configuration-related operations for the FindBuddy plugin
 */
object ConfigManager {
    
    /**
     * Get the distance in blocks when tracking stops
     */
    fun getStopDistance(): Int {
        return FindBuddy.instance.config.getInt("tracking.stop_distance", 25)
    }
    
    /**
     * Get the compass refresh cooldown in milliseconds
     */
    fun getCompassCooldown(): Long {
        return FindBuddy.instance.config.getLong("compass.refresh_cooldown", 20) * 1000
    }
    
    /**
     * Check if target players should be notified when tracked
     */
    fun shouldNotifyTarget(): Boolean {
        return FindBuddy.instance.config.getBoolean("tracking.notify_target", true)
    }
    
    /**
     * Check if tracking requests are required
     */
    fun requireRequests(): Boolean {
        return FindBuddy.instance.config.getBoolean("tracking.require_requests", false)
    }
    
    /**
     * Get the request timeout in milliseconds
     */
    fun getRequestTimeout(): Long {
        return FindBuddy.instance.config.getLong("tracking.request_timeout", 60) * 1000
    }
}