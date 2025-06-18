# 🧭 FindBuddy - Minecraft Player Tracking Plugin

**🎯 Track your friends with enchanted compasses!**  
**⚡ Real-time location updates with action bar!**  
**🔒 Secure permission-based tracking system!**

---

## 🌟 Features

- 🧭 **Smart Compass** - Enchanted compass that points to tracked players
- 📍 **Real-time Tracking** - Live distance and direction updates in action bar
- 🔐 **Permission System** - Optional request-based tracking (like TPA)
- 🌍 **Cross-World Support** - Works across different worlds and dimensions
- ⚙️ **Highly Configurable** - Customize distances, cooldowns, and notifications
- 🛡️ **Admin Tools** - Cleanup commands for server management
- 🔄 **Auto Cleanup** - Automatic compass removal to prevent issues

---

## 📥 Installation

1. **Download** the latest JAR file
2. **Place** in your server's `plugins/` folder
3. **Restart** your server
4. **Configure** using the generated `config.yml`

### Requirements
- **Minecraft**: 1.21+ (Paper/Spigot/Bukkit)
- **Java**: 17 or higher

---

## 📋 Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/findbuddy locate <player>` | Start tracking a player |
| `/findbuddy cancel` | Cancel current tracking |
| `/findbuddy accept` | Accept a tracking request |
| `/findbuddy decline` | Decline a tracking request |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/findbuddy cleanup <player>` | Remove all tracking compasses from a player |

**Aliases**: `/fb`, `/find`

---

## ⚙️ Configuration

```yaml
# FindBuddy Configuration
tracking:
  stop_distance: 25          # Distance when tracking stops
  notify_target: true        # Notify target when tracked
  require_requests: true     # Require permission requests
  request_timeout: 90        # Request timeout in seconds

compass:
  refresh_cooldown: 20       # Compass refresh cooldown in seconds
```

---

## 🔧 Permissions

```yaml
# Basic permissions
findbuddy.find:
  default: true
  description: "Allows using basic FindBuddy commands"

# Admin permissions
findbuddy.cleanup:
  default: op
  description: "Allows using cleanup commands"

# All permissions
findbuddy.*:
  default: op
  children:
    findbuddy.find: true
    findbuddy.cleanup: true
```

---

## 🎮 How It Works

1. **Player A** uses `/findbuddy locate PlayerB`
2. **System checks** if requests are required
3. **If direct tracking**: Player A gets a compass and starts tracking
4. **If requests required**: Player B gets a request to accept/decline
5. **Action bar shows** real-time distance and direction
6. **Compass points** to Player B's location

### Compass Features
- **Right-click** to refresh location (with cooldown)
- **Cannot be dropped** or moved to external inventories
- **Auto-removed** when tracking stops
- **Glows** with enchantment effect

---

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| **Compass not removed** | Use `/findbuddy cleanup <player>` |
| **Permission denied** | Check `findbuddy.find` permission |
| **Players can't track** | Verify `require_requests` setting |
| **Compass not working** | Check if players are in same world |

---

## 👨‍💻 For Developers

### Quick Start
```bash
# Clone and build
git clone https://github.com/hellofaizan/FindBuddy.git
cd FindBuddy
./gradlew build

# Run with test server
./gradlew runServer
```

### Project Structure
```
src/main/kotlin/in/mohammadfaizan/minecraft/
├── FindBuddy.kt                    # Main plugin class
├── commands/FindBuddyCommand.kt    # Command executor
├── listeners/                      # Event handling
├── managers/                       # Business logic
├── models/                         # Data models
├── utils/                          # Utilities
└── ui/                            # UI components
```

### Key Components
- **TrackingManager** - Core tracking logic and task management
- **CompassManager** - Compass item creation and management
- **ConfigManager** - Type-safe configuration access
- **MessageUtils** - Consistent UI message formatting

### Contributing
1. **Fork** the repository
2. **Create** feature branch
3. **Make changes** following Kotlin conventions
4. **Add tests** for new functionality
5. **Submit** pull request

---

## 📄 License

This project is licensed under the **MIT License**.

---

<div align="center">

**⭐ Star this repository if you find it useful!**  
**🐛 Report issues on GitHub**  
**💡 Suggest features and improvements**

Made with ❤️ by [Mohammad Faizan](https://mohammadfaizan.in)

</div> 