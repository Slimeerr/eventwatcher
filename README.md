# IMPORTANT: AI & Account Risk
Using the "selfbot" option goes against Discord TOS, but usually doesn't flag anything. It's recommended to a alt or a throwaway account for ts. 
This mod uses AI work and is vibecoded, if you don't want to support AI work then don't download this.




# EventWatcher

A client-side Fabric mod for Minecraft 1.21.11 that watches a Discord channel for keywords and instantly connects you (or notifies you to connect) to an event server — works straight from the Title Screen.

## Features

- Connects to the Discord gateway (WebSocket) or falls back to REST polling to scan a channel for configured keywords
- Auto-joins (or prompts you to join) a target server when a keyword fires
- In-game settings screen (keybind or ModMenu) for token, channel, keywords, sounds, and behavior
- `/ewjoin` client command to connect to the target server on demand
- Title Screen and pause menu integration


## Building

```
./gradlew build
```

The built jar lands in `build/libs/`.

## Requirements

- Minecraft 1.21.11, Fabric Loader ≥ 0.19.0, Fabric API
- Java 21
- ModMenu (optional, for the config screen entry)

## License

MIT — see [LICENSE](LICENSE).
