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

package de.markusbordihn.dogscompanion.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.AnyQuery;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class DogOffenseSystem extends DamageEventSystem {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final double ASSIST_ACTIVATION_RADIUS = 20.0;

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return new AnyQuery<>();
  }

  @Override
  public void handle(
      int eventComponentIndex,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Damage damage) {
    Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(eventComponentIndex);
    onDamageDealt(victimRef, damage, store, commandBuffer);
  }

  private void onDamageDealt(
      @Nonnull Ref<EntityStore> victimRef,
      @Nonnull Damage damage,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    Damage.Source damageSource = damage.getSource();
    if (!(damageSource instanceof Damage.EntitySource entitySource)) {
      return;
    }

    Ref<EntityStore> attackerRef = entitySource.getRef();
    if (attackerRef == null || !attackerRef.isValid()) {
      return;
    }

    UUIDComponent attackerUUID = store.getComponent(attackerRef, UUIDComponent.getComponentType());
    if (attackerUUID == null) {
      return;
    }

    TransformComponent attackerTransform =
        store.getComponent(attackerRef, TransformComponent.getComponentType());
    if (attackerTransform == null) {
      return;
    }
    Vector3d attackerPos = attackerTransform.getPosition();

    DogStateComponent victimDogState =
        store.getComponent(victimRef, DogStateComponent.getComponentType());
    if (victimDogState != null) {
      return;
    }

    Set<Ref<EntityStore>> ownedDogs =
        DogsManager.getInstance().getDogsByOwner(attackerUUID.getUuid(), store);

    for (Ref<EntityStore> dogRef : ownedDogs) {
      if (!dogRef.isValid()) {
        continue;
      }

      DogStateComponent stateComponent =
          store.getComponent(dogRef, DogStateComponent.getComponentType());
      if (stateComponent == null || stateComponent.getState() != DogState.OFFENSE) {
        continue;
      }

      NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        String currentState = npcEntity.getRole().getStateSupport().getStateName();
        if (currentState != null && currentState.contains("Attacking")) {
          continue;
        }
      }

      TransformComponent dogTransform =
          store.getComponent(dogRef, TransformComponent.getComponentType());
      if (dogTransform == null) {
        continue;
      }

      Vector3d dogPos = dogTransform.getPosition();
      double dx = attackerPos.x - dogPos.x;
      double dy = attackerPos.y - dogPos.y;
      double dz = attackerPos.z - dogPos.z;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      if (distance > ASSIST_ACTIVATION_RADIUS) {
        continue;
      }

      activateDogAssist(dogRef, victimRef, store, commandBuffer);
    }
  }

  private void activateDogAssist(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Ref<EntityStore> targetRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    DogStateComponent newStateComponent = new DogStateComponent(DogState.ATTACKING);
    commandBuffer.putComponent(dogRef, DogStateComponent.getComponentType(), newStateComponent);

    DogNameComponent nameComponent =
        store.getComponent(dogRef, DogNameComponent.getComponentType());
    if (nameComponent != null) {
      Nameplate nameplate = store.getComponent(dogRef, Nameplate.getComponentType());
      if (nameplate != null) {
        Nameplate updatedNameplate = (Nameplate) nameplate.clone();
        updatedNameplate.setText("[ATK] " + nameComponent.getName());
        commandBuffer.putComponent(dogRef, Nameplate.getComponentType(), updatedNameplate);
      }
    }

    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      npcEntity.getRole().getMarkedEntitySupport().setMarkedEntity("LockedTarget", targetRef);
      npcEntity.getRole().getStateSupport().setState(dogRef, "Pet", "Attacking", commandBuffer);

      LOGGER.at(Level.FINE).log(
          "Dog %s assisting owner in combat", DogsManager.getInstance().getUuid(dogRef, store));
    }
  }
}
