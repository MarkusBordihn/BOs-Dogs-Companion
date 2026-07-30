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

package de.markusbordihn.dogscompanion.manager;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogMoodParticles;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogSearchReturn;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogDataEntry;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.data.DogStatus;
import de.markusbordihn.dogscompanion.data.DogType;
import de.markusbordihn.dogscompanion.utils.DogNameplateUtils;
import de.markusbordihn.dogscompanion.world.storage.DogsCompanionDataResource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class DogsManager extends RefSystem<EntityStore> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static DogsManager instance;
  private final ComponentType<EntityStore, DogStateComponent> componentType;
  private final Map<UUID, Ref<EntityStore>> dogRefCache = new ConcurrentHashMap<>();

  public DogsManager(ComponentType<EntityStore, DogStateComponent> componentType) {
    this.componentType = componentType;
    instance = this;
  }

  @Nonnull
  public static DogsManager getInstance() {
    return instance;
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return componentType;
  }

  @Override
  public void onEntityAdded(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull AddReason reason,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    UUID entityUuid = getUuid(ref, store);
    if (entityUuid != null) {
      dogRefCache.put(entityUuid, ref);

      DogNameComponent nameComponent = store.getComponent(ref, DogNameComponent.getComponentType());
      if (nameComponent == null) {
        Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
        if (nameplate != null && nameplate.getText() != null && !nameplate.getText().isEmpty()) {
          String baseName = DogNameplateUtils.getBaseNameFromNameplate(nameplate.getText());
          commandBuffer.addComponent(
              ref, DogNameComponent.getComponentType(), new DogNameComponent(baseName));
          LOGGER.at(Level.INFO).log(
              "Migrated dog name '%s' to DogNameComponent for UUID %s", baseName, entityUuid);
        }
      }

      DogOwnerComponent ownerComponent =
          store.getComponent(ref, DogOwnerComponent.getComponentType());
      if (ownerComponent != null) {
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity != null && ownerComponent.hasOwner()) {
          npcEntity.setSpawnConfiguration(Integer.MIN_VALUE);
          npcEntity.updateSpawnTrackingState(false);
          LOGGER.at(Level.FINE).log(
              "Disabled spawn tracking for tamed dog UUID %s (Owner: %s)",
              entityUuid, ownerComponent.getOwnerName());
        }

        DogsCompanionDataResource resource =
            store.getResource(DogsCompanionDataResource.getResourceType());
        if (resource != null && resource.getDog(entityUuid) == null) {
          registerDog(ref, store);
          LOGGER.at(Level.INFO).log(
              "Auto-registered missing dog entry for UUID %s (Owner: %s)",
              entityUuid, ownerComponent.getOwnerName());
        }
      }

      if (reason == AddReason.LOAD) {
        DogStateComponent stateComponent =
            store.getComponent(ref, DogStateComponent.getComponentType());
        if (stateComponent != null) {
          DogState dogState = stateComponent.getState();
          applyNpcStateFromDogState(ref, dogState, store);
          LOGGER.at(Level.FINE).log(
              "Restored NPC state for dog %s (DogState: %s)", entityUuid, dogState);
        }
      }
    }
  }

  @Override
  public void onEntityRemove(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull RemoveReason reason,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    UUID entityUuid = getUuid(ref, store);
    if (entityUuid != null) {
      this.dogRefCache.remove(entityUuid);
      BuilderActionDogMoodParticles.ActionDogMoodParticles.clearEntity(ref);
      BuilderActionDogSearchReturn.ActionDogSearchReturn.resetSearch(ref);

      DogsCompanionDataResource resource =
          store.getResource(DogsCompanionDataResource.getResourceType());
      if (resource != null) {
        DogDataEntry dogData = resource.getDog(entityUuid);
        if (dogData != null) {
          DogDataEntry updatedData = dogData.withPosition(getPosition(ref, store));
          if (reason == RemoveReason.REMOVE && dogData.status() == DogStatus.SPAWNED) {
            updatedData = updatedData.withStatus(DogStatus.DESPAWNED);
          }
          resource.updateDog(entityUuid, updatedData);
        }
      }
    }
  }

  @Nullable
  public Ref<EntityStore> getDogByUuid(
      @Nonnull UUID entityUuid, @Nonnull Store<EntityStore> store) {
    Ref<EntityStore> cached = dogRefCache.get(entityUuid);
    if (cached != null) {
      if (cached.isValid()) {
        return cached;
      }
      dogRefCache.remove(entityUuid);
    }

    Ref<EntityStore> resolved = store.getExternalData().getRefFromUUID(entityUuid);
    if (resolved != null && resolved.isValid()) {
      dogRefCache.put(entityUuid, resolved);
      return resolved;
    }
    return null;
  }

  @Nonnull
  public Set<Ref<EntityStore>> getDogsByOwner(
      @Nonnull UUID ownerUuid, @Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource == null) {
      return Collections.emptySet();
    }

    Set<Ref<EntityStore>> dogRefs = new HashSet<>();
    for (DogDataEntry dogDataEntry : resource.getDogsByOwner(ownerUuid)) {
      if (dogDataEntry.isSpawned()) {
        Ref<EntityStore> dogRef = getDogByUuid(dogDataEntry.uuid(), store);
        if (dogRef != null && dogRef.isValid()) {
          dogRefs.add(dogRef);
        }
      }
    }
    return dogRefs;
  }

  @Nonnull
  public Set<Ref<EntityStore>> getAllDogs(@Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource == null) {
      return Collections.emptySet();
    }

    Set<Ref<EntityStore>> allDogs = new HashSet<>();
    for (DogDataEntry dogDataEntry : resource.getSpawnedDogs()) {
      Ref<EntityStore> dogRef = getDogByUuid(dogDataEntry.uuid(), store);
      if (dogRef != null && dogRef.isValid()) {
        allDogs.add(dogRef);
      }
    }
    return allDogs;
  }

  public int getDogCount(@Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    return resource != null ? resource.getTotalDogCount() : 0;
  }

  public int getDogCountByOwner(@Nonnull UUID ownerUuid, @Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    return resource != null ? resource.getOwnedDogCount(ownerUuid) : 0;
  }

  public void registerDog(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    UUID dogUuid = getUuid(dogRef, store);
    if (dogUuid == null) {
      LOGGER.at(Level.WARNING).log("Cannot register dog - dog has no UUID");
      return;
    }

    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource == null) {
      LOGGER.at(Level.WARNING).log("DogsCompanionDataResource not available");
      return;
    }

    DogOwnerComponent ownerComponent =
        store.getComponent(dogRef, DogOwnerComponent.getComponentType());
    UUID ownerUuid = ownerComponent != null ? ownerComponent.getOwnerUUID() : null;
    String ownerName = ownerComponent != null ? ownerComponent.getOwnerName() : null;

    DogNameComponent nameComponent =
        store.getComponent(dogRef, DogNameComponent.getComponentType());
    String dogName = nameComponent != null ? nameComponent.getName() : null;

    DogStateComponent stateComponent =
        store.getComponent(dogRef, DogStateComponent.getComponentType());
    DogState dogState = stateComponent != null ? stateComponent.getState() : DogState.FOLLOWING;

    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    DogType dogType = DogType.UNKNOWN;
    if (npcEntity != null && npcEntity.getRole() != null) {
      dogType = DogType.fromRoleName(npcEntity.getRole().getRoleName());
    }

    DogDataEntry existingEntry = resource.getDog(dogUuid);
    if (existingEntry != null) {
      resource.updateDog(
          dogUuid,
          existingEntry
              .withOwner(ownerUuid, ownerName)
              .withName(dogName)
              .withState(dogState)
              .withPosition(getPosition(dogRef, store))
              .withStatus(DogStatus.SPAWNED));
    } else {
      DogDataEntry newEntry =
          new DogDataEntry(
              dogUuid,
              ownerUuid,
              ownerName,
              dogType,
              dogName,
              dogState,
              getPosition(dogRef, store),
              DogStatus.SPAWNED);
      resource.addDog(newEntry);
    }
  }

  public void assignOwner(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull UUID ownerUuid,
      @Nonnull String ownerName,
      @Nullable String dogName,
      @Nonnull Store<EntityStore> store) {

    UUID dogUuid = getUuid(dogRef, store);
    if (dogUuid == null) {
      LOGGER.at(Level.WARNING).log("Cannot assign owner - dog has no UUID");
      return;
    }

    store.putComponent(
        dogRef, DogOwnerComponent.getComponentType(), new DogOwnerComponent(ownerUuid, ownerName));

    store.putComponent(
        dogRef, DogStateComponent.getComponentType(), new DogStateComponent(DogState.FOLLOWING));

    if (dogName != null && !dogName.isEmpty()) {
      store.putComponent(
          dogRef, DogNameComponent.getComponentType(), new DogNameComponent(dogName));
      DogNameplateUtils.updateNameplateWithState(dogRef, dogName, store);
    }

    registerDog(dogRef, store);
  }

  public void updateDogName(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull String dogName,
      @Nonnull Store<EntityStore> store) {
    UUID dogUuid = getUuid(dogRef, store);
    if (dogUuid == null) {
      return;
    }

    DogNameComponent nameComponent =
        store.ensureAndGetComponent(dogRef, DogNameComponent.getComponentType());
    nameComponent.setName(dogName);

    DogNameplateUtils.updateNameplateWithState(dogRef, dogName, store);

    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource != null) {
      DogDataEntry dogDataEntry = resource.getDog(dogUuid);
      if (dogDataEntry != null) {
        resource.updateDog(dogUuid, dogDataEntry.withName(dogName));
      }
    }
  }

  public void updateDogState(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull DogState state,
      @Nonnull Store<EntityStore> store) {
    UUID dogUuid = getUuid(dogRef, store);
    if (dogUuid == null) {
      return;
    }

    DogStateComponent stateComponent =
        store.getComponent(dogRef, DogStateComponent.getComponentType());
    if (stateComponent == null) {
      stateComponent = new DogStateComponent(state);
    } else {
      stateComponent.setState(state);
    }
    store.putComponent(dogRef, DogStateComponent.getComponentType(), stateComponent);

    DogNameComponent nameComponent =
        store.getComponent(dogRef, DogNameComponent.getComponentType());
    if (nameComponent != null) {
      String dogName = nameComponent.getName();
      DogNameplateUtils.updateNameplateWithState(dogRef, dogName, store);
    }

    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource != null) {
      DogDataEntry dogDataEntry = resource.getDog(dogUuid);
      if (dogDataEntry != null) {
        resource.updateDog(dogUuid, dogDataEntry.withState(state));
      }
    }
  }

  public void applyDogDataToEntity(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull DogDataEntry dogData,
      @Nonnull Store<EntityStore> store) {

    if (dogData.ownerUuid() != null) {
      DogOwnerComponent ownerComponent =
          new DogOwnerComponent(dogData.ownerUuid(), dogData.ownerName());
      store.putComponent(dogRef, DogOwnerComponent.getComponentType(), ownerComponent);
    }

    if (dogData.state() != null) {
      DogStateComponent stateComponent = new DogStateComponent(dogData.state());
      store.putComponent(dogRef, DogStateComponent.getComponentType(), stateComponent);
      applyNpcStateFromDogState(dogRef, dogData.state(), store);
    }

    if (dogData.name() != null && !dogData.name().isEmpty()) {
      store.putComponent(
          dogRef, DogNameComponent.getComponentType(), new DogNameComponent(dogData.name()));
      DogNameplateUtils.updateNameplateWithState(dogRef, dogData.name(), store);
    }
  }

  public void applyNpcStateFromDogState(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull DogState dogState,
      @Nonnull Store<EntityStore> store) {

    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      npcEntity
          .getRole()
          .getStateSupport()
          .setState(dogRef, "Pet", dogState.getNpcSubstate(), store);
    }
  }

  @Nullable
  public UUID getUuid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    UUIDComponent component = store.getComponent(ref, UUIDComponent.getComponentType());
    return component != null ? component.getUuid() : null;
  }

  @Nullable
  public DogDataEntry getDogData(@Nonnull UUID dogUuid, @Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    return resource != null ? resource.getDog(dogUuid) : null;
  }

  @Nullable
  public DogDataEntry getDogData(
      @Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    UUID dogUuid = getUuid(dogRef, store);
    return dogUuid != null ? getDogData(dogUuid, store) : null;
  }

  @Nonnull
  public java.util.Collection<DogDataEntry> getDogDataByOwner(
      @Nonnull UUID ownerUuid, @Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource == null) {
      return Collections.emptyList();
    }
    return resource.getDogsByOwner(ownerUuid);
  }

  public void releaseOwnership(
      @Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    UUID dogUuid = getUuid(dogRef, store);
    if (dogUuid == null) {
      LOGGER.at(Level.WARNING).log("Cannot release ownership - dog has no UUID");
      return;
    }

    store.removeComponent(dogRef, DogOwnerComponent.getComponentType());

    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource != null) {
      DogDataEntry dogDataEntry = resource.getDog(dogUuid);
      if (dogDataEntry != null) {
        resource.updateDog(dogUuid, dogDataEntry.withOwner(null, null));
      }
    }
  }

  public void updateDogUuid(
      @Nonnull UUID oldUuid, @Nonnull UUID newUuid, @Nonnull Store<EntityStore> store) {
    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource != null) {
      DogDataEntry dogData = resource.getDog(oldUuid);
      if (dogData != null) {
        resource.removeDog(oldUuid);
        resource.addDog(dogData.withUuid(newUuid).withStatus(DogStatus.SPAWNED));
        Ref<EntityStore> oldRef = dogRefCache.remove(oldUuid);
        if (oldRef != null) {
          dogRefCache.put(newUuid, oldRef);
        }
      }
    }
  }

  public boolean isDogAliveInWorld(@Nonnull UUID dogUuid, @Nonnull Store<EntityStore> store) {
    Ref<EntityStore> dogRef = getDogByUuid(dogUuid, store);
    if (dogRef == null) {
      return false;
    }

    EntityStatMap entityStatMap = store.getComponent(dogRef, EntityStatMap.getComponentType());
    if (entityStatMap == null) {
      return false;
    }

    EntityStatValue healthStat = entityStatMap.get(DefaultEntityStatTypes.getHealth());
    return healthStat != null && healthStat.get() > 0;
  }

  @Nullable
  public Ref<EntityStore> getOwnerRef(
      @Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogOwnerComponent ownerComponent =
        store.getComponent(dogRef, DogOwnerComponent.getComponentType());
    if (ownerComponent == null || ownerComponent.getOwnerUUID() == null) {
      return null;
    }
    return store.getExternalData().getRefFromUUID(ownerComponent.getOwnerUUID());
  }

  public void sendMessageToOwner(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull Message message) {
    Ref<EntityStore> ownerRef = getOwnerRef(dogRef, store);
    if (ownerRef == null || !ownerRef.isValid()) {
      return;
    }

    Player player = store.getComponent(ownerRef, Player.getComponentType());
    if (player != null) {
      PlayerRef playerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
      if (playerRef != null) {
        playerRef.sendMessage(message);
      }
    }
  }

  @Nullable
  private Vector3i getPosition(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform != null) {
      Vector3d pos = transform.getPosition();
      return new Vector3i((int) pos.x, (int) pos.y, (int) pos.z);
    }
    return null;
  }
}
