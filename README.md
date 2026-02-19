# BlockCraft 2D — LibGDX (Desktop)

A colorful  see-and-play block world game built with **LibGDX** + **LWJGL3**.
Designed for teaching Java concepts *and* for extension into a bigger Minecraft-like project.

## What you get (today)
- Smooth real-time movement (WASD)
- Camera follows the player
- Mouse mining (Left Click) and placing (Right Click)
- Hotbar: keys **1-6** or Mouse Wheel to select block
- Simple procedural world (grass/dirt/stone, water pools, trees)
- A wandering mob
- Save/Load (press **F5** / **F9**) to a local file

## Requirements
- Java **17+**
- Internet on first run (Gradle + dependencies download)

## Run (Windows)
```bat
gradlew.bat run
```

## Run (macOS/Linux)
```bash
chmod +x gradlew
./gradlew run
```

If your environment blocks scripts, you can run:
```bash
java -jar gradle/wrapper/gradle-wrapper.jar run
```

## Controls
- Move: **W A S D**
- Mine: **Left Mouse**
- Place: **Right Mouse**
- Select block: **1..6** or **Mouse Wheel**
- Save: **F5**
- Load: **F9**
- Toggle help overlay: **H**
- Quit: **ESC**

## Project layout
- `src/main/java/blockcraft/` : game code (single module)
- `gradle/wrapper/` : a tiny self-contained Gradle wrapper jar (no Gradle install required)

## Extend next
- Add crafting UI (Scene2D)
- Add chunks (32x32) + infinite world streaming
- Add tools (pickaxe speed), health, hunger
- Add lighting/shadows
- Add multiplayer (later)
