package `in`.mohammadfaizan.minecraft.managers

import `in`.mohammadfaizan.minecraft.FindBuddy
import `in`.mohammadfaizan.minecraft.utils.ConfigManager
import `in`.mohammadfaizan.minecraft.utils.LocationUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.*

/**
 * Manages all compass-related operations
 */
class CompassManager {
    
    // Track compass items per player
    private val compassItems = mutableMapOf<UUID, ItemStack>()
    
    // Track compass cooldowns per player
    private val compassCooldowns = mutableMapOf<UUID, Long>()
    
    // Custom compass identifier
    private val compassKey = NamespacedKey(FindBuddy.instance, "findbuddy_compass")
    
    /**
     * Create an enchanted compass for tracking a player
     */
    fun createEnchantedCompass(targetPlayer: Player): ItemStack {
        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta
        
        // Set display name
        meta.displayName(
            Component.text("🧭 ")
                .color(NamedTextColor.WHITE)
                .append(
                    Component.text("Tracking ")
                        .color(NamedTextColor.WHITE)
                )
                .append(
                    Component.text(targetPlayer.name)
                        .color(NamedTextColor.WHITE)
                )
                .append(Component.text(" - Right click to refresh").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
        )
        
        // Set lore
        val lore = listOf(
            Component.text("Right click to refresh player location").color(NamedTextColor.YELLOW).decorate(TextDecoration.ITALIC),
        )
        meta.lore(lore)
        
        // Add enchantment glow effect without showing enchantment name
        meta.addEnchant(Enchantment.LOYALTY, 1, true)
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        
        // Add custom identifier
        meta.persistentDataContainer.set(compassKey, PersistentDataType.STRING, targetPlayer.uniqueId.toString())
        
        compass.itemMeta = meta
        return compass
    }
    
    /**
     * Find an empty hotbar slot for a player
     */
    fun findEmptyHotbarSlot(player: Player): Int? {
        for (i in 0..8) {
            if (player.inventory.getItem(i) == null || player.inventory.getItem(i)?.type == Material.AIR) {
                return i
            }
        }
        return null
    }
    
    /**
     * Check if an item is a FindBuddy compass
     */
    fun isFindBuddyCompass(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.COMPASS) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(compassKey, PersistentDataType.STRING)
    }
    
    /**
     * Check if an item is a compass with LOYALTY enchantment
     */
    fun isLoyaltyCompass(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.COMPASS) return false
        val meta = item.itemMeta ?: return false
        return meta.hasEnchant(Enchantment.LOYALTY)
    }
    
    /**
     * Remove compass from a player's inventory
     */
    fun removeCompass(player: Player) {
        // Check main inventory slots (0-35)
        for (i in 0..35) {
            val item = player.inventory.getItem(i)
            if (isFindBuddyCompass(item)) {
                player.inventory.setItem(i, null)
                break
            }
        }
        
        // Check off-hand slot
        val offHandItem = player.inventory.itemInOffHand
        if (isFindBuddyCompass(offHandItem)) {
            player.inventory.setItemInOffHand(null)
        }
        
        // Clean up tracking data
        compassItems.remove(player.uniqueId)
        compassCooldowns.remove(player.uniqueId)
        resetCompassTarget(player)
    }
    
    /**
     * Forcefully remove ANY compass with LOYALTY enchantment from player's inventory
     * This is a fallback method to ensure compasses are removed even if they're not properly tracked
     */
    fun forceRemoveLoyaltyCompasses(player: Player): Int {
        var removedCount = 0
        
        // Check main inventory slots (0-35)
        for (i in 0..35) {
            val item = player.inventory.getItem(i)
            if (isLoyaltyCompass(item)) {
                player.inventory.setItem(i, null)
                removedCount++
            }
        }
        
        // Check off-hand slot
        val offHandItem = player.inventory.itemInOffHand
        if (isLoyaltyCompass(offHandItem)) {
            player.inventory.setItemInOffHand(null)
            removedCount++
        }
        
        // Also clean up tracking data
        compassItems.remove(player.uniqueId)
        compassCooldowns.remove(player.uniqueId)
        resetCompassTarget(player)
        
        return removedCount
    }
    
    /**
     * Forcefully remove ALL compasses with LOYALTY enchantment from player's inventory
     * This method doesn't clean up tracking data, just removes the items
     * Used for admin cleanup commands
     */
    fun forceRemoveAllLoyaltyCompasses(player: Player): Int {
        var removedCount = 0
        
        // Check main inventory slots (0-35)
        for (i in 0..35) {
            val item = player.inventory.getItem(i)
            if (isLoyaltyCompass(item)) {
                player.inventory.setItem(i, null)
                removedCount++
            }
        }
        
        // Check off-hand slot
        val offHandItem = player.inventory.itemInOffHand
        if (isLoyaltyCompass(offHandItem)) {
            player.inventory.setItemInOffHand(null)
            removedCount++
        }
        
        return removedCount
    }
    
    /**
     * Update compass direction to point to target player
     */
    fun updateCompassDirection(player: Player, targetPlayer: Player) {
        player.setCompassTarget(targetPlayer.location)
    }
    
    /**
     * Reset compass target to world spawn
     */
    fun resetCompassTarget(player: Player) {
        player.setCompassTarget(player.world.spawnLocation)
    }
    
    /**
     * Handle compass right-click interaction
     */
    fun handleCompassRightClick(player: Player): Boolean {
        // Safety check - only process if player has a compass
        if (!compassItems.containsKey(player.uniqueId)) {
            return false
        }
        
        // Check cooldown
        val currentTime = System.currentTimeMillis()
        val lastUpdate = compassCooldowns[player.uniqueId] ?: 0L
        val cooldownRemaining = (lastUpdate + ConfigManager.getCompassCooldown()) - currentTime
        
        if (cooldownRemaining > 0) {
            val secondsRemaining = (cooldownRemaining / 1000.0).toInt()
            player.sendMessage(
                Component.text("⏰ Compass refresh cooldown: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("${secondsRemaining}s remaining").color(NamedTextColor.RED))
            )
            return true
        }
        
        // Update compass direction
        val targetUUID = getTargetUUIDFromCompass(player)
        if (targetUUID != null) {
            val targetPlayer = Bukkit.getPlayer(targetUUID)
            if (targetPlayer != null && targetPlayer.isOnline) {
                updateCompassDirection(player, targetPlayer)
                compassCooldowns[player.uniqueId] = currentTime
                
                // Get distance for feedback
                val distance = LocationUtils.calculateDistance(player.location, targetPlayer.location)
                
                player.sendMessage(
                    Component.text("🧭 Compass updated! ")
                        .color(NamedTextColor.GREEN)
                        .append(
                            Component.text(targetPlayer.name)
                                .color(NamedTextColor.AQUA)
                                .decorate(TextDecoration.BOLD)
                        )
                        .append(Component.text(" is ").color(NamedTextColor.GREEN))
                        .append(Component.text("${distance.toInt()} blocks").color(NamedTextColor.YELLOW))
                        .append(Component.text(" away.").color(NamedTextColor.GREEN))
                )
            }
        }
        
        return true
    }
    
    /**
     * Get target UUID from compass item
     */
    private fun getTargetUUIDFromCompass(player: Player): UUID? {
        val compassItem = compassItems[player.uniqueId] ?: return null
        val meta = compassItem.itemMeta ?: return null
        val targetUUIDString = meta.persistentDataContainer.get(compassKey, PersistentDataType.STRING) ?: return null
        return try {
            UUID.fromString(targetUUIDString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * Give compass to player
     */
    fun giveCompass(player: Player, targetPlayer: Player): Boolean {
        val emptyHotbarSlot = findEmptyHotbarSlot(player)
        if (emptyHotbarSlot != null) {
            val enchantedCompass = createEnchantedCompass(targetPlayer)
            player.inventory.setItem(emptyHotbarSlot, enchantedCompass)
            compassItems[player.uniqueId] = enchantedCompass
            updateCompassDirection(player, targetPlayer)
            return true
        }
        return false
    }
    
    /**
     * Check if player has a compass
     */
    fun hasCompass(player: Player): Boolean {
        return compassItems.containsKey(player.uniqueId)
    }
    
    /**
     * Clean up compass data for a player
     */
    fun cleanupPlayer(player: Player) {
        compassItems.remove(player.uniqueId)
        compassCooldowns.remove(player.uniqueId)
        resetCompassTarget(player)
    }
}