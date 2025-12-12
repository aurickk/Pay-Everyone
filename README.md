
<p align="center">


<img src="https://github.com/user-attachments/assets/14690fb8-6bfc-4e4c-8fed-d533ec0c3781" alt="fake-mister-beast" width="15%"/>
</p>
<h1 align="center">Pay Everyone</h1>

<p align="center">A client-side Fabric mod that automatically scans and pay all online players on multiplayer servers. Be like Mister Beast!</p>


## Features

- **Pay Everyone**: Automatically discorvers and pay all online players with a single command
- **Customizable Delays**: Set delay between each payment to prevent command spam kick
- **Auto-Confirm**: Automatically click confirmation buttons
- **Player Exclusions**: Exclude specific players from receiving payments
- **Randomized Payment Order**: Payments are sent in random order to avoid patterns
- **Random Amounts**: Support for random payment amounts within a range (e.g., 300-3000)

## Requirements

- **Minecraft**: 1.21.4 - 1.21.10
- **Fabric Loader**: 0.15.0 or higher
- **Fabric API**: Latest version for your Minecraft version

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version
2. Download the latest [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version
3. Download the latest `pay-everyone-[version]-[mod_version].jar` from the [Releases](https://github.com/aurickk/pay-everyone/releases/) page
4. Place both mods in your `.minecraft/mods` folder
5. Launch Minecraft

## Setup

Not always necessary but allows for more customizations

### Exclude Players

Exclude specific players from receiving payments.

```
/payall exclude <player1> <player2>
/payall exclude Player1 Player2
```

### Automatically Confirm Menus 

Some servers show a confirmation menu before processing each payment. The auto-confirm feature clicks the assigned clickslot button to automatically confirm each payment.

```
/payall confirmclickslot <slot>           # Enable auto-click on slot ID
/payall confirmclickslot <slot> <delay>   # Enable with custom delay (50-2000ms)
/payall confirmclickslot                  # Show current status
/payall confirmclickslot off              # Disable auto-confirm
```
<img width="250" height="250" alt="image" src="https://github.com/user-attachments/assets/d1d47821-481e-4a63-9856-e0f281ebb0af" />

Chest inventory with slot IDs for reference

### Different Pay Command
Use `/payall command /yourcommand` if your server uses something other than `/pay`.

## Usage

```
/payall <amount> <delay>              # Discovers and pay <amount> to players (set <delay> in ms in between)
/payall <amount1>-<amount2> <delay>   # Payment amount could be a set range (Payment amount randomized)
/payall <amount/range> auto <delay>   # Automatically calculates payment per player 
```
1. **Amount Parsing**: Converts shortened numbers (k/m/b/t) to actual values
   - `4.9k` → 4,900, `2.5m` → 2,500,000, `1b` → 1,000,000,000, `5t` → 5,000,000,000,000
3. **`Auto`** divides amount of money by the number of players (for distributing entire balances equally)
2. **Tab Scan**: Scans all prefixes (a-z, 0-9, _) on servers with payment username autocomplete
3. **Fallback**: If scan finds no players, uses players from the tab list instead
4. **Confirmation**: Shows dialog with payment configuration status
5. **Payment**: After `/payall confirm`, pays all players in random order with the applied settings

### Stop Payment Process

```
/payall stop
```
Stops the current payment process if one is running.

**Tip**: You can also press the **K** key (default keybind) to stop payments in progress


## Additional Options

### Keybinds

| Action | Default Key | Description |
|--------|-------------|-------------|
| Stop Payment/Scan | `K` | Instantly stops any running payment or tab scan |

**To change**: Options → Controls → Key Binds → Pay Everyone

### Tab Scan

Tab scan automatically starts when `/payall` is ran in servers with payment autocomplete, however, you can still run it manually.  

```
/payall tabscan <delay>            # Reads through /pay autofills to find avaliable players to pay, delay between each prefix search adjustable
/payall tabscan stop               # Stop scan in progress
```

Tab scan queries the server's `/pay` command autocomplete to discover players beyond the tab list limit. This is useful for large servers with hundreds of players that could not be include in the tab list.

**Note**: Moving while tab scan is in progress disrupts the process

**Tip**: You can also press the **K** key (default keybind) to instantly stop tab scan

### Manually Add Players to Pay List
```
/payall add <player1> <player2> <player3>
/payall add Player1 Player2 Player3
```

### Remove Manually Added Players 
```
/payall remove exclude <player1> <player2>    # Remove from exclusion list
/payall remove add <player1> <player2>       # Remove from manual add list
```

### Clear Lists
```
/payall clear all          # Clear everything
/payall clear exclude      # Clear excluded players
/payall clear add          # Clear manually added players
/payall clear tabscan      # Clear tab scan results
```

### View Player Lists
```
/payall list                       # Show debug info
/payall list tabscan               # List tab scan players
/payall list add                   # List manually added players
/payall list exclude                # List excluded players
/payall list tablist                # List default tab menu players
```
### Double Send

Some servers require sending the payment command twice to confirm one payment.

```
/payall doublesend              # Show current status
/payall doublesend <delay>      # Enable double send with delay (ms, 0 for no delay)
/payall doublesend off          # Disable double send
```

When enabled, each payment command will be sent twice to the same player. The delay ensures proper order: first command → wait for delay → second command → move to next player.

## How It Works

 **Player Discovery**: The mod collects players from multiple sources:
   - Default tab menu (limited to ~150 players)
   - Tab scan (queries server payment command autocomplete)
   - Tab menu player list (fallback when payment command could not be qeuried, limited to ~150 players)
   - Manual player list (you add players)

**Payment Process**: 
   - Player order are randomized before payment
   - Waits for a set interval between each command excution
   - Excluded players are skipped
   - Process can be stopped at any time

**Tab Scan**: 
   - Sends autocomplete requests for `/pay` command with different prefixes
   - Scans through: empty, a-z, 0-9, and underscore
   - Collects all player names from server responses
   - Takes time but discovers many more players

 **Auto-Confirm**: 
   - When enabled, monitors for confirmation menus opening
   - Automatically clicks the specified slot after a short delay
   - Only active during payment process
   - Configurable slot ID and click delay
   - When double send enabled, the same payment is sent twice
   
## Building from Source

### Prerequisites

- Java 21 or higher
- Gradle (included via wrapper)

### Build Steps

The mod supports building for Minecraft versions 1.21.4 - 1.21.10

1. Clone the repository:
   ```bash
   git clone https://github.com/aurickk/pay-everyone/
   cd Pay-Everyone
   ```

2. **Windows**: Run `build.bat` and select the version to build
   
   **Linux/Mac**:
   ```bash
   ./gradlew clean build
   ```

3. Built JARs will be in `build/libs/`:
   - `pay-everyone-[version]-[mod_version].jar`




