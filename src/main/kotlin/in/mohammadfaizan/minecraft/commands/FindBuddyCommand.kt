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

class FindBuddyCommand : CommandExecutor, TabCompleter {
    
    // Track active tasks per player
    private val activeTasks = mutableMapOf<UUID, BukkitRunnable>()

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
                    Component.text("Usage: /findbuddy <player>")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                )
                return true
            }

            // Find target player
            val targetPlayer = Bukkit.getPlayer(args[0])
            if (targetPlayer == null) {
                sender.sendMessage(
                    Component.text("❌ Player '${args[0]}' is not online!")
                        .color(NamedTextColor.RED)
                )
                return true
            }

            // Self-check
            if (targetPlayer.uniqueId == sender.uniqueId) {
                sender.sendMessage(
                    Component.text("🤔 You can't find yourself! Look in a mirror instead.")
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

            val targetLocation = targetPlayer.location
            val dx = sender.location.x - targetLocation.x
            val dz = sender.location.z - targetLocation.z
            val distance = kotlin.math.sqrt(dx * dx + dz * dz)
            val direction = getDirection(sender.location, targetLocation)
            val compass = getCompassDirection(sender.location, targetLocation)

            // Format distance
            val distanceText = when {
                distance < 1.0 -> "very close"
                distance < 10.0 -> "${String.format("%.1f", distance)} blocks"
                else -> "${distance.toInt()} blocks"
            }

            // Send the main message
            sender.sendMessage(
                Component.text("🧭 ")
                    .color(NamedTextColor.GREEN)
                    .append(
                        Component.text(targetPlayer.name)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                    )
                    .append(Component.text(" is ").color(NamedTextColor.GREEN))
                    .append(Component.text(distanceText).color(NamedTextColor.YELLOW))
                    .append(Component.text(" ").color(NamedTextColor.GREEN))
                    .append(Component.text(direction).color(NamedTextColor.GOLD))
                    .append(Component.text(" of you").color(NamedTextColor.GREEN))
            )

            // Add compass direction for precision
            sender.sendMessage(
                Component.text("   📍 Direction: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(compass).color(NamedTextColor.WHITE))
            )

            // Add Y-level difference if significant
            val yDifference = targetLocation.blockY - sender.location.blockY
            if (abs(yDifference) > 5) {
                val verticalDirection = if (yDifference > 0) "above" else "below"
                sender.sendMessage(
                    Component.text("   ⬆️ Height: ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text("${abs(yDifference)} blocks $verticalDirection you").color(NamedTextColor.LIGHT_PURPLE))
                )
            }

            // Cancel any previous tracking task for this sender
            activeTasks.remove(sender.uniqueId)?.cancel()

            // Start a new repeating task to update the action bar
            val plugin = JavaPlugin.getProvidingPlugin(this::class.java)
            val task = object : BukkitRunnable() {
                override fun run() {
                    if (!sender.isOnline || !targetPlayer.isOnline) {
                        this.cancel(); activeTasks.remove(sender.uniqueId); return
                    }
                    if (sender.world.name != targetPlayer.world.name) {
                        this.cancel(); activeTasks.remove(sender.uniqueId); return
                    }
                    val dx = sender.location.x - targetPlayer.location.x
                    val dz = sender.location.z - targetPlayer.location.z
                    val distance = kotlin.math.sqrt(dx * dx + dz * dz)
                    if (distance > 25.0) {
                        val actionBarMsg = net.kyori.adventure.text.Component.text()
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
                        sender.sendActionBar("")
                        this.cancel(); activeTasks.remove(sender.uniqueId)
                    }
                }
            }
            task.runTaskTimer(plugin, 0L, 5L) // every 0.25 seconds
            activeTasks[sender.uniqueId] = task

            return true

        } catch (e: Exception) {
            sender.sendMessage(
                Component.text("❌ An error occurred while executing the command.")
                    .color(NamedTextColor.RED)
            )
            Bukkit.getLogger().log(Level.SEVERE, "Error in FindBuddyCommand", e)
            return true
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        // Only provide suggestions for the first argument and if sender is a player
        if (args.size != 1 || sender !is Player) {
            return emptyList()
        }

        val partialName = args[0].lowercase()
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

        return suggestions
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
}