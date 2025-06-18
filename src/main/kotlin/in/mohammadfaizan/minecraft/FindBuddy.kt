package `in`.mohammadfaizan.minecraft

import org.bukkit.plugin.java.JavaPlugin
import `in`.mohammadfaizan.minecraft.commands.FindBuddyCommand
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

class FindBuddy : JavaPlugin() {

    override fun onEnable() {
        // Create command executor
        val findBuddyCommand = FindBuddyCommand()
        
        // Register command programmatically
        registerCommand("findbuddy", findBuddyCommand, findBuddyCommand, "findbuddy.find")
        
        // Register event listener for compass management
        server.pluginManager.registerEvents(findBuddyCommand, this)
        
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
        command.usage = "/$name <locate|cancel> [player]"
    }
}