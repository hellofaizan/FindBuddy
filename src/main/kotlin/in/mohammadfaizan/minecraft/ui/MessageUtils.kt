package `in`.mohammadfaizan.minecraft.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player

/**
 * Utility class for creating formatted messages and UI components
 */
object MessageUtils {
    
    /**
     * Send a tracking request message to target player
     */
    fun sendTrackingRequestMessage(targetPlayer: Player, requesterName: String) {
        targetPlayer.sendMessage(
            Component.text("§7§m                                                    ")
        )
        targetPlayer.sendMessage(
            Component.text("")
                .append(
                    Component.text("§e⚡ Tracking Request")
                        .decorate(TextDecoration.BOLD)
                )
        )
        targetPlayer.sendMessage(
            Component.text("§7")
                .append(
                    Component.text(requesterName)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("§7 wants to track your location"))
        )
        targetPlayer.sendMessage(Component.text(""))
        
        // Send clickable accept/decline buttons
        val acceptButton = Component.text("§a[ACCEPT]")
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/findbuddy accept"))
            .hoverEvent(HoverEvent.showText(
                Component.text("§7Click to §aACCEPT§7 the tracking request\n")
                    .append(Component.text("§7This will allow §b$requesterName§7 to track you"))
            ))
        
        val declineButton = Component.text("§c[DECLINE]")
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/findbuddy decline"))
            .hoverEvent(HoverEvent.showText(
                Component.text("§7Click to §cDECLINE§7 the tracking request\n")
                    .append(Component.text("§7This will deny §b$requesterName§7's request"))
            ))
        
        targetPlayer.sendMessage(
            Component.text("           ")
                .append(acceptButton)
                .append(Component.text("     "))
                .append(declineButton)
        )
        targetPlayer.sendMessage(
            Component.text("§7§m                                                    ")
        )
    }
    
    /**
     * Send tracking request sent confirmation to requester
     */
    fun sendRequestSentMessage(requester: Player, targetName: String) {
        requester.sendMessage(
            Component.text("§7§m                                                    ")
        )
        requester.sendMessage(
            Component.text("")
                .append(
                    Component.text("§e⚡ Tracking Request Sent")
                        .decorate(TextDecoration.BOLD)
                )
        )
        requester.sendMessage(
            Component.text("§7Request sent to ")
                .append(
                    Component.text(targetName)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("§7!"))
        )
        requester.sendMessage(
            Component.text("§7§m                                                    ")
        )
    }
    
    /**
     * Send request accepted message to both players
     */
    fun sendRequestAcceptedMessages(target: Player, requester: Player) {
        target.sendMessage(
            Component.text("§7§m                                                    ")
        )
        target.sendMessage(
            Component.text("")
                .append(
                    Component.text("§a✓ Request Accepted")
                        .decorate(TextDecoration.BOLD)
                )
        )
        target.sendMessage(
            Component.text("§7§m                                                    ")
        )
        
        requester.sendMessage(
            Component.text("§7§m                                                    ")
        )
        requester.sendMessage(
            Component.text("")
                .append(
                    Component.text("§a✓ Request Accepted")
                        .decorate(TextDecoration.BOLD)
                )
        )
        requester.sendMessage(
            Component.text("§7")
                .append(
                    Component.text(target.name)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("§7 accepted your tracking request"))
        )
        requester.sendMessage(
            Component.text("§7§m                                                    ")
        )
    }
    
    /**
     * Send request declined message to both players
     */
    fun sendRequestDeclinedMessages(target: Player, requester: Player, requesterName: String) {
        requester.sendMessage(
            Component.text("§7§m                                                    ")
        )
        requester.sendMessage(
            Component.text("")
                .append(
                    Component.text("§c✗ Request Declined")
                        .decorate(TextDecoration.BOLD)
                )
        )
        requester.sendMessage(
            Component.text("§7")
                .append(
                    Component.text(target.name)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("§7 declined your tracking request"))
        )
        requester.sendMessage(
            Component.text("§7§m                                                    ")
        )
        
        target.sendMessage(
            Component.text("§7§m                                                    ")
        )
        target.sendMessage(
            Component.text("")
                .append(
                    Component.text("§c✗ Request Declined")
                        .decorate(TextDecoration.BOLD)
                )
        )
        target.sendMessage(
            Component.text("§7You declined ")
                .append(
                    Component.text(requesterName)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                )
                .append(Component.text("§7's tracking request"))
        )
        target.sendMessage(
            Component.text("§7§m                                                    ")
        )
    }
    
    /**
     * Send error message
     */
    fun sendErrorMessage(player: Player, message: String) {
        player.sendMessage(
            Component.text("❌ $message")
                .color(NamedTextColor.RED)
        )
    }
    
    /**
     * Send success message
     */
    fun sendSuccessMessage(player: Player, message: String) {
        player.sendMessage(
            Component.text("✅ $message")
                .color(NamedTextColor.GREEN)
        )
    }
    
    /**
     * Send info message
     */
    fun sendInfoMessage(player: Player, message: String) {
        player.sendMessage(
            Component.text("ℹ️ $message")
                .color(NamedTextColor.YELLOW)
        )
    }
    
    /**
     * Send warning message
     */
    fun sendWarningMessage(player: Player, message: String) {
        player.sendMessage(
            Component.text("⚠️ $message")
                .color(NamedTextColor.GOLD)
        )
    }
    
    /**
     * Send usage message
     */
    fun sendUsageMessage(player: Player, usage: String) {
        player.sendMessage(
            Component.text("Usage: $usage")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
        )
    }
}