
# EventWatcher

<img width="360" height="360" alt="image" src="https://github.com/user-attachments/assets/bc3d3eb4-2067-4ece-8d9f-cc722c089109" />


<p align="center">
  <h3 align="center">Watch Discord. Join faster.</h3>
  <p align="center">
    A client-side Fabric mod for <b>Minecraft 1.21.11</b> that monitors a Discord channel for keywords and notifies you or automatically joins your target server when a match is found.
  </p>
</p>


<p align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-5E9C36)
![Loader](https://img.shields.io/badge/Loader-Fabric-orange)
![Java](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/github/license/Slimeerr/eventwatcher)

</p>

> [!NOTE]
> EventWatcher works from both the Minecraft title screen and in-game.

> [!CAUTION]
> User tokens (selfbots) violate Discord's Terms of Service. If you choose to use one, use an alternate account and understand the risks.

## Features

- Monitor a Discord channel for keywords
- Works on the title screen and in-game
- Supports Bot and User tokens
- Notify or Auto-Connect modes
- Custom notification sounds
- Simple configuration screen
- ModMenu integration

## Installation

1. Install Fabric Loader for **1.21.11**
2. Install Fabric API.
3. (Optional) Install ModMenu.
4. Download the latest EventWatcher release.
5. Place the JAR in your `mods` folder.
6. Launch Minecraft.

## Quick Start

1. Open **EventWatcher** from the title screen or ModMenu.
2. Choose a token type.
3. Enter your Discord token.
4. Enter the Channel ID.
5. Add one or more keywords.
6. Enter your target server.
7. Choose **Notify Me** or **Auto-Connect**.
8. Save your settings.

## Configuration

| Setting | Description |
| --- | --- |
| Discord Token | Token used to access Discord. |
| Channel ID | Channel to monitor. |
| Keywords | Words or phrases that trigger EventWatcher. |
| Mode | Notify Me or Auto-Connect. |
| Target Server | Server to join when triggered. |
| Sound | Configure notification sounds. |

## Token Types

### Bot Token

Recommended whenever possible.

- Real-time Discord events
- Official Discord bot support
- Fastest option

Use this if you own the server or can invite bots.

### User Token

Use this only if you cannot add a bot.

- Polls the channel periodically
- Slightly slower than a bot token
- Violates Discord's Terms of Service

## Modes

### Notify Me

Displays a notification, plays your selected sound, and lets you join manually.

### Auto-Connect

Automatically connects you to your configured server when a keyword matches.

## Sounds

Choose from several built-in Minecraft sounds or provide your own `.wav` file.

Supported custom format:

- `.wav`

## Command

```text
/ewjoin
```

Immediately connects to your configured target server.

## Configuration File

```text
.minecraft/config/eventwatcher.json
```

Your token is stored in plain text. Keep the file private.

## Troubleshooting

**Nothing happens when a keyword is posted**

- Verify the Channel ID.
- Verify your keywords.
- Check the bot has access to the channel.
- Make sure Message Content Intent is enabled for bot tokens.

**Wrong server**

Check your configured target server address.

## Building

Requirements:

- Java 21
- JDK 21

```bash
git clone https://github.com/Slimeerr/eventwatcher.git
cd eventwatcher
./gradlew build
```

Windows:

```bash
gradlew.bat build
```

The compiled JAR will be available in `build/libs/`.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.0+
- Fabric API
- Java 21

## License

Released under the MIT License. See [LICENSE](LICENSE) for details.
