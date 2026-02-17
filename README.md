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

Bring loyal dog companions into your Hytale world.

This plugin adds tameable dogs that can follow you, defend you, and fight alongside you in combat.
Unlike passive companions such as cats (which are designed for decoration and interaction), dogs are
**active combat companions** that can deal damage, defend their owner, and assist in battles.

Dogs support multiple behavior modes including defensive guarding and offensive assault, making them
powerful allies for exploration, dungeon raids, and base defense.

**Companion comparison:**

* **🐱 Cats** – Peaceful decorative companions without combat abilities
* **🐕 Dogs** – Active combat companions that fight and defend

## ✅ Current Features

### Working Features

* Multiple dog breeds: Generic Dog, German Shepherd, Shiba Inu (more breeds planned)
* Taming system using raw or cooked meat
* Automatic dog naming with unique names for each dog
* Full command-based interaction via `/dog`
* Custom dog names with `/dog name`
* Dog ownership limits configurable via permissions
* Combat capabilities with damage dealing (6.0 damage per attack)
* **Defense Mode:** Dogs automatically defend their owner when attacked (20 block radius)
* **Offense Mode:** Dogs assist their owner in combat when owner attacks (20 block radius)
* **Attack Command:** Direct your dog to attack specific targets
* Multiple behavior states: sitting, following, waiting, wandering, sleeping, playing, attacking
* Matching animations for each behavior
* Visual nameplate indicators showing current mode: [DEF], [OFF], [ATK], [SIT]
* Dog sounds
* Item consumption for taming and feeding (items are consumed from the player's inventory)
* Persistent component data for owner + state (component CODECs)
* Full memory system support for NPC tracking and persistence
* Persistent dog data storage with owner names, positions, and states
  (saved in `worlds/default/resources/DogsCompanionData.json`)

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
2. Hold meat and approach a wild dog
3. Press F (interact) on the dog multiple times - requires 2-5 feedings (random)
4. Your dog gets an automatic unique name (e.g., "Max", "Buddy", "Rex")

**Tips:**
* Enable "Allow NPC Detection" in Creative Mode Quick Settings (TAB) to interact in Creative
* Tamed dogs can be fed meat to keep them happy
* Use empty hand to cycle combat modes (press F)
* Wrong items may upset wild dogs

### Combat System

Dogs have powerful combat capabilities that make them valuable companions:

#### Attack Stats

* **Base Damage:** 6.0 per hit
* **Attack Speed:** 2.5 seconds cooldown between attacks
* **Attack Range:** ~2.5 blocks
* **Strike Delay:** 400ms wind-up before damage is dealt

#### Combat Modes

**Mode Cycling (Interact with Dog)**

* Press F (interact) on your dog with an empty hand to cycle through combat modes
* Cycle order: Following → Defense → Offense → Sitting → Following
* Current mode is shown on the dog's nameplate

**Defense Mode [DEF]**

* Dogs automatically defend their owner when attacked by enemies
* Activation radius: 20 blocks
* Dogs will engage any entity that damages their owner
* Ideal for: Base defense, exploring dangerous areas, dungeon exploration

**Offense Mode [OFF]**

* Dogs assist when their owner initiates combat
* Activation radius: 20 blocks
* Dogs will attack the same target as their owner
* Ideal for: Hunting, raiding, aggressive playstyle

**Attack Command (`/dog attack`)**

* Manually direct your dog to attack a specific target
* Look at the target entity and use `/dog attack`
* Dog will engage until the target is defeated or command is cancelled
* Visual indicator: **[ATK]** on nameplate

**Combat Tips:**

* Dogs show a striking animation before dealing damage
* Heart particles appear when dogs defeat enemies
* Dogs automatically return to following after defeating targets in Defense/Offense mode
* Cycle to Sitting mode to keep dogs out of combat
* Use `/dog sit` or `/dog wait` to keep dogs stationary

### Available Commands

#### General Commands

* `/dog info` – Show detailed dog information (works on any dog)
* `/dog list` – List all dogs owned by a player
* `/dog owner` – Admin command to change ownership
* `/dog spawn` – Spawn a previously despawned dog
* `/dog despawn` – Despawn a dog (saves state, can be respawned later)

#### Tamed Dog Commands

Commands work by looking at your tamed dog or by providing its entity ID:

* `/dog follow` – Make the dog follow you (default behavior)
* `/dog name <name>` – Set a custom name
* `/dog play` – Enable playful behavior
* `/dog release` – Release your dog back to the wild
* `/dog sit` – Make the dog sit and stay
* `/dog sleep` – Put the dog to sleep
* `/dog wait` – Stop following and wait in place
* `/dog wander` – Allow free roaming
* `/dog attack` – **[Combat]** Command dog to attack the target you're looking at

**Combat Mode Switching:**

* **Interact (F key)** with your dog using an empty hand to cycle combat modes:
  Following → Defense → Offense → Sitting
* Visual indicators on nameplate: [DEF], [OFF], [ATK], [SIT]

**Command Aliases:**

* `/dog` or `/dogs` – Both work as the main command

**Tip:** For best results, look directly at your dog when using commands.

## 🔐 Permissions

The plugin supports both Hytale's permission system and LuckPerms.

**Default Dog Limit:** Players can own up to 16 dogs. Admins can adjust this using permissions like `markusbordihn.dogs.limit.8` or `markusbordihn.dogs.limit.unlimited`.

## ⚠️ Known Limitations

### Important Notes

* **No UI menu**
  The interactive menu is temporarily disabled and will return in a later version.

* **Natural spawning**
  Wild dogs do not yet spawn naturally in the world. Use spawn commands or spawn eggs.

* **Limited breed variety**
  Currently only 3 dog breeds are available. More breeds are planned for future updates.

## 🚧 Planned Features

Planned improvements and additions:

* Natural spawning across different biomes
* More dog breeds (Husky, Golden Retriever, Border Collie, etc.)
* Dog breeding and puppies
* Accessories such as collars and armor
* Dog beds and toys
* Advanced combat AI and tactics
* Experience/leveling system for dogs
* Special abilities per breed
* Pack behavior for multiple dogs

## 🗃️ Data Storage

Dog data is automatically saved to `worlds/default/resources/DogsCompanionData.json`.
This includes:

* Dog UUID and owner information
* Dog type, name, and current state
* Last known position and spawn status
* Combat mode and target information

Backup this file to preserve your dogs when moving worlds.

## 🐛 Known Issues

* Wild dogs may occasionally get stuck while approaching players holding meat
* Some animation transitions are not yet smooth
* Pathfinding still needs refinement
* Defense/Offense mode activation timing may delay slightly

## 🔗 Related Plugins

**🐱 Cats Companion**

Looking for peaceful companions? Check out the Cats Companion plugin! While dogs are perfect for combat and protection, cats are designed for peaceful companionship with decorative features, natural spawning, and playful interactions.

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
