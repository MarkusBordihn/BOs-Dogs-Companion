# Changelog for Dogs Companion (Hytale)

## Note

This change log includes the summarized changes.
For the full changelog, please go to the [GitHub History][history] instead.

Note: Please always back up your world before updating to a new version!

### 1.6.0

- Fixed status effect bug where dogs would get stuck in a status effect state.
- Fixed missing state updates and conditions in state machine.
- Added search functionality, for searching for items and return them to the owner.
- Added loyalty defense system, where dogs will automatically defend their owner when attacked.
- Migrated to Hytale 0.5.2 with updated APIs and systems.

### 1.5.0

- Added basic dog whistle item to command dogs (attack target, recall, cancel attack).
- Added Action Wheel UI for dog interactions (follow, sit, sleep, play, wander, combat modes,
  rename, etc.).
- Added dog naming UI on taming success and rename via action wheel.
- Added mood particles for dogs based on their current state.
- Added NPC sensors for held items (food, whistle, empty hand) with context-sensitive interaction
  hints.
- Improved interaction system: owner actions now route through item-specific handlers.
- Simplified Gradle build setup, removed legacy config and task files.
- Improved existing dog textures.

### 1.4.0

- Migrated to Hytale 2026.03.26-89796e57b with updated APIs and systems.
  (Navigation and Pathfinding is currently broken !!!)

### 1.3.0

- Fixed animation bug with default mode.
- Updated server version, again, to fix mod warning.

### 1.2.1

- Fixed mod warning by using fixed ServerVersion.

### 1.2.0

- Added healing system: Dogs now heal when fed (cooked food heals 10 HP, raw food heals 5 HP).
- Increased dog max health from 20 to 60 HP.
- Added hurt animation when dogs take damage.
- Improved dog model hitbox and eye height for better interaction.
- Added camera pitch/yaw targeting for head movement.
- Enhanced animation system with proper speeds and blending.
- Added death animation, knockback resistance, and separation behavior.
- Improved pathfinding with climb height and jump mechanics.

### 1.1.0

- Fixed German Shepherd dog textures.
- Added Shiba Inu dog breed.
- Added memories category for dogs.
- Added additional dog actions and interactions.

### 1.0.0 ✨

- Initial release of Dogs Companion - Loyal Combat Companions for Hytale!

[history]: https://github.com/MarkusBordihn/BOs-Dogs-Companion/commits/
