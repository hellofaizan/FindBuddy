package `in`.mohammadfaizan.minecraft.listeners

import `in`.mohammadfaizan.minecraft.managers.TrackingManager
import `in`.mohammadfaizan.minecraft.managers.CompassManager
import `in`.mohammadfaizan.minecraft.ui.MessageUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.entity.Player

/**
 * Listener for player-related events
 */
class PlayerListener : Listener {
    
    private val trackingManager = TrackingManager()
    private val compassManager = CompassManager()
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Clean up tracking data
        trackingManager.onPlayerQuit(player)
        
        // Forcefully remove any compass with LOYALTY enchantment as a final cleanup
        compassManager.forceRemoveLoyaltyCompasses(player)
    }
}