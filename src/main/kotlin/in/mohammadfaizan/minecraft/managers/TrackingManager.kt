package `in`.mohammadfaizan.minecraft.managers

import `in`.mohammadfaizan.minecraft.FindBuddy
import `in`.mohammadfaizan.minecraft.models.TrackingRequest
import `in`.mohammadfaizan.minecraft.utils.ConfigManager
import `in`.mohammadfaizan.minecraft.utils.LocationUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * Manages all tracking-related operations
 */
class TrackingManager {
    
    // Track active tasks per player
    private val activeTasks = mutableMapOf<UUID, BukkitRunnable>()
    
    // Track target players per sender
    private val targetPlayers = mutableMapOf<UUID, UUID>()
    
    // Track pending tracking requests
    private val pendingRequests = mutableMapOf<UUID, TrackingRequest>()
    
    // Compass manager instance
    private val compassManager = CompassManager()
    
    /**
     * Start tracking a player
     */
    fun startTracking(sender: Player, targetPlayer: Player): Boolean {
        // Cancel any previous tracking task for this sender
        activeTasks.remove(sender.uniqueId)?.cancel()
        
        targetPlayers[sender.uniqueId] = targetPlayer.uniqueId
        
        // Notify target player if enabled in config
        if (ConfigManager.shouldNotifyTarget()) {
            targetPlayer.sendMessage(
                Component.text("👁️ ")
                    .color(NamedTextColor.YELLOW)
                    .append(
                        Component.text(sender.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" is now tracking you!").color(NamedTextColor.YELLOW))
            )
        }
        
        // Start tracking task
        startTrackingTask(sender, targetPlayer)
        
        return true
    }
    
    /**
     * Cancel tracking for a player
     */
    fun cancelTracking(sender: Player): Boolean {
        val task = activeTasks.remove(sender.uniqueId)
        if (task != null) {
            task.cancel()
            targetPlayers.remove(sender.uniqueId)
            
            // Forcefully remove any compass with LOYALTY enchantment
            compassManager.forceRemoveLoyaltyCompasses(sender)
            
            return true
        }
        return false
    }
    
    /**
     * Check if a player is currently tracking someone
     */
    fun isTracking(player: Player): Boolean {
        return activeTasks.containsKey(player.uniqueId)
    }
    
    /**
     * Get the target player UUID for a tracker
     */
    fun getTargetPlayer(tracker: Player): UUID? {
        return targetPlayers[tracker.uniqueId]
    }
    
    /**
     * Send a tracking request
     */
    fun sendTrackingRequest(sender: Player, targetPlayer: Player): Boolean {
        // Check if there's already a pending request
        val existingRequest = pendingRequests[targetPlayer.uniqueId]
        if (existingRequest != null && existingRequest.requester == sender.uniqueId) {
            return false
        }
        
        // Create tracking request
        val request = TrackingRequest(
            requester = sender.uniqueId,
            requesterName = sender.name,
            target = targetPlayer.uniqueId,
            targetName = targetPlayer.name,
            timestamp = System.currentTimeMillis()
        )
        
        pendingRequests[targetPlayer.uniqueId] = request
        
        // Schedule request timeout
        val plugin = FindBuddy.instance
        Bukkit.getScheduler().runTaskLater(plugin, object : Runnable {
            override fun run() {
                val currentRequest = pendingRequests[targetPlayer.uniqueId]
                if (currentRequest != null && currentRequest.requester == sender.uniqueId) {
                    pendingRequests.remove(targetPlayer.uniqueId)
                    
                    // Notify both players about timeout
                    val requester = Bukkit.getPlayer(sender.uniqueId)
                    requester?.sendMessage(
                        Component.text("⏰ Tracking request to ")
                            .color(NamedTextColor.YELLOW)
                            .append(
                                Component.text(targetPlayer.name)
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD)
                            )
                            .append(Component.text(" timed out!").color(NamedTextColor.YELLOW))
                    )
                    
                    targetPlayer.sendMessage(
                        Component.text("⏰ Tracking request from ")
                            .color(NamedTextColor.YELLOW)
                            .append(
                                Component.text(sender.name)
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD)
                            )
                            .append(Component.text(" timed out!").color(NamedTextColor.YELLOW))
                    )
                }
            }
        }, ConfigManager.getRequestTimeout() / 50) // Convert milliseconds to ticks
        
        return true
    }
    
    /**
     * Accept a tracking request
     */
    fun acceptTrackingRequest(target: Player): TrackingRequest? {
        val request = pendingRequests[target.uniqueId] ?: return null
        pendingRequests.remove(target.uniqueId)
        return request
    }
    
    /**
     * Decline a tracking request
     */
    fun declineTrackingRequest(target: Player): TrackingRequest? {
        val request = pendingRequests[target.uniqueId] ?: return null
        pendingRequests.remove(target.uniqueId)
        return request
    }
    
    /**
     * Get pending request for a player
     */
    fun getPendingRequest(player: Player): TrackingRequest? {
        return pendingRequests[player.uniqueId]
    }
    
    /**
     * Clean up tracking when a player quits
     */
    fun onPlayerQuit(player: Player) {
        // Clean up tracking if player was being tracked
        val targetUUID = targetPlayers[player.uniqueId]
        if (targetUUID != null) {
            val targetPlayer = Bukkit.getPlayer(targetUUID)
            targetPlayer?.sendMessage(
                Component.text("📱 ")
                    .color(NamedTextColor.YELLOW)
                    .append(
                        Component.text(player.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" has left the server. Tracking stopped.").color(NamedTextColor.YELLOW))
            )
        }
        
        // Clean up if player was tracking someone
        activeTasks.remove(player.uniqueId)?.cancel()
        targetPlayers.remove(player.uniqueId)
        
        // Forcefully remove any compass with LOYALTY enchantment
        compassManager.forceRemoveLoyaltyCompasses(player)
        
        // Clean up pending requests
        val pendingRequest = pendingRequests[player.uniqueId]
        if (pendingRequest != null) {
            pendingRequests.remove(player.uniqueId)
            
            // Notify requester that target left
            val requester = Bukkit.getPlayer(pendingRequest.requester)
            requester?.sendMessage(
                Component.text("📱 ")
                    .color(NamedTextColor.YELLOW)
                    .append(
                        Component.text(player.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" left the server. Tracking request cancelled.").color(NamedTextColor.YELLOW))
            )
        }
        
        // Clean up requests sent by this player
        pendingRequests.entries.removeIf { (_, request) ->
            if (request.requester == player.uniqueId) {
                // Notify target that requester left
                val target = Bukkit.getPlayer(request.target)
                target?.sendMessage(
                    Component.text("📱 ")
                        .color(NamedTextColor.YELLOW)
                        .append(
                            Component.text(player.name)
                                .color(NamedTextColor.AQUA)
                                .decorate(TextDecoration.BOLD)
                        )
                        .append(Component.text(" left the server. Tracking request cancelled.").color(NamedTextColor.YELLOW))
                )
                true // Remove this request
            } else {
                false // Keep other requests
            }
        }
    }
    
    /**
     * Start the tracking task for a player
     */
    private fun startTrackingTask(sender: Player, targetPlayer: Player) {
        val plugin = FindBuddy.instance
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!sender.isOnline || !targetPlayer.isOnline) {
                    this.cancel()
                    activeTasks.remove(sender.uniqueId)
                    targetPlayers.remove(sender.uniqueId)
                    
                    // Forcefully remove any compass with LOYALTY enchantment
                    compassManager.forceRemoveLoyaltyCompasses(sender)
                    return
                }
                
                if (sender.world.name != targetPlayer.world.name) {
                    this.cancel()
                    activeTasks.remove(sender.uniqueId)
                    targetPlayers.remove(sender.uniqueId)
                    
                    // Forcefully remove any compass with LOYALTY enchantment
                    compassManager.forceRemoveLoyaltyCompasses(sender)
                    return
                }
                
                val distance = LocationUtils.calculateDistance(sender.location, targetPlayer.location)
                
                if (distance > ConfigManager.getStopDistance()) {
                    val direction = LocationUtils.getDirection(sender.location, targetPlayer.location)
                    
                    val actionBarMsg = Component.text()
                        .append(
                            Component.text("Tracking ")
                                .color(NamedTextColor.GRAY)
                        )
                        .append(
                            Component.text(targetPlayer.name)
                                .color(NamedTextColor.AQUA)
                                .decorate(TextDecoration.BOLD)
                        )
                        .append(Component.text(" - ").color(NamedTextColor.GRAY))
                        .append(
                            Component.text("${distance.toInt()} blocks")
                                .color(NamedTextColor.YELLOW)
                        )
                        .append(Component.text(" - ").color(NamedTextColor.GRAY))
                        .append(
                            Component.text("( ")
                                .color(NamedTextColor.GRAY)
                        )
                        .append(
                            Component.text("🧭 $direction")
                                .color(NamedTextColor.GOLD)
                                .decorate(TextDecoration.BOLD)
                        )
                        .append(
                            Component.text(")")
                                .color(NamedTextColor.GRAY)
                        )
                        .build()
                    
                    sender.sendActionBar(actionBarMsg)
                } else {
                    // Target reached - stop tracking
                    sender.sendActionBar("")
                    sender.sendMessage(
                        Component.text("🎯 ")
                            .color(NamedTextColor.GREEN)
                            .append(
                                Component.text(targetPlayer.name)
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD)
                            )
                            .append(Component.text(" is nearby!").color(NamedTextColor.GREEN))
                    )
                    this.cancel()
                    activeTasks.remove(sender.uniqueId)
                    targetPlayers.remove(sender.uniqueId)
                    
                    // Forcefully remove any compass with LOYALTY enchantment
                    compassManager.forceRemoveLoyaltyCompasses(sender)
                }
            }
        }
        task.runTaskTimer(plugin, 0L, 5L) // every 0.25 seconds
        activeTasks[sender.uniqueId] = task
    }
}