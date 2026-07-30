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

package de.markusbordihn.dogscompanion.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.data.DogStateData;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DogCombatUtils {

  private static final String LOCKED_TARGET_SLOT = "LockedTarget";

  private DogCombatUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  @Nullable
  public static Ref<EntityStore> getCurrentTarget(
      Ref<EntityStore> dogRef, Store<EntityStore> store) {
    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity == null || npcEntity.getRole() == null) {
      return null;
    }

    Ref<EntityStore> targetRef =
        npcEntity.getRole().getMarkedEntitySupport().getMarkedEntityRef(LOCKED_TARGET_SLOT);
    return (targetRef != null && targetRef.isValid()) ? targetRef : null;
  }

  public static boolean isTargetAlive(Ref<EntityStore> targetRef, Store<EntityStore> store) {
    if (targetRef == null || !targetRef.isValid()) {
      return false;
    }

    EntityStatMap targetStats = store.getComponent(targetRef, EntityStatMap.getComponentType());
    if (targetStats == null) {
      return true;
    }

    EntityStatValue health = targetStats.get(DefaultEntityStatTypes.getHealth());
    return health == null || health.get() > 0;
  }

  public static void engageTarget(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Ref<EntityStore> targetRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity == null || npcEntity.getRole() == null) {
      return;
    }

    String currentNpcState = npcEntity.getRole().getStateSupport().getStateName();
    if (currentNpcState != null && currentNpcState.contains(DogState.ATTACKING.getNpcSubstate())) {
      return;
    }

    DogStateComponent stateComponent =
        store.getComponent(dogRef, DogStateComponent.getComponentType());
    DogStateData engagedData =
        stateComponent != null
            ? stateComponent.getData().withState(DogState.ATTACKING)
            : DogStateData.of(DogState.ATTACKING);

    // don't modify store during event processing
    DogStateComponent engagedComponent = new DogStateComponent(engagedData);
    commandBuffer.putComponent(dogRef, DogStateComponent.getComponentType(), engagedComponent);

    DogNameComponent nameComponent =
        store.getComponent(dogRef, DogNameComponent.getComponentType());
    Nameplate nameplate = store.getComponent(dogRef, Nameplate.getComponentType());
    if (nameComponent != null && nameplate != null) {
      Nameplate updatedNameplate = (Nameplate) nameplate.clone();
      updatedNameplate.setText(
          DogNameplateUtils.getStateSymbol(DogState.ATTACKING) + " " + nameComponent.getName());
      commandBuffer.putComponent(dogRef, Nameplate.getComponentType(), updatedNameplate);
    }

    npcEntity.getRole().getMarkedEntitySupport().setMarkedEntity(LOCKED_TARGET_SLOT, targetRef);
    npcEntity
        .getRole()
        .getStateSupport()
        .setState(dogRef, "Pet", DogState.ATTACKING.getNpcSubstate(), commandBuffer);
  }

  public static void clearTarget(Ref<EntityStore> dogRef, Store<EntityStore> store) {
    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      npcEntity.getRole().getMarkedEntitySupport().setMarkedEntity(LOCKED_TARGET_SLOT, null);
    }
  }
}
