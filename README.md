# Better Silk Touch

A small Fabric client mod for Minecraft `1.21.x` that prevents breaking selected blocks unless the held tool has Silk Touch.

Author: `flodlol`

## What it does

- Lets the player maintain a protected block list in a Mod Menu settings screen
- Cancels breaking those blocks when the held tool does not have Silk Touch
- Plays a light ping sound on blocked attempts
- Shows a short red warning above the hotbar: `you're not using silk touch`

## Target versions

- Built against Minecraft `1.21.11`
- Declared compatible with `1.21.x`
- Loader: Fabric

## Requirements

- Fabric Loader `0.17.3+`
- Fabric API
- Mod Menu for the in-game settings page

## Development

```bash
./gradlew build
```

Built jars are written to `build/libs/`.

## Configuration

Open the mod settings through Mod Menu.

You can:

- add a block by id, such as `minecraft:glass`
- add the block you are currently looking at
- remove blocks from the protected list

The config file is stored at:

`config/better-silk-touch.json`

## Release notes

This repository is set up to be pushed directly to GitHub:

- Gradle wrapper included
- `.gitignore` included
- source jar enabled
- build verified locally

Before a public release, update the contact links and license if needed.
