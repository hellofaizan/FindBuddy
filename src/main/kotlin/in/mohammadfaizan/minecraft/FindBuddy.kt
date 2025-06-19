package `in`.mohammadfaizan.minecraft

import org.bukkit.plugin.java.JavaPlugin
import `in`.mohammadfaizan.minecraft.commands.FindBuddyCommand
import `in`.mohammadfaizan.minecraft.commands.WaypointCommand
import `in`.mohammadfaizan.minecraft.listeners.CompassListener
import `in`.mohammadfaizan.minecraft.listeners.PlayerListener
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

class FindBuddy : JavaPlugin() {

    companion object {
        lateinit var instance: FindBuddy
            private set
    }

    override fun onEnable() {
        instance = this
        
        // Save default config if it doesn't exist
        saveDefaultConfig()
        
        // Reload config to ensure it's loaded
        reloadConfig()
        
        // Create command executor
        val findBuddyCommand = FindBuddyCommand()
        
        // Register command programmatically
        registerCommand("findbuddy", findBuddyCommand, findBuddyCommand, "findbuddy.find")
        
        // Register waypoint command
        val waypointCommand = WaypointCommand()
        registerCommand("waypoint", waypointCommand, waypointCommand, "findbuddy.waypoint")
        
        // Register event listeners
        server.pluginManager.registerEvents(CompassListener(), this)
        server.pluginManager.registerEvents(PlayerListener(), this)
        
        logger.info("FindBuddy plugin has been enabled!")
    }

    override fun onDisable() {
        logger.info("FindBuddy plugin has been disabled!")
    }
    
    private fun registerCommand(
        name: String,
        executor: CommandExecutor,
        tabCompleter: TabCompleter,
        permission: String
    ) {
        val command = getCommand(name) ?: throw IllegalStateException("Command $name not found!")
        command.setExecutor(executor)
        command.tabCompleter = tabCompleter
        command.permission = permission
        command.usage = "/$name <locate|cancel|accept|decline> [player]"
    }
}