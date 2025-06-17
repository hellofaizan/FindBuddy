package `in`.mohammadfaizan.minecraft

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.roundToInt

class DirectionHUD(private val plugin: JavaPlugin) {
    private var task: BukkitRunnable? = null

    fun start() {
        task = object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    updateDirectionScale(player)
                }
            }
        }
        task?.runTaskTimer(plugin, 0L, 2L) // Update every 2 ticks (0.1 seconds) for smoother display
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun updateDirectionScale(player: Player) {
        val yaw = normalizeYaw(player.location.yaw)
        val direction = getCardinalDirection(yaw)
        val scale = buildDirectionScale(yaw)
        
        // Create a more visible display with bold text and colors
        player.sendActionBar(
            Component.text("[ ").color(NamedTextColor.DARK_GRAY)
                .append(Component.text(scale).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" ] ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(direction).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
        )
    }

    private fun normalizeYaw(yaw: Float): Float {
        var normalized = yaw + 180
        while (normalized < 0) normalized += 360
        while (normalized > 360) normalized -= 360
        return normalized
    }

    private fun getCardinalDirection(yaw: Float): String {
        return when {
            yaw < 22.5 -> "N"
            yaw < 67.5 -> "NE"
            yaw < 112.5 -> "E"
            yaw < 157.5 -> "SE"
            yaw < 202.5 -> "S"
            yaw < 247.5 -> "SW"
            yaw < 292.5 -> "W"
            yaw < 337.5 -> "NW"
            else -> "N"
        }
    }

    private fun buildDirectionScale(yaw: Float): String {
        val center = (yaw / 15).roundToInt()
        val scale = StringBuilder()
        
        // Add cardinal directions with spacing
        if (center in 21..27) scale.append("W ") 
        if (center in 0..3 || center >= 21) scale.append("N ") 
        if (center in 6..12) scale.append("E ") 
        if (center in 12..18) scale.append("S ")
        
        // Build a more visible scale
        for (i in -4..4) {
            val mark = when (i) {
                0 -> "+"
                in -4..-1 -> "—"  // Using em dash for better visibility
                else -> "—"       // Using em dash for better visibility
            }
            scale.append(mark)
        }
        
        return scale.toString()
    }
}