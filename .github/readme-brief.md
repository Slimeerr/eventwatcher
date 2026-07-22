Write a polished GitHub README.md for my Minecraft mod "EventWatcher".
Use GitHub-flavored markdown, including > [!WARNING] / > [!CAUTION] / > [!NOTE] alert boxes.
Keep a casual, friendly tone. Output raw markdown only — no surrounding code fences, no commentary.

WHAT IT IS
- Client-side Fabric mod for Minecraft 1.21.11
- Watches one or more Discord channels for keywords, then either notifies you or auto-connects you to a target Minecraft server the instant an event drops
- Works from the Title Screen and in-game

KEY FEATURES (v1.5)
- Multiple target servers, each with its own Discord channel(s), keywords, and mode (Notify Me / Auto-Connect)
- Bot token uses Discord's live gateway (fastest); User token (selfbot) polls via REST
- Match precision: whole-word matching (so "event" won't fire on "prevent"), negative keywords with a "!" prefix (e.g. "!ended"), and a per-server cooldown
- Retry-until-joined: keeps re-attempting a full/queued event server until you get in
- Notification queue: multiple pending events, each its own join button; the button shows who posted, with their Discord avatar
- Test Alert button, persistent "events caught" counter, keyword-collision warning, hover tooltips
- Custom sounds (built-in Minecraft sounds or your own .wav)
- /ewjoin command to jump to the target server on demand
- A "galaxy" gradient UI theme with an animated starfield
- Config lives at .minecraft/config/eventwatcher.json (token stored in plain text)

WARNINGS TO INCLUDE
- Using a User token (selfbot) violates Discord's Terms of Service — use an alt/throwaway account, not your main
- This mod is AI-assisted / vibecoded; if that's not your thing, don't download it
- Use at your own risk (bans/restrictions are on you)

INSTALL
1. Fabric Loader for 1.21.11 + Fabric API
2. (Optional) ModMenu
3. Download the jar from the latest release and drop it in your mods folder
4. Launch

BUILD FROM SOURCE
- Needs JDK 21; run ./gradlew build (gradlew.bat on Windows); jar lands in build/libs/

REPO / RELEASE
- github.com/Slimeerr/eventwatcher
- Latest release: v1.5.0
- License: MIT
