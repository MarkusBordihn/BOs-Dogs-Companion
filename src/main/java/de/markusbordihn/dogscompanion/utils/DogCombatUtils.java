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

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
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

  public static boolean hasTarget(Ref<EntityStore> dogRef, Store<EntityStore> store) {
    return getCurrentTarget(dogRef, store) != null;
  }

  public static boolean isAttackingTarget(
      Ref<EntityStore> dogRef, Ref<EntityStore> targetRef, Store<EntityStore> store) {
    if (targetRef == null || !targetRef.isValid()) {
      return false;
    }
    Ref<EntityStore> currentTarget = getCurrentTarget(dogRef, store);
    return currentTarget != null && currentTarget.equals(targetRef);
  }

  public static boolean isTargetAlive(Ref<EntityStore> targetRef, Store<EntityStore> store) {
    if (targetRef == null || !targetRef.isValid()) {
      return false;
    }

    EntityStatMap targetStats = store.getComponent(targetRef, EntityStatMap.getComponentType());
    if (targetStats == null) {
      return true;
    }

    int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
    if (healthIndex < 0) {
      return true;
    }

    EntityStatValue health = targetStats.get(healthIndex);
    if (health == null) {
      return true;
    }

    return health.get() > 0;
  }

  public static void clearTarget(Ref<EntityStore> dogRef, Store<EntityStore> store) {
    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      npcEntity.getRole().getMarkedEntitySupport().setMarkedEntity(LOCKED_TARGET_SLOT, null);
    }
  }
}
