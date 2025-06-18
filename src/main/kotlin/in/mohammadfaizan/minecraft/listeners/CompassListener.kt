package `in`.mohammadfaizan.minecraft.listeners

import `in`.mohammadfaizan.minecraft.managers.CompassManager
import `in`.mohammadfaizan.minecraft.ui.MessageUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.entity.Player

/**
 * Listener for compass-related events
 */
class CompassListener : Listener {
    
    private val compassManager = CompassManager()
    
    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (compassManager.isFindBuddyCompass(event.itemDrop.itemStack)) {
            event.isCancelled = true
            MessageUtils.sendErrorMessage(event.player, "You cannot drop the tracking compass!")
        }
    }
    
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Check if clicked item is a FindBuddy compass
        if (compassManager.isFindBuddyCompass(event.currentItem) || compassManager.isFindBuddyCompass(event.cursor)) {
            // Always allow movement within player's own inventory
            if (event.inventory == player.inventory) {
                return // Allow all movement within player inventory
            }
            
            // Block movement to external inventories (chests, etc.)
            event.isCancelled = true
            MessageUtils.sendErrorMessage(player, "You cannot move the tracking compass!")
        }
    }
    
    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Check if any dragged item is a FindBuddy compass
        for (itemStack in event.newItems.values) {
            if (compassManager.isFindBuddyCompass(itemStack)) {
                // Always allow movement within player's own inventory
                if (event.inventory == player.inventory) {
                    return
                }
                
                // Block movement to external inventories (chests, etc.)
                event.isCancelled = true
                MessageUtils.sendErrorMessage(player, "You cannot move the tracking compass")
                return
            }
        }
    }
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val player = event.player
            
            // Only process if player has a compass
            if (!compassManager.hasCompass(player)) {
                return
            }
            
            // Check main hand
            val mainHandItem = player.inventory.itemInMainHand
            if (compassManager.isFindBuddyCompass(mainHandItem)) {
                compassManager.handleCompassRightClick(player)
                return
            }
            
            // Check off hand
            val offHandItem = player.inventory.itemInOffHand
            if (compassManager.isFindBuddyCompass(offHandItem)) {
                compassManager.handleCompassRightClick(player)
                return
            }
        }
    }
    
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity as Player
        
        // Remove FindBuddy compass from drops when player dies
        event.drops.removeIf { itemStack ->
            if (compassManager.isFindBuddyCompass(itemStack)) {
                // Remove compass from player's inventory
                player.inventory.removeItem(itemStack)
                true // Remove from drops
            } else {
                false // Keep other items
            }
        }
    }
}