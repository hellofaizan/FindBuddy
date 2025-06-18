package `in`.mohammadfaizan.minecraft.commands

import `in`.mohammadfaizan.minecraft.managers.TrackingManager
import `in`.mohammadfaizan.minecraft.managers.CompassManager
import `in`.mohammadfaizan.minecraft.utils.ConfigManager
import `in`.mohammadfaizan.minecraft.utils.LocationUtils
import `in`.mohammadfaizan.minecraft.ui.MessageUtils
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.logging.Level

/**
 * Command executor for FindBuddy commands
 * Handles locate, cancel, accept, and decline subcommands
 */
class FindBuddyCommand : CommandExecutor, TabCompleter {
    
    private val trackingManager = TrackingManager()
    private val compassManager = CompassManager()
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            // Permission check
            if (!sender.hasPermission("findbuddy.find")) {
                MessageUtils.sendErrorMessage(sender as? Player ?: return true, "You don't have permission to use this command!")
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
                MessageUtils.sendUsageMessage(sender, "/findbuddy <locate|cancel|accept|decline|cleanup> [player]")
                return true
            }

            val subcommand = args[0].lowercase()

            when (subcommand) {
                "locate" -> {
                    if (args.size < 2) {
                        MessageUtils.sendUsageMessage(sender, "/findbuddy locate <player>")
                        return true
                    }
                    return locatePlayer(sender, args[1])
                }
                "cancel" -> {
                    return cancelTracking(sender)
                }
                "accept" -> {
                    return acceptTrackingRequest(sender)
                }
                "decline" -> {
                    return declineTrackingRequest(sender)
                }
                "cleanup" -> {
                    if (args.size < 2) {
                        MessageUtils.sendUsageMessage(sender, "/findbuddy cleanup <player>")
                        return true
                    }
                    return cleanupPlayerCompasses(sender, args[1])
                }
                else -> {
                    MessageUtils.sendErrorMessage(sender, "Unknown subcommand: $subcommand")
                    MessageUtils.sendUsageMessage(sender, "/findbuddy <locate|cancel|accept|decline|cleanup> [player]")
                    return true
                }
            }

        } catch (e: Exception) {
            MessageUtils.sendErrorMessage(sender as? Player ?: return true, "An error occurred while executing the command.")
            Bukkit.getLogger().log(Level.SEVERE, "Error in FindBuddyCommand", e)
            return true
        }
    }

    private fun locatePlayer(sender: Player, targetName: String): Boolean {
        // Find target player
        val targetPlayer = Bukkit.getPlayer(targetName)
        if (targetPlayer == null) {
            MessageUtils.sendErrorMessage(sender, "Player '$targetName' is not online!")
            return true
        }

        // Self-check
        if (targetPlayer.uniqueId == sender.uniqueId) {
            MessageUtils.sendInfoMessage(sender, "You can't track yourself! Instead, look into a mirror.")
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

        // Check distance - if already within stop distance, don't start tracking
        val distance = LocationUtils.calculateDistance(sender.location, targetPlayer.location)
        if (distance <= ConfigManager.getStopDistance()) {
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
        val currentTargetUUID = trackingManager.getTargetPlayer(sender)
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

        // Check if requests are required
        if (ConfigManager.requireRequests()) {
            return sendTrackingRequest(sender, targetPlayer)
        }

        // Direct tracking
        return startTracking(sender, targetPlayer)
    }

    private fun sendTrackingRequest(sender: Player, targetPlayer: Player): Boolean {
        // Check if there's already a pending request
        val existingRequest = trackingManager.getPendingRequest(targetPlayer)
        if (existingRequest != null && existingRequest.requester == sender.uniqueId) {
            MessageUtils.sendInfoMessage(sender, "You already have a pending tracking request with ${targetPlayer.name}!")
            return true
        }
        
        // Send tracking request
        if (trackingManager.sendTrackingRequest(sender, targetPlayer)) {
            MessageUtils.sendTrackingRequestMessage(targetPlayer, sender.name)
            MessageUtils.sendRequestSentMessage(sender, targetPlayer.name)
            return true
        }
        
        return false
    }
    
    private fun startTracking(sender: Player, targetPlayer: Player): Boolean {
        // Cancel any previous tracking
        trackingManager.cancelTracking(sender)
        compassManager.removeCompass(sender)

        // Try to give compass
        val hasCompass = compassManager.giveCompass(sender, targetPlayer)
        
        if (hasCompass) {
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
        
        // Start tracking
        return trackingManager.startTracking(sender, targetPlayer)
    }

    private fun cancelTracking(sender: Player): Boolean {
        val wasTracking = trackingManager.cancelTracking(sender)
        
        // Forcefully remove ANY compass with LOYALTY enchantment
        val removedCompasses = compassManager.forceRemoveLoyaltyCompasses(sender)
        
        if (wasTracking || removedCompasses > 0) {
            if (removedCompasses > 0) {
                MessageUtils.sendSuccessMessage(sender, "Tracking cancelled and $removedCompasses compass(es) removed!")
            } else {
                MessageUtils.sendSuccessMessage(sender, "Tracking cancelled successfully!")
            }
        } else {
            MessageUtils.sendInfoMessage(sender, "You don't have any active tracking to cancel.")
        }
        
        return true
    }

    private fun acceptTrackingRequest(sender: Player): Boolean {
        val request = trackingManager.acceptTrackingRequest(sender)
        if (request == null) {
            MessageUtils.sendErrorMessage(sender, "You don't have any pending tracking requests!")
            return true
        }
        
        // Get the requester
        val requester = Bukkit.getPlayer(request.requester)
        if (requester == null || !requester.isOnline) {
            MessageUtils.sendErrorMessage(sender, "The player who sent the request is no longer online!")
            return true
        }
        
        // Send messages to both players
        MessageUtils.sendRequestAcceptedMessages(sender, requester)
        
        // Start tracking
        return startTracking(requester, sender)
    }
    
    private fun declineTrackingRequest(sender: Player): Boolean {
        val request = trackingManager.declineTrackingRequest(sender)
        if (request == null) {
            MessageUtils.sendErrorMessage(sender, "You don't have any pending tracking requests!")
            return true
        }
        
        // Get the requester
        val requester = Bukkit.getPlayer(request.requester)
        if (requester != null && requester.isOnline) {
            MessageUtils.sendRequestDeclinedMessages(sender, requester, request.requesterName)
        }
        
        return true
    }

    private fun cleanupPlayerCompasses(sender: Player, targetName: String): Boolean {
        // Permission check for cleanup command
        if (!sender.hasPermission("findbuddy.cleanup")) {
            MessageUtils.sendErrorMessage(sender, "You don't have permission to use the cleanup command!")
            return true
        }
        
        // Find target player
        val targetPlayer = Bukkit.getPlayer(targetName)
        if (targetPlayer == null) {
            MessageUtils.sendErrorMessage(sender, "Player '$targetName' is not online!")
            return true
        }

        // Self-check
        if (targetPlayer.uniqueId == sender.uniqueId) {
            MessageUtils.sendInfoMessage(sender, "You can't clean up your own compasses!")
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

        // Forcefully remove ALL compasses with LOYALTY enchantment
        val removedCompasses = compassManager.forceRemoveAllLoyaltyCompasses(targetPlayer)
        
        if (removedCompasses > 0) {
            MessageUtils.sendSuccessMessage(sender, "Cleaned up $removedCompasses compass(es) from ${targetPlayer.name}!")
        } else {
            MessageUtils.sendInfoMessage(sender, "${targetPlayer.name} doesn't have any compasses with LOYALTY enchantment!")
        }
        
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
                listOf("locate", "cancel", "accept", "decline", "cleanup").filter {
                    it.startsWith(args[0].lowercase())
                }
            }
            2 -> {
                // If first argument is "locate" or "cleanup", suggest player names
                if (args[0].lowercase() == "locate" || args[0].lowercase() == "cleanup") {
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
}
