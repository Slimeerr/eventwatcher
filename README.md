# EventWatcher

> A client-side Fabric mod for Minecraft **1.21.11** that watches a Discord channel for keywords and connects you to an event server the moment one drops. Works straight from the Title Screen, so you catch events even when you're sitting at the menu.

---

> [!CAUTION]
> **Account risk.** The selfbot (User token) option goes against Discord's TOS. It usually doesn't flag anything, but use an alt or a throwaway account for this, **not your main**.

> [!WARNING]
> **This mod is AI work and vibecoded.** If you don't want to support AI work, don't download it.

> [!NOTE]
> Use this mod at your own risk. I'm not responsible for any account bans, restrictions, or anything else that happens from using it.

---

## What it does

You give EventWatcher a Discord channel and a list of keywords. It watches that channel in the background while your game runs. When someone posts a message containing one of your keywords, the mod either drops you onto your target server or pings you with a button to join. Event servers fill up in seconds, so the point is to beat everyone else to the queue.

It runs the whole time, even on the Title Screen, so you don't have to be in a world to catch a drop.

### How it watches the channel

The mod picks its method based on your token type:

- **Bot token:** it opens a live WebSocket to Discord's gateway. Messages hit you the instant they're posted. This is the fast path.
- **User token:** it polls the channel over REST every few seconds. Still quick, but there's a small gap between the post and the mod seeing it.

If you have the choice, a bot token reacts faster.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for 1.21.11.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and put it in your `mods` folder. EventWatcher needs it.
3. (Optional but recommended) grab [ModMenu](https://modrinth.com/mod/modmenu). It adds a settings button to the mod list, which is the easiest way into the config.
4. Download `eventwatcher-1.0.0.jar` from the [latest release](https://github.com/Slimeerr/eventwatcher/releases/latest) and drop it into your `mods` folder.
5. Launch the game. You'll see an **EventWatcher** button on the Title Screen once it loads.

Your `mods` folder lives at:

- **Windows:** `%appdata%\.minecraft\mods`
- **macOS:** `~/Library/Application Support/minecraft/mods`
- **Linux:** `~/.minecraft/mods`

If you use a launcher like Prism, MultiMC, or the Modrinth app, drop it into that instance's mods folder instead.

## Quick start

For the impatient, here's the short version. The rest of the README explains each step in full.

1. Open the settings screen (Title Screen button or ModMenu).
2. Paste in a Discord token and set the token type to **Bot** or **User**.
3. Paste in the **Channel ID** of the channel you want watched.
4. Add a few **keywords**.
5. Type your **Target Server** address.
6. Pick **Notify Me** or **Auto-Connect**.
7. Hit **Save**.

## Setup, field by field

Open the settings screen two ways: click the **EventWatcher** button on the Title Screen, or open it through ModMenu. You can also bind a key for it under `Options > Controls > Key Binds`, then press that key from anywhere.

| Field | What it does |
| --- | --- |
| **Discord Token** | The token EventWatcher logs in with to read the channel. The token-type button next to it flips between **Bot** and **User**. Which one you want depends on whose server it is, covered in the next section. |
| **Channel ID** | The exact channel the mod watches. Not the server, the channel. See [How to get a Channel ID](#how-to-get-a-channel-id). |
| **Keywords** | Type a word, click **Add**, and it joins the list. A message trips the mod when it contains any keyword on the list. Add as many as you want. |
| **Mode** | **Notify Me** or **Auto-Connect**. See [Modes](#modes) for the difference. |
| **Target Server** | The Minecraft server address the mod sends you to, like `play.example.net`. |
| **Sound...** | Opens the sound screen, where you set the alert noise. See [Sounds](#sounds). |

Hit **Save** when you're done. **Cancel** throws away your changes.

## Which token do I use?

This is the part people get wrong, so read it.

- **It's your own Discord server** (you own it or you're an admin): make a bot and use its **Bot** token. Discord allows this, it's the intended way, and it never touches your personal account. Set the token type to **Bot**.
- **It's someone else's server** and you can't add a bot: use your own account's **User** token, which is the selfbot mode. This breaks Discord's TOS. It rarely gets flagged, but the risk is a ban, so run it on an alt you don't care about. Set the token type to **User**.

Rule of thumb: bot for your server, selfbot for someone else's.

## How to get a Channel ID

1. Open Discord and go to **Settings > Advanced**.
2. Turn on **Developer Mode**.
3. Go back to your server and right-click the channel you want to watch.
4. Click **Copy Channel ID** at the bottom of the menu.
5. Paste it into the **Channel ID** field in EventWatcher.

The ID is a long string of numbers. If you right-clicked and don't see **Copy Channel ID**, Developer Mode isn't on yet.

## How to set up a bot and get its token

Use this when you're watching your own server.

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications) and log in.
2. Click **New Application** in the top right. Give it a name and hit **Create**.
3. Open the **Bot** tab on the left.
4. Click **Reset Token**, confirm, then **Copy**. That string is your bot token. Treat it like a password and don't share it. If you lose it, reset it again for a new one.
5. Scroll down to **Privileged Gateway Intents** and turn on **Message Content Intent**. Without this, your bot connects but reads none of the message text, so the mod never sees your keywords. This one trips up most people.
6. Open the **OAuth2** tab, then **URL Generator**.
7. Under **Scopes**, check `bot`.
8. Under **Bot Permissions**, check **View Channels** and **Read Message History**.
9. Copy the URL at the bottom of the page, paste it into your browser, pick your server, and click **Authorize**. Your bot joins the server.
10. Make sure the bot can actually see the channel you're watching. If the channel is private, give the bot's role access.
11. In EventWatcher, set the token type to **Bot** and paste the token.

## How to get your User token (selfbot)

Use this only when you can't add a bot, and only on an alt.

> [!CAUTION]
> A user token is full access to your account. Anyone who has it can log in as you, read your DMs, and get you banned. Never paste it anywhere but your own settings, never share it, and never use your main.

1. Open Discord in your web browser, not the desktop app, and log in on your alt.
2. Press `F12` to open Developer Tools.
3. Go to the **Network** tab.
4. Do something that makes Discord talk to its servers, like clicking into a channel or sending a message.
5. In the list of requests, click one that goes to `discord.com/api`.
6. Open the **Headers** section for that request and look for the `authorization` header. The value next to it is your token.
7. In EventWatcher, set the token type to **User** and paste it.

If you change your password or log out everywhere, the token dies and you'll have to grab a fresh one.

## Modes

Set this with the **Notify Me** / **Auto-Connect** toggle in settings.

- **Notify Me:** when a keyword hits, the mod puts a join button in front of you and plays your sound. You choose when to click it. Good if you want to finish what you're doing first, or double-check the event before jumping.
- **Auto-Connect:** when a keyword hits, the mod connects you to the target server on its own, no click needed. Good for events where a half-second head start matters. If you're already on that server, it leaves you alone.

## Keywords

- Add a keyword by typing it in the box and clicking **Add**.
- A message trips the mod if it contains any single keyword from your list.
- Keep them specific. A keyword like `e` matches almost everything and fires constantly. A keyword like `event start` or a host's name is tighter.
- Think about what the channel actually posts when an event goes live, and match that.

## Sounds

Open the sound screen with the **Sound...** button in settings.

- **Sound: ON / OFF** toggles the alert noise.
- **Source** flips between a built-in Minecraft sound and a custom file.
- **Minecraft sound:** cycle through the built-in list with the button. Each click plays a preview. The options are:

  | Sound ID | What it is |
  | --- | --- |
  | `block.note_block.pling` | Note block pling (default) |
  | `block.note_block.bell` | Note block bell |
  | `block.bell.use` | Bell ring |
  | `entity.experience_orb.pickup` | XP pickup blip |
  | `entity.player.levelup` | Level-up chime |
  | `ui.button.click` | UI click |
  | `block.amethyst_block.chime` | Amethyst chime |
  | `entity.arrow.hit_player` | Arrow hit ding |
  | `entity.ender_eye.death` | Ender eye shatter |

- **Custom .wav file:** switch the source to file, then paste the full path to a `.wav` on your machine. It has to be WAV, not MP3 or OGG.
- **Test sound** plays your current pick so you can check it before you rely on it.
- Hit **Done** to go back.

## Commands

- `/ewjoin` connects you to your target server right away, no keyword needed. Handy when you already know an event is live and just want the fast join.

## Where your settings live

EventWatcher saves everything to:

```
.minecraft/config/eventwatcher.json
```

Your token sits in that file in plain text, so don't hand your config folder to anyone. If you ever want to wipe your setup, delete that file and the mod rebuilds it with defaults next launch.

## Troubleshooting

- **No EventWatcher button on the Title Screen:** the mod didn't load. Check that Fabric API is in your mods folder and that you're on 1.21.11.
- **Nothing happens when a keyword gets posted:** using a bot token, make sure **Message Content Intent** is on and the bot can see the channel. Double-check the **Channel ID** is the right channel. Confirm your keyword actually appears in the message.
- **It connects to the wrong place:** check the **Target Server** field for a typo.
- **Bot token got leaked:** reset it in the Developer Portal. **User token got leaked:** change your Discord password, which kills the token.

## Building from source

You need a JDK 21 on your machine. Then:

```bash
git clone https://github.com/Slimeerr/eventwatcher.git
cd eventwatcher
./gradlew build
```

The finished jar lands in `build/libs/`. On Windows, use `gradlew.bat build` instead.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader `>= 0.19.0`
- Fabric API
- Java **21**
- ModMenu (optional, for the settings button)

## License

[MIT](LICENSE). Do what you want with it.
