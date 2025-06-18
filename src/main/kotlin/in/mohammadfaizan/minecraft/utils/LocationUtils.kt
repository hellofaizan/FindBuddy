package `in`.mohammadfaizan.minecraft.utils

import org.bukkit.Location
import kotlin.math.*

/**
 * Utility class for location and direction calculations
 */
object LocationUtils {
    
    /**
     * Calculate distance between two locations (ignoring Y coordinate)
     */
    fun calculateDistance(from: Location, to: Location): Double {
        val dx = from.x - to.x
        val dz = from.z - to.z
        return sqrt(dx * dx + dz * dz)
    }
    
    /**
     * Get cardinal direction from one location to another
     */
    fun getDirection(from: Location, to: Location): String {
        val dx = to.x - from.x
        val dz = to.z - from.z
        val angle = atan2(dz, dx).toDegrees()
        
        return when {
            angle >= -22.5 && angle < 22.5 -> "east"
            angle >= 22.5 && angle < 67.5 -> "southeast" 
            angle >= 67.5 && angle < 112.5 -> "south"
            angle >= 112.5 && angle < 157.5 -> "southwest"
            angle >= 157.5 || angle < -157.5 -> "west"
            angle >= -157.5 && angle < -112.5 -> "northwest"
            angle >= -112.5 && angle < -67.5 -> "north"
            else -> "northeast"
        }
    }
    
    /**
     * Get compass direction with degrees from one location to another
     */
    fun getCompassDirection(from: Location, to: Location): String {
        val dx = to.x - from.x
        val dz = to.z - from.z
        val angle = atan2(dz, dx).toDegrees()
        
        // Convert to 0-360 range
        val normalizedAngle = if (angle < 0) angle + 360 else angle
        
        return when {
            normalizedAngle >= 337.5 || normalizedAngle < 22.5 -> "E (90°)"
            normalizedAngle >= 22.5 && normalizedAngle < 67.5 -> "SE (135°)"
            normalizedAngle >= 67.5 && normalizedAngle < 112.5 -> "S (180°)"
            normalizedAngle >= 112.5 && normalizedAngle < 157.5 -> "SW (225°)"
            normalizedAngle >= 157.5 && normalizedAngle < 202.5 -> "W (270°)"
            normalizedAngle >= 202.5 && normalizedAngle < 247.5 -> "NW (315°)"
            normalizedAngle >= 247.5 && normalizedAngle < 292.5 -> "N (0°)"
            else -> "NE (45°)"
        }
    }
    
    /**
     * Extension function to convert radians to degrees
     */
    private fun Double.toDegrees(): Double = this * 180.0 / PI
}