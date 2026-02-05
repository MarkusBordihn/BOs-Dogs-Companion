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
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogDataEntry;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.data.DogStatus;
import de.markusbordihn.dogscompanion.data.DogType;
import de.markusbordihn.dogscompanion.world.storage.DogsCompanionDataResource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DogsManager extends RefSystem<EntityStore> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static DogsManager instance;
  private final ComponentType<EntityStore, DogStateComponent> componentType;
  private final Map<UUID, Ref<EntityStore>> dogRefCache = new HashMap<>();

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
      DogOwnerComponent ownerComponent =
          store.getComponent(ref, DogOwnerComponent.getComponentType());
      if (ownerComponent != null) {
        DogsCompanionDataResource resource =
            store.getResource(DogsCompanionDataResource.getResourceType());
        if (resource != null && resource.getDog(entityUuid) == null) {
          registerDog(ref, store);
          LOGGER.at(Level.INFO).log(
              "Auto-registered missing dog entry for UUID %s (Owner: %s)",
              entityUuid, ownerComponent.getOwnerName());
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
      dogRefCache.remove(entityUuid);
      DogsCompanionDataResource resource =
          store.getResource(DogsCompanionDataResource.getResourceType());
      if (resource != null) {
        DogDataEntry dogData = resource.getDog(entityUuid);
        if (dogData != null && reason == RemoveReason.REMOVE) {
          if (dogData.status() == DogStatus.SPAWNED) {
            resource.updateDog(entityUuid, dogData.withStatus(DogStatus.DESPAWNED));
          }
        }
      }
    }
  }

  @Nullable
  public Ref<EntityStore> getDogByUuid(
      @Nonnull UUID entityUuid, @Nonnull Store<EntityStore> store) {
    Ref<EntityStore> cached = dogRefCache.get(entityUuid);
    if (cached != null && cached.isValid()) {
      return cached;
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

    Nameplate nameplate = store.getComponent(dogRef, Nameplate.getComponentType());
    String dogName = nameplate != null ? nameplate.getText() : null;

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

    if (dogName != null && !dogName.isEmpty()) {
      store.ensureAndGetComponent(dogRef, Nameplate.getComponentType()).setText(dogName);
    }

    store.putComponent(
        dogRef, DogStateComponent.getComponentType(), new DogStateComponent(DogState.FOLLOWING));

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

    store.putComponent(dogRef, DogStateComponent.getComponentType(), new DogStateComponent(state));

    DogsCompanionDataResource resource =
        store.getResource(DogsCompanionDataResource.getResourceType());
    if (resource != null) {
      DogDataEntry dogDataEntry = resource.getDog(dogUuid);
      if (dogDataEntry != null) {
        resource.updateDog(dogUuid, dogDataEntry.withState(state));
      }
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
