# TNT Trails

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=flat-square)](https://www.minecraft.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.2%2B-DBD0B4?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-E76F00?style=flat-square)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey?style=flat-square)](LICENSE)

TNT Trails is a client-side Fabric mod that records the movement of primed TNT entities and renders their trajectory directly in the world.

It provides a live TNT ESP, configurable trail rendering, post-explosion ESP persistence, a custom in-game interface, and automatic update checks.

## Features

- Records the complete movement path of primed TNT entities
- Renders trails with configurable color, opacity, and line width
- Displays a live ESP box from ignition until explosion
- Keeps the final ESP position visible after the explosion
- Applies a smooth fade to trails and lingering ESP boxes
- Includes an in-game configuration menu
- Saves settings automatically between sessions
- Checks GitHub Releases for updates on startup
- Runs entirely on the client without server-side installation

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | `1.21.11` |
| Java | `21` or newer |
| Fabric Loader | `0.19.2` or newer |
| Fabric API | Compatible with Minecraft `1.21.11` |
| Fabric Language Kotlin | Compatible with the installed Fabric environment |

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Fabric API and Fabric Language Kotlin.
3. Download `tnt-trails-1.0.0.jar` from the latest GitHub Release.
4. Place the JAR inside the `mods` directory of your Minecraft profile.
5. Start Minecraft and join a world or server.

For Lunar Client, use the Fabric mods directory associated with the Minecraft `1.21.11` profile.

## Usage

Open the configuration menu with:

```text
/tnttrails
```

The menu does not pause the game. Changes are applied immediately and saved when the screen closes.

## Configuration

| Setting | Default | Description |
| --- | ---: | --- |
| `Enabled` | On | Enables or disables all TNT Trails functionality. |
| `TNT ESP` | On | Renders a box around active TNT and its final position after explosion. |
| `Trail Delay` | `3000 ms` | Controls how long a completed trail remains visible. |
| `Line Width` | `2.0` | Controls the thickness of trail and ESP outline rendering. |
| `ESP Hold` | `1000 ms` | Controls how long the final ESP box remains after the TNT explodes. Set it to `0` to disable post-explosion persistence. |
| `Red` | `255` | Red component of the render color. |
| `Green` | `72` | Green component of the render color. |
| `Blue` | `72` | Blue component of the render color. |
| `Alpha` | `220` | Opacity of trails and ESP rendering. |

The color preview in the menu updates as the RGBA values change.

## How It Works

When a TNT entity is ignited, the mod begins recording its position every client tick.

While the TNT is active:

- Its current position is highlighted by the TNT ESP.
- Every movement point is added to its trajectory.

When the TNT explodes or is removed:

- Its recorded trajectory becomes a completed trail.
- The trail remains visible for the configured `Trail Delay`.
- The ESP remains at the final recorded position for the configured `ESP Hold`.
- Both effects fade independently according to their configured durations.

Tracking data is cleared automatically when changing worlds, disconnecting, or disabling the mod.

## Configuration File

Settings are stored in:

```text
config/tnt-trails.json
```

The file is created automatically. Existing configurations are normalized and migrated when new settings are introduced.

## Update Checker

TNT Trails checks the latest release from the configured GitHub repository when the client starts.

When a newer version is available:

- A prompt is displayed after the game reaches a screen.
- `Download` opens the release or JAR download page.
- `Ignore` dismisses the prompt for the current session.
- The download link remains available inside `/tnttrails`.

## Building From Source

Clone the repository and run:

```powershell
.\gradlew.bat clean build
```

The production JAR will be generated at:

```text
build/libs/tnt-trails-1.0.0.jar
```

The project requires a Java 21 development environment.

## Project Structure

```text
src/client/kotlin/davi/lopes
|-- command
|-- config
|-- render
|-- screen
|-- update
`-- util
```

The codebase separates configuration, world rendering, interface components, commands, update handling, and utility functions.

Developed by **Lopes (jvmexploit)**.

The interface uses the bundled **Baflion Sans Black** font.

## License

This project is distributed under the [CC0 1.0 Universal](LICENSE) license.
