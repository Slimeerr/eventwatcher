<h1 align="center">EventWatcher</h1>

<p align="center">
  <img src="https://github.com/user-attachments/assets/bc3d3eb4-2067-4ece-8d9f-cc722c089109" width="300" alt="EventWatcher Logo">
</p>

<p align="center">
  <strong>Watch Discord. Join faster.</strong>
</p>

<p align="center">
  A client-side Fabric mod for <strong>Minecraft 1.21.11</strong> that watches Discord channels for event drops and either notifies you or automatically connects you to your target server the instant a matching message appears.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-5E9C36" alt="Minecraft">
  <img src="https://img.shields.io/badge/Loader-Fabric-orange" alt="Fabric">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java">
  <img src="https://img.shields.io/github/license/Slimeerr/eventwatcher" alt="License">
</p>

---

Ever missed an event because someone posted it in Discord before you saw it?

**EventWatcher** watches the channel for you and reacts instantly. Whether you just want a notification or want Minecraft to automatically connect to your target server, EventWatcher is designed to get you there as fast as possible.

It works both from the **Minecraft Title Screen** and **while you're already in-game**, so you never have to sit refreshing Discord again.

> [!NOTE]
> EventWatcher works both from the Minecraft title screen and while connected to another server.

> [!WARNING]
> This project was built with AI assistance ("vibecoding"). If that isn't your thing, this project probably isn't for you.

> [!CAUTION]
> Using a **User Token (selfbot)** violates Discord's Terms of Service. If you choose to use one, use an alternate account—not your primary account.

> [!CAUTION]
> Use this mod at your own risk. Any bans, restrictions, or other actions taken by Discord, Minecraft servers, or third parties are your responsibility.

---

## 📺 Video Tutorial

New to EventWatcher? Here's a quick setup walkthrough:

<video src="https://raw.githubusercontent.com/Slimeerr/eventwatcher/main/media/tutorial.mp4" controls width="100%"></video>

[▶ Or watch it here](media/tutorial.mp4) if the player above doesn't load.

---

# ✨ Features

## 🚀 Multiple Server Profiles

Configure as many servers as you want.

Each profile has its own:

- Discord channel(s)
- Keywords
- Target Minecraft server
- Notify Me / Auto-Connect mode
- Cooldown timer

---

## ⚡ Fast Discord Detection

Choose whichever authentication method fits your situation.

### Bot Token (Recommended)

- Uses Discord's Gateway API
- Receives messages instantly
- Fastest possible detection
- Fully supported by Discord

Perfect if you own the Discord server or can invite a bot.

### User Token (Selfbot)

- Uses Discord's REST API
- Polls Discord periodically
- Useful when bots cannot be added
- Violates Discord's Terms of Service

---

## 🎯 Smart Keyword Matching

Designed to avoid false positives.

Supports:

- Whole-word matching
- Negative keywords using `!`
- Per-server cooldowns

Examples:

```
event
!ended
```

The keyword `event` won't trigger on words like `prevent`.

---

## 🔄 Retry Until Joined

If your event server is:

- Full
- Queueing
- Temporarily unavailable

EventWatcher will continue trying until Minecraft successfully joins.

---

## 🔔 Notification Queue

Missed notifications are a thing of the past.

Every detected event gets its own notification containing:

- Join button
- Discord username
- Discord avatar

Multiple events can stay queued at once.

---

## 🎵 Custom Sounds

Choose from:

- Built-in Minecraft sounds
- Your own custom `.wav` files

---

## 🌌 Galaxy Interface

A custom UI with:

- Animated starfield
- Galaxy gradient theme
- Modern interface
- Smooth animations

---

## 🛠 Quality of Life

- Test Alert button
- Persistent Events Caught counter
- Keyword collision warnings
- Helpful hover tooltips
- `/ewjoin` command
- ModMenu integration
- Works on the Title Screen
- Works while connected to servers

---

# 📸 Screenshots

*Coming soon.*

---

# 🚀 Installation

1. Install **Fabric Loader** for **Minecraft 1.21.11**
2. Install **Fabric API**
3. *(Optional)* Install **ModMenu**
4. Download the latest EventWatcher release
5. Place the JAR inside your `mods` folder
6. Launch Minecraft

---

# ⚙️ Quick Start

1. Open EventWatcher.
2. Choose your token type.
3. Enter your Discord token.
4. Add one or more Discord channels.
5. Configure your keywords.
6. Enter your target server.
7. Choose **Notify Me** or **Auto-Connect**.
8. Save.

You're done.

---

# ⚙️ Configuration

Configuration is stored in:

```text
.minecraft/config/eventwatcher.json
```

> [!NOTE]
> Your Discord token is stored in plain text inside the configuration file. Keep it private and never share it.

---

# 🖥 Command

```text
/ewjoin
```

Immediately connects you to the configured target server.

---

# ❓ FAQ

### Is EventWatcher client-side?

Yes.

Everything runs on your Minecraft client.

---

### Does it work while I'm already playing?

Yes.

EventWatcher works both on the Title Screen and while connected to another server.

---

### Can I monitor multiple servers?

Yes.

Each server can have its own:

- Discord channels
- Keywords
- Mode
- Cooldown
- Target server

---

### Which token type should I use?

Whenever possible, use a **Bot Token**.

Only use a **User Token** if you cannot add a bot to the Discord server.

---

# 🔧 Troubleshooting

### Nothing happens

- Verify your Discord Channel IDs.
- Verify your keywords.
- Make sure your bot has permission to read the channel.
- For bot tokens, ensure **Message Content Intent** is enabled.

---

### It joins the wrong server

Check your configured target server address.

---

### My custom sound doesn't play

Ensure the file is a valid `.wav`.

---

# 🏗 Building

Requires:

- JDK 21
- Java 21

Clone the repository:

```bash
git clone https://github.com/Slimeerr/eventwatcher.git
cd eventwatcher
```

Build:

Linux/macOS

```bash
./gradlew build
```

Windows

```bat
gradlew.bat build
```

The compiled JAR will be located inside:

```text
build/libs/
```

---

# 📋 Requirements

- Minecraft 1.21.11
- Fabric Loader
- Fabric API
- Java 21

---

# 🗺 Roadmap

- [x] Multi-server support
- [x] Notification queue
- [x] Retry Until Joined
- [x] Whole-word keyword matching
- [x] Negative keywords
- [x] Custom sounds
- [x] Galaxy UI
- [ ] More notification customization
- [ ] Additional quality-of-life improvements

---

# 📄 License

Released under the **MIT License**.

See the [LICENSE](LICENSE) file for more information.

---

<p align="center">
Made for people who'd rather be playing Minecraft than refreshing Discord.
</p>

