package `in`.mohammadfaizan.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.logging.Level
import kotlin.math.*
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.meta.CompassMeta
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent

class FindBuddyCommand : CommandExecutor, TabCompleter, Listener {
    
    // Track active tasks per player
    private val activeTasks = mutableMapOf<UUID, BukkitRunnable>()

    // Track compass items per player
    private val compassItems = mutableMapOf<UUID, ItemStack>()

    // Track target players per sender
    private val targetPlayers = mutableMapOf<UUID, UUID>()

    // Track compass cooldowns per player
    private val compassCooldowns = mutableMapOf<UUID, Long>()

    // Custom compass identifier
    private val compassKey = NamespacedKey(JavaPlugin.getProvidingPlugin(FindBuddyCommand::class.java), "findbuddy_compass")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            // Permission check
            if (!sender.hasPermission("findbuddy.find")) {
                sender.sendMessage(
                    Component.text("❌ You don't have permission to use this command!")
                        .color(NamedTextColor.RED)
                )
                return true
            }

            // Player check
            if (sender !is Player) {
                sender.sendMessage(
                    Component.text("❌ This command can only be used by players!")
                        .color(NamedTextColor.RED)
                )
                return true
            }

            // Usage check
            if (args.isEmpty()) {
                sender.sendMessage(
                    Component.text("Usage: /findbuddy <locate|cancel> [player]")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                )
                return true
            }

            val subcommand = args[0].lowercase()

            when (subcommand) {
                "locate" -> {
                    if (args.size < 2) {
                        sender.sendMessage(
                            Component.text("Usage: /findbuddy locate <player>")
                                .color(NamedTextColor.GOLD)
                                .decorate(TextDecoration.BOLD)
                        )
                        return true
                    }
                    return locatePlayer(sender, args[1])
                }
                "cancel" -> {
                    return cancelTracking(sender)
                }
                else -> {
                    sender.sendMessage(
                        Component.text("Unknown subcommand: $subcommand")
                            .color(NamedTextColor.RED)
                    )
                    sender.sendMessage(
                        Component.text("Usage: /findbuddy <locate|cancel> [player]")
                            .color(NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD)
                    )
                    return true
                }
            }

        } catch (e: Exception) {
            sender.sendMessage(
                Component.text("❌ An error occurred while executing the command.")
                    .color(NamedTextColor.RED)
            )
            Bukkit.getLogger().log(Level.SEVERE, "Error in FindBuddyCommand", e)
            return true
        }
    }

    private fun locatePlayer(sender: Player, targetName: String): Boolean {
            // Find target player
        val targetPlayer = Bukkit.getPlayer(targetName)
            if (targetPlayer == null) {
                sender.sendMessage(
                Component.text("❌ Player '$targetName' is not online!")
                        .color(NamedTextColor.RED)
                )
                return true
            }

            // Self-check
            if (targetPlayer.uniqueId == sender.uniqueId) {
                sender.sendMessage(
                Component.text("🤔 You can't track yourself! Instead, look into a mirror.")
                        .color(NamedTextColor.YELLOW)
                )
                return true
            }

            // Check if players are in the same world
            if (sender.world.name != targetPlayer.world.name) {
                sender.sendMessage(
                    Component.text("🌍 ")
                        .color(NamedTextColor.BLUE)
                        .append(
                            Component.text(targetPlayer.name)
                                .color(NamedTextColor.AQUA)
                                .decorate(TextDecoration.BOLD)
                        )
                        .append(Component.text(" is in a different world (").color(NamedTextColor.GRAY))
                        .append(Component.text(targetPlayer.world.name).color(NamedTextColor.WHITE))
                        .append(Component.text(")!").color(NamedTextColor.GRAY))
                )
                return true
            }

        // Check distance - if already within 25 blocks, don't start tracking
            val targetLocation = targetPlayer.location
            val dx = sender.location.x - targetLocation.x
            val dz = sender.location.z - targetLocation.z
            val distance = kotlin.math.sqrt(dx * dx + dz * dz)

        if (distance <= 25.0) {
            sender.sendMessage(
                Component.text("🎯 ")
                    .color(NamedTextColor.GREEN)
                    .append(
                        Component.text(targetPlayer.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" is already nearby!").color(NamedTextColor.GREEN))
            )
            return true
        }

        // Check if already tracking this player
        val currentTargetUUID = targetPlayers[sender.uniqueId]
        if (currentTargetUUID == targetPlayer.uniqueId) {
            sender.sendMessage(
                Component.text("🔄 You are already tracking ")
                    .color(NamedTextColor.YELLOW)
                    .append(
                        Component.text(targetPlayer.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text("!").color(NamedTextColor.YELLOW))
            )
            return true
        }

        // Cancel any previous tracking task for this sender
        activeTasks.remove(sender.uniqueId)?.cancel()
        removeCompass(sender)

        // Check for empty hotbar slot specifically
        val emptyHotbarSlot = findEmptyHotbarSlot(sender)
        var hasCompass = false
        
        if (emptyHotbarSlot != null) {
            // Create and give enchanted compass
            val enchantedCompass = createEnchantedCompass(targetPlayer)
            sender.inventory.setItem(emptyHotbarSlot, enchantedCompass)
            compassItems[sender.uniqueId] = enchantedCompass
            hasCompass = true
            
            sender.sendMessage(
                Component.text("🧭 Tracking compass for ")
                    .color(NamedTextColor.GREEN)
                    .append(
                        Component.text(targetPlayer.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" added to your hotbar!").color(NamedTextColor.GREEN))
            )
        } else {
            sender.sendMessage(
                Component.text("🧭 Started tracking ")
                    .color(NamedTextColor.GREEN)
                    .append(
                        Component.text(targetPlayer.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" (free up a hotbar slot for compass)").color(NamedTextColor.YELLOW))
            )
        }
        
        targetPlayers[sender.uniqueId] = targetPlayer.uniqueId

        // Set the compass to point to the target player (only if we have compass)
        if (hasCompass) {
            updateCompassDirection(sender, targetPlayer)
        }

        // Start a new repeating task to update the action bar and compass
            val plugin = JavaPlugin.getProvidingPlugin(this::class.java)
            val task = object : BukkitRunnable() {
                override fun run() {
                    if (!sender.isOnline || !targetPlayer.isOnline) {
                    this.cancel(); 
                    activeTasks.remove(sender.uniqueId)
                    removeCompass(sender)
                    resetCompassTarget(sender)
                    return
                    }
                    if (sender.world.name != targetPlayer.world.name) {
                    this.cancel(); 
                    activeTasks.remove(sender.uniqueId)
                    removeCompass(sender)
                    resetCompassTarget(sender)
                    return
                    }
                
                    val dx = sender.location.x - targetPlayer.location.x
                    val dz = sender.location.z - targetPlayer.location.z
                    val distance = kotlin.math.sqrt(dx * dx + dz * dz)
                
                    if (distance > 25.0) {
                        val hasCompass = compassItems.containsKey(sender.uniqueId)
                        val compassColor = if (hasCompass) NamedTextColor.LIGHT_PURPLE else NamedTextColor.GRAY
                        
                        val actionBarMsg = net.kyori.adventure.text.Component.text()
                            .append(
                                net.kyori.adventure.text.Component.text("Tracking ")
                                    .color(NamedTextColor.GRAY)
                            )
                            .append(
                                net.kyori.adventure.text.Component.text(targetPlayer.name)
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.BOLD)
                            )
                            .append(net.kyori.adventure.text.Component.text(" - ").color(NamedTextColor.GRAY))
                            .append(
                                net.kyori.adventure.text.Component.text("${distance.toInt()} blocks")
                                    .color(NamedTextColor.YELLOW)
                            )
                            .append(net.kyori.adventure.text.Component.text(" - ").color(NamedTextColor.GRAY))
                            .append(
                                net.kyori.adventure.text.Component.text("( ")
                                    .color(NamedTextColor.GRAY)
                            )
                            .append(
                                net.kyori.adventure.text.Component.text("🧭 ${getDirection(sender.location, targetPlayer.location)}")
                                    .color(NamedTextColor.GOLD)
                                    .decorate(TextDecoration.BOLD)
                            )
                            .append(
                                net.kyori.adventure.text.Component.text(")")
                                    .color(NamedTextColor.GRAY)
                            )
                            .build()
                        sender.sendActionBar(actionBarMsg)
                    } else {
                    // Target reached - remove compass and stop tracking
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
                    removeCompass(sender)
                    resetCompassTarget(sender)
                    this.cancel(); 
                    activeTasks.remove(sender.uniqueId)
                    }
                }
            }
            task.runTaskTimer(plugin, 0L, 5L) // every 0.25 seconds
            activeTasks[sender.uniqueId] = task

            return true
    }

    private fun cancelTracking(sender: Player): Boolean {
        val task = activeTasks.remove(sender.uniqueId)
        if (task != null) {
            task.cancel()
            sender.sendMessage(
                Component.text("✅ Tracking cancelled successfully!")
                    .color(NamedTextColor.GREEN)
            )
        } else {
            sender.sendMessage(
                Component.text("ℹ️ You don't have any active tracking to cancel.")
                    .color(NamedTextColor.YELLOW)
            )
        }
        
        // Remove compass if exists
        removeCompass(sender)
        
        // Reset compass target to world spawn
        resetCompassTarget(sender)
        
        // Clean up cooldown
        compassCooldowns.remove(sender.uniqueId)
        
        return true
    }

    private fun createEnchantedCompass(targetPlayer: Player): ItemStack {
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

    private fun findEmptyHotbarSlot(player: Player): Int? {
        for (i in 0..8) {
            if (player.inventory.getItem(i) == null || player.inventory.getItem(i)?.type == Material.AIR) {
                return i
            }
        }
        return null
    }

    private fun findEmptyInventorySlot(player: Player): Int? {
        for (i in 0..35) { // Check all inventory slots (0-35)
            if (player.inventory.getItem(i) == null || player.inventory.getItem(i)?.type == Material.AIR) {
                return i
            }
        }
        return null
    }

    private fun isFindBuddyCompass(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.COMPASS) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(compassKey, PersistentDataType.STRING)
    }

    private fun removeCompass(player: Player) {
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
        targetPlayers.remove(player.uniqueId)
        compassCooldowns.remove(player.uniqueId)
        resetCompassTarget(player)
    }

    private fun updateCompassDirection(player: Player, targetPlayer: Player) {
        // Use the proper setCompassTarget method to point to the target player
        player.setCompassTarget(targetPlayer.location)
    }

    private fun resetCompassTarget(player: Player) {
        // Reset compass to point to world spawn
        player.setCompassTarget(player.world.spawnLocation)
    }

    private fun handleCompassRightClick(player: Player): Boolean {
        // Safety check - only process if player has a compass
        if (!compassItems.containsKey(player.uniqueId)) {
            return false
        }
        
        val targetUUID = targetPlayers[player.uniqueId] ?: return false
        val targetPlayer = Bukkit.getPlayer(targetUUID) ?: return false
        
        // Check cooldown (20 seconds = 20000 milliseconds)
        val currentTime = System.currentTimeMillis()
        val lastUpdate = compassCooldowns[player.uniqueId] ?: 0L
        val cooldownRemaining = (lastUpdate + 20000) - currentTime
        
        if (cooldownRemaining > 0) {
            val secondsRemaining = (cooldownRemaining / 1000.0).toInt()
            player.sendMessage(
                Component.text("⏰ Compass refresh cooldown: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("${secondsRemaining}s remaining").color(NamedTextColor.RED))
            )
            return true
        }
        
        // Update compass direction using setCompassTarget
        updateCompassDirection(player, targetPlayer)
        compassCooldowns[player.uniqueId] = currentTime
        
        // Get distance for feedback
        val dx = player.location.x - targetPlayer.location.x
        val dz = player.location.z - targetPlayer.location.z
        val distance = kotlin.math.sqrt(dx * dx + dz * dz)
        
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
        
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        // Only provide suggestions if sender is a player
        if (sender !is Player) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // Suggest subcommands
                listOf("locate", "cancel").filter {
                    it.startsWith(args[0].lowercase())
                }
            }
            2 -> {
                // If first argument is "locate", suggest player names
                if (args[0].lowercase() == "locate") {
                    val partialName = args[1].lowercase()
        val suggestions = mutableListOf<String>()

        // Get all online players in the same world and filter based on input
        Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { player ->
                // Exclude the sender and players in different worlds
                player.uniqueId != sender.uniqueId &&
                player.world.name == sender.world.name &&
                // Include players whose names start with the partial input
                (partialName.isEmpty() || player.name.lowercase().startsWith(partialName))
            }
            .map { it.name }
            .sorted() // Sort alphabetically
            .toCollection(suggestions)

                    suggestions
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun getDirection(from: Location, to: Location): String {
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

    private fun getCompassDirection(from: Location, to: Location): String {
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

    private fun Double.toDegrees(): Double = this * 180.0 / PI

    // Event handlers for compass management
    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (isFindBuddyCompass(event.itemDrop.itemStack)) {
            event.isCancelled = true
            event.player.sendMessage(
                Component.text("❌ You cannot drop the tracking compass!")
                    .color(NamedTextColor.RED)
            )
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Check if clicked item is a FindBuddy compass
        if (isFindBuddyCompass(event.currentItem) || isFindBuddyCompass(event.cursor)) {
            // Always allow movement within player's own inventory
            if (event.inventory == player.inventory) {
                return // Allow all movement within player inventory
            }
            
            // Block movement to external inventories (chests, etc.)
            event.isCancelled = true
            player.sendMessage(
                Component.text("❌ You cannot move the tracking compass!")
                    .color(NamedTextColor.RED)
            )
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Check if any dragged item is a FindBuddy compass
        for (itemStack in event.newItems.values) {
            if (isFindBuddyCompass(itemStack)) {
                // Always allow movement within player's own inventory
                if (event.inventory == player.inventory) {
                    return
                }
                
                // Block movement to external inventories (chests, etc.)
                event.isCancelled = true
                player.sendMessage(
                    Component.text("❌ You cannot move the tracking compass")
                    .color(NamedTextColor.RED)
                )
                return
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
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
        removeCompass(player)
        resetCompassTarget(player)
    }

    @EventHandler
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val newSlot = event.newSlot

        // Check if the new held item is a FindBuddy compass
        val item = player.inventory.getItem(newSlot)
        if (isFindBuddyCompass(item)) {
            val targetUUID = targetPlayers[player.uniqueId]
            if (targetUUID != null) {
                val targetPlayer = Bukkit.getPlayer(targetUUID)
                if (targetPlayer != null && targetPlayer.isOnline) {
                    val direction = getDirection(player.location, targetPlayer.location)
                    val dx = player.location.x - targetPlayer.location.x
                    val dz = player.location.z - targetPlayer.location.z
                    val distance = kotlin.math.sqrt(dx * dx + dz * dz)

                    // player.sendMessage(
                    //     Component.text("🧭 Compass points ")
                    //         .color(NamedTextColor.GOLD)
                    //         .append(Component.text(direction).color(NamedTextColor.AQUA))
                    //         .append(Component.text(" to ").color(NamedTextColor.GOLD))
                    //         .append(
                    //             Component.text(targetPlayer.name)
                    //                 .color(NamedTextColor.WHITE)
                    //                 .decorate(TextDecoration.BOLD)
                    //         )
                    //         .append(Component.text(" (${distance.toInt()} blocks away)").color(NamedTextColor.GRAY))
                    // )
                }
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val player = event.player
            
            // Only process if player has a compass
            if (!compassItems.containsKey(player.uniqueId)) {
                return
            }
            
            // Check main hand
            val mainHandItem = player.inventory.itemInMainHand
            if (isFindBuddyCompass(mainHandItem)) {
                handleCompassRightClick(player)
                return
            }
            
            // Check off hand
            val offHandItem = player.inventory.itemInOffHand
            if (isFindBuddyCompass(offHandItem)) {
                handleCompassRightClick(player)
                return
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity as Player
        
        // Remove FindBuddy compass from drops when player dies
        event.drops.removeIf { itemStack ->
            if (isFindBuddyCompass(itemStack)) {
                // Remove compass from player's inventory
                player.inventory.removeItem(itemStack)
                true // Remove from drops
            } else {
                false // Keep other items
            }
        }
    }
}
