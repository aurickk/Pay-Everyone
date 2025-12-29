<p align="center">
<img src="https://github.com/user-attachments/assets/14690fb8-6bfc-4e4c-8fed-d533ec0c3781" alt="Pay Everyone" width="15%"/>
</p>

<h1 align="center">Pay Everyone</h1>

<p align="center">A Minecraft Fabric mod that pays all online players automatically. Be like MrBeast!</p>

## What It Does

A GUI that automatically opens with your inventory to pay all online players with one click. Discovers players, randomizes payment order, and send commands with configurable delays.


![paydemo](https://github.com/user-attachments/assets/1e32f506-f7b8-46ac-a46d-8d8bd6c27397)

## Requirements

- **Minecraft** 1.21.1 – 1.21.10
- **Fabric Loader** 0.15.0+
- **Fabric API** (matching your Minecraft version)

### Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version
2. Download the latest [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version
3. Download the latest `pay-everyone-[minecraft_version]-[version].jar` from the [Releases](https://github.com/aurickk/Pay-Everyone/releases/) page
4. Place both mods in your `.minecraft/mods` folder
5. Launch Minecraft

## How to Use

The GUI opens automatically when you open your inventory. Use the minimize button to hide the GUI and `/payeveryone show` to unhide the menu. Click the pin button to keep it visible when inventory is closed.

### Main Tab

| Field | Description |
|-------|-------------|
| **Amount** | Amount per player. Supports `1000`, `5k`, `2.5m`, or ranges like `100-500` |
| **Auto** | Divides your total balance equally among all players |
| **Delay** | Milliseconds between each payment (default: 1000ms) |
| **Enable TabScan** | Automatically scan for players before paying |
| **Start / Pause / Cancel** | Control payment process |

### Players Tab

- **Add Player** — Manually add players to pay
- **Exclude Player** — Skip specific players
- **Online Players List** — View discovered players; right-click to exclude

### Scan Tab

Discovers players on large servers by querying the server's `/pay` command autocomplete.

- **Scan Interval** — Delay between queries (default: 50ms)
- **Start Scan / Cancel / Clear List** — Control the scan

### Settings Tab

| Setting | Description |
|---------|-------------|
| **Pay command** | Change from `/pay` if your server uses something else |
| **Reverse syntax** | Use `/pay <amount> <player>` instead of `/pay <player> <amount>` |
| **Auto Confirm** | Automatically click a confirmation button slot ID |
| **Double Send** | Send each payment command twice |
| **Keybinds** | View/change the force-stop keybind (default: **J**) |

## Commands & Keybinds

- `/payeveryone show` — Show the GUI
- `/payeveryone hide` — Hide the GUI
- **J** (default) — Force stop payment or scan

Change keybinds in **Options → Controls → Key Binds → Pay Everyone**.



## Building from Source

### Prerequisites

- **Java 21** or higher
- **Gradle** (included via wrapper)

### Building the Minecraft Mod

1. **Clone the repository**
   ```bash
   git clone https://github.com/aurickk/Pay-Everyone.git
   cd Pay-Everyone
   ```

2. **Build the mod**
   ```bash
   # Windows
   .\gradlew.bat build
   
   # Linux/Mac
   ./gradlew build
   ```

Output JARs are in `legacy/build/libs/` (1.21.1 - 1.21.5) and `modern/build/libs/` (1.21.6 - 1.21.10).
