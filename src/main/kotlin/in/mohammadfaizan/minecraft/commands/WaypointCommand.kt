package `in`.mohammadfaizan.minecraft.commands

import `in`.mohammadfaizan.minecraft.managers.WaypointManager
import `in`.mohammadfaizan.minecraft.models.Waypoint
import `in`.mohammadfaizan.minecraft.utils.ConfigManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemFlag
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.enchantments.Enchantment

class WaypointCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("findbuddy.waypoint")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED))
            return true
        }
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED))
            return true
        }
        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }
        when (args[0].lowercase()) {
            "set" -> handleSet(sender, args)
            "remove" -> handleRemove(sender, args)
            "locate" -> handleLocate(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(Component.text("/waypoint set <name> <x> <y> <z> [world]", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/waypoint remove <name>", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/waypoint locate <name>", NamedTextColor.YELLOW))
    }

    private fun handleSet(player: Player, args: Array<out String>) {
        if (args.size < 5) {
            player.sendMessage(Component.text("Usage: /waypoint set <name> <x> <y> <z> [world]", NamedTextColor.RED))
            return
        }
        val name = args[1]
        val x = args[2].toDoubleOrNull()
        val y = args[3].toDoubleOrNull()
        val z = args[4].toDoubleOrNull()
        val world = if (args.size > 5) args[5] else player.world.name
        if (x == null || y == null || z == null) {
            player.sendMessage(Component.text("Coordinates must be numbers!", NamedTextColor.RED))
            return
        }
        val waypoint = Waypoint(name, world, x, y, z)
        WaypointManager.addWaypoint(player, waypoint)
        player.sendMessage(Component.text("Waypoint '$name' set at ($x, $y, $z) in world '$world'!", NamedTextColor.GREEN))
    }

    private fun handleRemove(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /waypoint remove <name>", NamedTextColor.RED))
            return
        }
        val name = args[1]
        val removed = WaypointManager.removeWaypoint(player, name)
        if (removed) {
            player.sendMessage(Component.text("Waypoint '$name' removed!", NamedTextColor.GREEN))
        } else {
            player.sendMessage(Component.text("Waypoint '$name' not found!", NamedTextColor.RED))
        }
    }

    private fun handleLocate(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /waypoint locate <name>", NamedTextColor.RED))
            return
        }
        val name = args[1]
        val waypoint = WaypointManager.getWaypoint(player, name)
        if (waypoint == null) {
            player.sendMessage(Component.text("Waypoint '$name' not found!", NamedTextColor.RED))
            return
        }
        val location = waypoint.toLocation()
        if (location == null) {
            player.sendMessage(Component.text("World '${waypoint.world}' not found!", NamedTextColor.RED))
            return
        }
        // Give a temporary compass
        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta
        meta.displayName(Component.text("Waypoint: $name", NamedTextColor.AQUA))
        meta.addEnchant(Enchantment.LOYALTY, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        compass.itemMeta = meta
        player.inventory.addItem(compass)
        player.sendMessage(Component.text("Compass given! It will disappear when you are close to the waypoint.", NamedTextColor.GREEN))
        // Start tracking
        object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    cancel()
                    return
                }
                val current = player.location
                if (current.world?.name != location.world?.name) return
                player.compassTarget = location
                val stopDistance = ConfigManager.getStopDistance() // Use existing config for stop distance
                if (current.distance(location) <= stopDistance) {
                    // Remove compass from inventory
                    player.inventory.contents = player.inventory.contents.map {
                        if (it != null && it.type == Material.COMPASS && it.itemMeta.displayName() == meta.displayName()) null else it
                    }.toTypedArray()
                    player.sendMessage(Component.text("You have reached the waypoint '$name'! Compass removed.", NamedTextColor.GOLD))
                    cancel()
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("FindBuddy")!!, 0L, 20L)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) return listOf("set", "remove", "locate").filter { it.startsWith(args[0], true) }
        if (args.size == 2 && sender is Player) {
            val sub = args[0].lowercase()
            if (sub == "remove" || sub == "locate") {
                return WaypointManager.getWaypoints(sender).map { it.name }.filter { it.startsWith(args[1], true) }
            }
        }
        // Suggest coordinates for /waypoint set <name> <x> <y> <z> [world]
        if (args[0].equals("set", true) && sender is Player) {
            val targetBlock = sender.getTargetBlockExact(100)
            if (targetBlock != null) {
                val x = targetBlock.location.blockX.toString()
                val y = targetBlock.location.blockY.toString()
                val z = targetBlock.location.blockZ.toString()
                when (args.size) {
                    3 -> return listOf(x)
                    4 -> return listOf(y)
                    5 -> return listOf(z)
                    6 -> return listOf(targetBlock.world.name)
                }
            }
        }
        return emptyList()
    }
}