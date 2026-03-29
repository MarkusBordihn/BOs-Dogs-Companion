# 🐕 Dogs Companion – Loyal Combat Companions

[![CurseForge](https://cf.way2muchnoise.eu/title/1453431.svg)](https://www.curseforge.com/hytale/mods/dogs-companion)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1453431_downloads.svg)](https://www.curseforge.com/hytale/mods/dogs-companion)
[![🎮 Use Hytale Creator Code Kaworru](https://img.shields.io/badge/%20Use%20Hytale%20Creator%20Code-Kaworru-orange)](https://hytale.com/)

[![Report an Issue](https://img.shields.io/badge/Report%20Issue%20%2F%20Bug%20%2F%20Feature%20Request-grey?logo=github)](https://github.com/MarkusBordihn/BOs-Dogs-Companion/issues)
[![Open Issues](https://img.shields.io/github/issues/MarkusBordihn/BOs-Dogs-Companion?logo=github&color=red)](https://github.com/MarkusBordihn/BOs-Dogs-Companion/issues?q=is%3Aopen)
[![Closed Issues](https://img.shields.io/github/issues-closed/MarkusBordihn/BOs-Dogs-Companion?logo=github)](https://github.com/MarkusBordihn/BOs-Dogs-Companion/issues?q=is%3Aclosed)

⚠️ **BETA VERSION**  This is a beta version, several features are still work in progress.

> **Important update note:**
> Always remove all old versions of this plugin from your mods folder before updating, to avoid
> issues like double saving or server startup errors!

## 📖 Overview

This plugin adds tameable dogs to Hytale that follow you, defend you, and fight alongside you.
Unlike cats (peaceful, decorative), dogs are **active combat companions** with defense and offense
modes.

* **🐱 Cats** – Peaceful companions, no combat
* **🐕 Dogs** – Combat companions that fight and defend

## ✅ Features

* 3 breeds: Generic Dog, German Shepherd, Shiba Inu (more planned)
* Taming with raw or cooked meat, success screen with name input
* **Action Wheel** to control your dog (follow, sit, sleep, play, wander, combat mode, search, pet,
  rename)
* **Dog Whistle** to recall dogs, send them to attack, or cancel attacks
* Mood particles show how your dog feels
* Interaction hints change based on what you hold (food, whistle, empty hand)
* Combat modes: Defense (auto-protect), Offense (assist attacks), Attack Command (manual target)
* Nameplates show the current mode: [DEF], [OFF], [ATK], [SIT]
* Sounds and animations for each behavior state
* All dog data is saved persistently (owner, state, position)
* Dog limit per player, configurable via permissions
* `/dog reload` to reload config on the fly (admin only)

### How to Get a Dog

**Manual Spawning:** Use the NPC spawn command to create dogs:

```unix
/npc spawn DogsCompanion_Wild
/npc spawn DogsCompanion_GermanShepherd_Wild
/npc spawn DogsCompanion_ShibaInu_Wild
```

**Spawn Eggs:** Each breed has a spawn egg:

* `Egg_Spawner_DogsCompanion` – Generic Dog
* `Egg_Spawner_DogsCompanion_GermanShepherd` – German Shepherd
* `Egg_Spawner_DogsCompanion_ShibaInu` – Shiba Inu

> **Note:** Natural spawning is planned for a future update.

### How to Tame a Dog

1. Get meat (Raw Wildmeat or Cooked Wildmeat)
2. Hold meat and walk up to a wild dog
3. Press F (interact) a few times – takes 2-5 feedings
4. A success screen shows the breed and suggests a name
5. Keep the name or type a new one, then confirm

> In Creative Mode, enable "Allow NPC Detection" in Quick Settings (TAB) first.
> Wrong items may upset wild dogs!

### Dog Whistle

Use a Dog Whistle to command your dogs from a distance:

* **Recall** – use without targeting anything, all nearby dogs come back
* **Attack** – use while looking at an enemy, all nearby dogs attack it
* **Cancel** – use on one of your attacking dogs to stop the attack

Has a short cooldown. Only affects dogs within range.

### Action Wheel

Press F on your tamed dog to open the Action Wheel:

| Action              | Effect                           |
|---------------------|----------------------------------|
| **Follow**          | Dog follows you                  |
| **Sit**             | Dog sits and stays               |
| **Sleep / Wake Up** | Toggle sleep                     |
| **Play**            | Playful mood                     |
| **Wander / Return** | Free roaming or come back        |
| **Combat Mode**     | Cycle Normal → Defense → Offense |
| **Search**          | Dog searches the area            |
| **Pet**             | Pet your dog                     |

**Stop** (center) cancels the current action, **Rename** opens a name input screen.

### Combat System

#### Stats

* 6.0 damage per hit, 2.5s cooldown, ~2.5 block range, 400ms wind-up

#### Combat Modes

Switch modes via the Action Wheel or with commands.
The current mode is shown on the nameplate.

**Defense [DEF]** – Dog auto-defends you when something attacks you (20 block radius)

**Offense [OFF]** – Dog helps when you attack something (20 block radius)

**Attack [ATK]** – Look at a target and use `/dog attack` to send your dog after it

Dogs return to following after defeating a target in Defense/Offense mode.
Set them to Sit to keep them out of combat.

### Commands

All commands start with `/dog` (or `/dogs`). Look at your dog when using them.

**General:**
`info` – dog details | `list` – your dogs | `spawn` / `despawn` – spawn or save a dog | `owner` –
change owner (admin) | `reload` – reload config (admin)

**Dog control:**
`follow` | `sit` | `sleep` | `wait` | `wander` | `play` | `name <name>` | `release` | `attack`

## 🔐 Permissions

Supports Hytale permissions and LuckPerms. Default limit is 16 dogs per player.
Adjust with `markusbordihn.dogs.limit.8` or `markusbordihn.dogs.limit.unlimited`.

## ⚠️ Known Limitations

### Important Notes

* **Natural spawning**
  Wild dogs do not yet spawn naturally in the world. Use spawn commands or spawn eggs.

* **Limited breed variety**
  Currently only 3 dog breeds are available. More breeds are planned for future updates.

## 🚧 Planned

* Natural spawning in different biomes
* More breeds (Husky, Golden Retriever, Border Collie, ...)
* Breeding and puppies
* Collars, armor, beds, toys
* Leveling system and breed-specific abilities
* Pack behavior for multiple dogs

## 🗃️ Data Storage

Dog data saves automatically to `worlds/default/resources/DogsCompanionData.json`
(UUIDs, names, states, positions, combat info). Back up this file when moving worlds.

## 🐛 Known Issues

* Wild dogs sometimes get stuck near players with meat
* Some animation transitions are rough
* Pathfinding needs work
* Defense/Offense mode may activate with slight delay

## 🔗 Related Plugins

**🐱 Cats Companion** – Peaceful companions with decorative features and playful interactions, no
combat.

👉 [Download Cats Companion](https://www.curseforge.com/hytale/mods/cats)

## 📜 License

**This project is open source under the MIT License.**

⚠️ **Important:** The license applies **only to the source code** in this repository.

**Assets are excluded from the license:**

* 3D models (`.bbmodel` files)
* Textures and images
* Sounds and music
* Animations
* Other creative/artistic content

**These assets may not be redistributed, modified, or used in other projects without permission.**

For the full license text, see [LICENSE.md](LICENSE.md).

---

Enjoy your new canine companions. 🐕

*This plugin is under active development. Updates and improvements are added regularly.*
