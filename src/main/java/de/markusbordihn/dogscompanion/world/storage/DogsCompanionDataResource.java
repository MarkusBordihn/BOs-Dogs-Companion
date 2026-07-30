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

package de.markusbordihn.dogscompanion.world.storage;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.DogsCompanion;
import de.markusbordihn.dogscompanion.data.DogDataEntry;
import de.markusbordihn.dogscompanion.data.DogStatus;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DogsCompanionDataResource implements Resource<EntityStore> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  public static final BuilderCodec<DogsCompanionDataResource> CODEC =
      BuilderCodec.builder(DogsCompanionDataResource.class, DogsCompanionDataResource::new)
          .append(
              new KeyedCodec<>("Dogs", new MapCodec<>(DogDataEntry.CODEC, HashMap::new, false)),
              (resource, map) -> {
                resource.dogs = new HashMap<>();
                map.forEach((k, v) -> resource.dogs.put(UUID.fromString(k), v));
                LOGGER.at(Level.FINE).log("Loaded %d dogs into resource", resource.dogs.size());
              },
              resource -> {
                Map<String, DogDataEntry> stringMap = new HashMap<>();
                resource.dogs.forEach((k, v) -> stringMap.put(k.toString(), v));
                return stringMap;
              })
          .add()
          .build();

  @Nonnull private Map<UUID, DogDataEntry> dogs = new HashMap<>();

  public DogsCompanionDataResource() {}

  @Nonnull
  public static ResourceType<EntityStore, DogsCompanionDataResource> getResourceType() {
    return DogsCompanion.getInstance().dogsDataResourceType;
  }

  public static void setResourceType(
      @Nonnull ResourceType<EntityStore, DogsCompanionDataResource> type) {
    // Method kept for compatibility - actual type stored in DogsCompanion
  }

  public void addDog(@Nonnull DogDataEntry dogDataEntry) {
    dogs.put(dogDataEntry.uuid(), dogDataEntry);
    LOGGER.at(Level.INFO).log("Added dog: %s", dogDataEntry);
  }

  public void updateDog(@Nonnull UUID dogUuid, @Nonnull DogDataEntry dogDataEntry) {
    dogs.put(dogUuid, dogDataEntry);
    LOGGER.at(Level.FINE).log("Updated dog: %s", dogDataEntry);
  }

  public void removeDog(@Nonnull UUID dogUuid) {
    DogDataEntry removed = dogs.remove(dogUuid);
    if (removed != null) {
      LOGGER.at(Level.INFO).log("Removed dog: %s", removed);
    }
  }

  @Nullable
  public DogDataEntry getDog(@Nonnull UUID dogUuid) {
    return dogs.get(dogUuid);
  }

  @Nonnull
  public Set<UUID> getAllDogUuids() {
    return Collections.unmodifiableSet(dogs.keySet());
  }

  @Nonnull
  public Set<DogDataEntry> getAllDogs() {
    return dogs.values().stream().collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<DogDataEntry> getDogsByOwner(@Nonnull UUID ownerUuid) {
    return dogs.values().stream()
        .filter(dog -> ownerUuid.equals(dog.ownerUuid()))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<DogDataEntry> getDogsWithoutOwner() {
    return dogs.values().stream()
        .filter(dog -> !dog.hasOwner())
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<DogDataEntry> getSpawnedDogs() {
    return dogs.values().stream()
        .filter(DogDataEntry::isSpawned)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<DogDataEntry> getDespawnedDogs() {
    return dogs.values().stream()
        .filter(dog -> dog.status() == DogStatus.DESPAWNED)
        .collect(Collectors.toUnmodifiableSet());
  }

  public int getTotalDogCount() {
    return dogs.size();
  }

  public int getOwnedDogCount(@Nonnull UUID ownerUuid) {
    return (int) dogs.values().stream().filter(dog -> ownerUuid.equals(dog.ownerUuid())).count();
  }

  @Nullable
  public UUID findOwnerOfDog(@Nonnull UUID dogUuid) {
    DogDataEntry dogDataEntry = dogs.get(dogUuid);
    return dogDataEntry != null ? dogDataEntry.ownerUuid() : null;
  }

  @Nonnull
  @Override
  public DogsCompanionDataResource clone() {
    DogsCompanionDataResource cloned = new DogsCompanionDataResource();
    cloned.dogs = new HashMap<>(this.dogs);
    return cloned;
  }
}
