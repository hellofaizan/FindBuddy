package `in`.mohammadfaizan.minecraft.managers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import `in`.mohammadfaizan.minecraft.FindBuddy
import `in`.mohammadfaizan.minecraft.models.Waypoint
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

object WaypointManager {
    private val gson = Gson()
    private val dataFolder: File
        get() {
            val folder = File(FindBuddy.instance.dataFolder, "Finddata")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }

    private fun getPlayerFile(uuid: UUID): File = File(dataFolder, "$uuid.json")

    fun getWaypoints(player: OfflinePlayer): MutableList<Waypoint> {
        val file = getPlayerFile(player.uniqueId)
        if (!file.exists()) return mutableListOf()
        FileReader(file).use { reader ->
            val type = object : TypeToken<MutableList<Waypoint>>() {}.type
            return gson.fromJson(reader, type) ?: mutableListOf()
        }
    }

    fun saveWaypoints(player: OfflinePlayer, waypoints: List<Waypoint>) {
        val file = getPlayerFile(player.uniqueId)
        FileWriter(file).use { writer ->
            gson.toJson(waypoints, writer)
        }
    }

    fun addWaypoint(player: OfflinePlayer, waypoint: Waypoint) {
        val waypoints = getWaypoints(player)
        waypoints.removeIf { it.name.equals(waypoint.name, ignoreCase = true) }
        waypoints.add(waypoint)
        saveWaypoints(player, waypoints)
    }

    fun removeWaypoint(player: OfflinePlayer, name: String): Boolean {
        val waypoints = getWaypoints(player)
        val removed = waypoints.removeIf { it.name.equals(name, ignoreCase = true) }
        if (removed) saveWaypoints(player, waypoints)
        return removed
    }

    fun getWaypoint(player: OfflinePlayer, name: String): Waypoint? {
        return getWaypoints(player).find { it.name.equals(name, ignoreCase = true) }
    }
} 