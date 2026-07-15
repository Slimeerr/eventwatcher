# EventWatcher

> A client-side Fabric mod for Minecraft **1.21.11** that watches a Discord channel for keywords and connects you to an event server the moment one drops. Works straight from the Title Screen, so you catch events even when you're not in a world.

---

> [!CAUTION]
> **Account risk.** The "selfbot" (User token) option goes against Discord's TOS. It usually doesn't flag anything, but use an alt or a throwaway account for this, **not your main**.

> [!WARNING]
> **This mod is AI work and vibecoded.** If you don't want to support AI work, don't download it.

> [!NOTE]
> Use this mod at your own risk. I'm not responsible for any account bans, restrictions, or anything else that happens from using it.

---

## Features

- Watches a Discord channel over the gateway (WebSocket), or falls back to REST polling
- Auto-connects to your target server when a keyword fires, or just notifies you
- In-game settings screen for token, channel, keywords, sounds, and mode
- Custom notification sounds, either built-in Minecraft ones or your own file
- `/ewjoin` command to jump to the target server whenever you want
- Buttons on the Title Screen and pause menu

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for 1.21.11 and [Fabric API](https://modrinth.com/mod/fabric-api).
2. (Optional but recommended) grab [ModMenu](https://modrinth.com/mod/modmenu) so you get a settings button in the mods list.
3. Drop `eventwatcher-1.0.0.jar` into your `mods` folder.
4. Launch the game.

## Setup

Open the settings screen two ways: click the **EventWatcher** button on the Title Screen, or open it through ModMenu. You can also bind a key under `Options > Controls > Key Binds`.

| Field | What it does |
| --- | --- |
| **Discord Token** | The token EventWatcher uses to read the channel. Hit the token-type button to switch between **Bot** and **User**. Bot is safe. User is the selfbot mode, so read the warning above. |
| **Channel ID** | The channel you want watched. Turn on Developer Mode in Discord (`Settings > Advanced`), then right-click the channel and **Copy ID**. |
| **Keywords** | Type a word and click **Add**. When any of these show up in the channel, the mod fires. |
| **Mode** | **Notify Me** puts a button on screen so you pick when to join. **Auto-Connect** joins on its own. |
| **Target Server** | The server address it connects you to. |
| **Sound...** | A notification sound, either a built-in Minecraft one or a file of your own. |

Hit **Save**. When a keyword lands, EventWatcher does its thing based on your mode.

## Building from source

```bash
./gradlew build
```

The jar drops into `build/libs/`.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader `>= 0.19.0`
- Fabric API
- Java **21**
- ModMenu (optional, for the settings button)

## License

[MIT](LICENSE)
