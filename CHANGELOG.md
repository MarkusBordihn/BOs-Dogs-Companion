# Changelog for Dogs Companion (Hytale)

## Note

This change log includes the summarized changes. For the full changelog, please go to
the [GitHub History][history] instead.

Note: Please always back up your world before updating to a new version!

### 2.1.0

- Fixed dogs not attacking when more than one dog was active, because the attack rate limit was
  applied per dog instead of per tick.
- Fixed a queued bite with a dead target cancelling the attacking dog's whole turn.
- Fixed most food items being rejected when feeding or taming, because nine of the fourteen
  configured item ids do not exist in Hytale anymore.
- Fixed dogs in Defense or Offense mode falling back to Follow after every fight instead of
  returning to their combat mode.
- Fixed the attack cooldown resetting, which let dogs bite again immediately.
- Fixed dogs losing their Wander or Play behavior after a server restart.
- Fixed the Offense wheel action putting the dog into a state that did not survive a relog.
- Fixed `/dog spawn` duplicating dogs whose chunk was merely unloaded.
- Fixed "Last Position" in `/dog list` and `/dog info` never updating after taming.
- Fixed the per-player dog limit having no effect, so `markusbordihn.dogs.limit.*` was ignored when
  taming.
- Fixed `markusbordihn.dogs.admin.bypass` not bypassing ownership checks.
- Fixed the `/dog reload` permission node being unusable due to a missing prefix.
- Fixed all dog names falling back to "Dog" until restart when the name config failed to load.
- Fixed a memory leak where search and mood timers were never released for removed dogs.
- Fixed stack traces being dropped from several error messages.
- Changed the dog bite to use the `Slashing` damage cause instead of the deprecated `Physical`
  constant.
- Changed the Dog Whistle cooldown to start only when the whistle actually reached a dog.
- Changed `/dog reload` to require operator permissions.
- Changed mood particles to be sent directly to nearby players, so they no longer depend on behavior
  states.
- Added mood particles to tamed dogs — the feature was advertised but never activated.
- Added German and English translations for `/dog list` and `/dog info`, which were English-only.
- Improved damage handling so dog lookups only run for players, instead of on every damage event in
  the world.
- Improved logging by moving per-tick messages from INFO to FINE.
- Migrated to Hytale 0.5.7.

### 2.0.0

- Migrated to Hytale 0.5.6

### 1.8.0

- Added Australian Shepherd dog breed.
- Fixed multi-player issues with owner are not recognized.

### 1.7.0

- Fixed tamed dogs following any nearby player instead of only their owner.
- Fixed stranger interaction firing every tick, which flooded the logs and repeatedly forced the dog
  into its rejection state (freezing it and overriding owner commands like sit/stay).
- Fixed tamed dogs being managed by the natural spawn system, which could despawn or duplicate them.
- Fixed stale entity reference cache entries (invalid refs and UUID changes) in the dog manager.
- Migrated to Hytale 0.5.4 with updated APIs and systems.
- Added basic default natural spawn definitions.

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

- Migrated to Hytale 2026.03.26-89796e57b with updated APIs and systems. (Navigation and Pathfinding
  is currently broken !!!)

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
