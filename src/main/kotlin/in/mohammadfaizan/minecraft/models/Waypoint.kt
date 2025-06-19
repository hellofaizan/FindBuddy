package `in`.mohammadfaizan.minecraft.models

import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * Data class representing a waypoint set by a player
 */
data class Waypoint(
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double
) {
    fun toLocation(): Location? = Bukkit.getWorld(world)?.let { Location(it, x, y, z) }

    companion object {
        fun fromLocation(name: String, location: Location): Waypoint = Waypoint(
            name,
            location.world?.name ?: "world",
            location.x,
            location.y,
            location.z
        )
    }
}