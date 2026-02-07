/*
 * Copyright 2026 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.dogscompanion.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.data.DogDataEntry;
import de.markusbordihn.dogscompanion.data.DogType;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import it.unimi.dsi.fastutil.Pair;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;

final class DogSpawnCommand extends DogCommand {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final double MAX_SPAWN_DISTANCE = 32.0;
  @Nonnull private final OptionalArg<String> filterArg;

  public DogSpawnCommand() {
    super("spawn", "Spawns your despawned dogs (optionally filter by name or UUID)");
    this.filterArg =
        this.withOptionalArg(
            "name_or_uuid",
            "Optional: dog name or UUID to spawn (use quotes for names with spaces)",
            ArgTypes.STRING);
  }

  private static boolean matchesFilter(@Nonnull DogDataEntry dog, @Nonnull String filterLower) {
    return dog.uuid().toString().toLowerCase(Locale.ROOT).contains(filterLower)
        || (dog.name() != null && dog.name().toLowerCase(Locale.ROOT).contains(filterLower));
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    if (!context.isPlayer()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.player_only")
              .color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    if (playerUuid == null) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_uuid")
              .color(Constants.COLOR_ERROR));
      return;
    }

    DogsManager dogsManager = DogsManager.getInstance();

    String filter = this.filterArg.get(context);
    if (filter != null && !filter.isEmpty()) {
      String filterLower = filter.toLowerCase(java.util.Locale.ROOT);

      for (DogDataEntry dog : dogsManager.getDogDataByOwner(playerUuid, store)) {
        Ref<EntityStore> dogInWorld = dogsManager.getDogByUuid(dog.uuid(), store);
        if (dogInWorld != null
            && dogsManager.isDogAliveInWorld(dog.uuid(), store)
            && matchesFilter(dog, filterLower)
            && dogInWorld.isValid()) {
          TransformComponent dogTransform =
              store.getComponent(dogInWorld, TransformComponent.getComponentType());
          if (dogTransform != null) {
            Ref<EntityStore> playerRef = store.getExternalData().getRefFromUUID(playerUuid);
            if (playerRef != null && playerRef.isValid()) {
              TransformComponent playerTransform =
                  store.getComponent(playerRef, TransformComponent.getComponentType());
              if (playerTransform != null) {
                Vector3d playerPos = playerTransform.getPosition();
                dogTransform.setPosition(playerPos);

                String dogName = dog.name() != null ? dog.name() : "Dog";
                context.sendMessage(
                    Message.raw("The dog " + dogName + " has been teleported to you!")
                        .color(Constants.COLOR_SUCCESS));
                return;
              }
            }
          }
        }
      }
    }

    Collection<DogDataEntry> despawnedDogs =
        dogsManager.getDogDataByOwner(playerUuid, store).stream()
            .filter(
                dog -> {
                  Ref<EntityStore> dogRef = dogsManager.getDogByUuid(dog.uuid(), store);
                  return dogRef == null || !dogsManager.isDogAliveInWorld(dog.uuid(), store);
                })
            .toList();

    if (filter != null && !filter.isEmpty()) {
      despawnedDogs =
          despawnedDogs.stream()
              .filter(dog -> matchesFilter(dog, filter.toLowerCase(java.util.Locale.ROOT)))
              .toList();

      if (despawnedDogs.isEmpty()) {
        context.sendMessage(
            Message.translation("dogs_companion.commands.spawn.no_match")
                .param("filter", filter)
                .color(Constants.COLOR_WARNING));
        return;
      }
    }

    if (despawnedDogs.isEmpty()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.spawn.no_despawned")
              .color(Constants.COLOR_INFO));
      return;
    }

    Ref<EntityStore> playerRef = store.getExternalData().getRefFromUUID(playerUuid);
    if (playerRef == null || !playerRef.isValid()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.player_pos_not_found")
              .color(Constants.COLOR_ERROR));
      return;
    }

    TransformComponent playerTransform =
        store.getComponent(playerRef, TransformComponent.getComponentType());
    if (playerTransform == null) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.player_pos_not_found")
              .color(Constants.COLOR_ERROR));
      return;
    }

    int spawnedCount = 0;
    for (DogDataEntry dogData : despawnedDogs) {
      Vector3d spawnPos = calculateSpawnPosition(dogData, playerTransform.getPosition());
      if (spawnDog(dogData, spawnPos, world, store)) {
        spawnedCount++;
      }
    }

    if (spawnedCount > 0) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.spawn.success")
              .param("count", spawnedCount)
              .color(Constants.COLOR_SUCCESS));
    } else {
      context.sendMessage(
          Message.translation("dogs_companion.commands.spawn.failed").color(Constants.COLOR_ERROR));
    }
  }

  @Nonnull
  private Vector3d calculateSpawnPosition(
      @Nonnull DogDataEntry dogData, @Nonnull Vector3d playerPos) {
    if (dogData.position() != null) {
      Vector3d savedPos =
          new Vector3d(dogData.position().x, dogData.position().y, dogData.position().z);
      double dx = playerPos.x - savedPos.x;
      double dy = playerPos.y - savedPos.y;
      double dz = playerPos.z - savedPos.z;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      if (distance <= MAX_SPAWN_DISTANCE) {
        return savedPos;
      }
    }

    return playerPos.add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2);
  }

  private boolean spawnDog(
      @Nonnull DogDataEntry dogData,
      @Nonnull Vector3d position,
      @Nonnull World world,
      @Nonnull Store<EntityStore> store) {

    DogsManager dogsManager = DogsManager.getInstance();
    Ref<EntityStore> existingDog = dogsManager.getDogByUuid(dogData.uuid(), store);
    if (existingDog != null && existingDog.isValid()) {
      LOGGER.at(Level.WARNING).log(
          "Dog %s (UUID: %s) is already spawned, skipping", dogData.name(), dogData.uuid());
      return false;
    }

    if (dogData.dogType() == DogType.UNKNOWN) {
      return false;
    }

    try {
      NPCPlugin npcPlugin = NPCPlugin.get();
      if (npcPlugin == null) {
        LOGGER.at(Level.WARNING).log("Cannot spawn dog: NPCPlugin is not available");
        return false;
      }

      String roleName = dogData.dogType().getRoleName(dogData.hasOwner());
      int roleIndex = npcPlugin.getIndex(roleName);
      if (roleIndex < 0) {
        LOGGER.at(Level.WARNING).log("Cannot spawn dog: Role '" + roleName + "' not found");
        return false;
      }

      Vector3f rotation = new Vector3f();
      Pair<Ref<EntityStore>, NPCEntity> spawnResult =
          npcPlugin.spawnEntity(store, roleIndex, position, rotation, null, null, null);

      if (spawnResult == null || spawnResult.left() == null || !spawnResult.left().isValid()) {
        LOGGER.at(Level.WARNING).log(
            "Failed to spawn dog: NPCPlugin.spawnEntity returned null or invalid reference");
        return false;
      }

      Ref<EntityStore> dogRef = spawnResult.left();

      // Update UUID if needed
      UUID newEntityUuid = dogsManager.getUuid(dogRef, store);
      if (newEntityUuid != null && !newEntityUuid.equals(dogData.uuid())) {
        dogsManager.updateDogUuid(dogData.uuid(), newEntityUuid, store);
      }

      // Apply all dog data (owner, name, state) using DogsManager
      dogsManager.applyDogDataToEntity(dogRef, dogData, store);

      return true;
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).withCause(e).log("Failed to spawn dog: " + dogData.name());
      return false;
    }
  }
}
