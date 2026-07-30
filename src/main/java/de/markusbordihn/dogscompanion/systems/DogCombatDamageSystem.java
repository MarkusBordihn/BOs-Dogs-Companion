/*
 * Copyright 2025 Markus Bordihn
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
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.damage.DogDamageSource;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.utils.DogCombatUtils;
import de.markusbordihn.dogscompanion.utils.DogEntityNameUtils;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class DogCombatDamageSystem extends EntityTickingSystem<EntityStore> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final Query<EntityStore> QUERY = DogStateComponent.getComponentType();

  private static final float DOG_ATTACK_DAMAGE = 6.0f;
  private static final double ATTACK_RANGE_SQUARED = 6.25;
  private static final long ATTACK_COOLDOWN_MS = 2500;
  private static final long STRIKE_DELAY_MS = 400;
  private static final String BITE_DAMAGE_CAUSE_ID = "Slashing";

  private static volatile DamageCause biteDamageCause;

  private final ConcurrentLinkedQueue<PendingStrike> pendingStrikes = new ConcurrentLinkedQueue<>();

  @Nonnull
  private static DamageCause resolveBiteDamageCause() {
    DamageCause damageCause = biteDamageCause;
    if (damageCause == null) {
      damageCause = DamageCause.getAssetMap().getAsset(BITE_DAMAGE_CAUSE_ID);
      if (damageCause == null) {
        throw new IllegalStateException(
            "Damage cause " + BITE_DAMAGE_CAUSE_ID + " is not registered");
      }
      biteDamageCause = damageCause;
    }

    return damageCause;
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return QUERY;
  }

  @Override
  public void tick(
      float deltaTime,
      @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    this.applyPendingStrikes(store, commandBuffer);
    super.tick(deltaTime, chunk, store, commandBuffer);
  }

  private void applyPendingStrikes(
      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    long currentTimeMs = System.currentTimeMillis();

    PendingStrike strike;
    while ((strike = this.pendingStrikes.peek()) != null && currentTimeMs >= strike.applyAtMs) {
      this.pendingStrikes.poll();

      if (!strike.dogRef.isValid() || !DogCombatUtils.isTargetAlive(strike.targetRef, store)) {
        continue;
      }

      // NPC state Striking for visual only; DogStateComponent stays ATTACKING
      NPCEntity npcEntity = store.getComponent(strike.dogRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        npcEntity.getRole().getStateSupport().setState(strike.dogRef, "Pet", "Striking", store);
      }

      DamageSystems.executeDamage(strike.targetRef, commandBuffer, strike.damage);

      if (!DogCombatUtils.isTargetAlive(strike.targetRef, store)) {
        DogsManager.getInstance()
            .sendMessageToOwner(
                strike.dogRef,
                store,
                Message.translation("dogs_companion.combat.defeated")
                    .param(
                        "dog",
                        DogEntityNameUtils.getDogName(
                            strike.dogRef, store, DogEntityNameUtils.FALLBACK_DOG))
                    .param("target", strike.targetName)
                    .color(Constants.COLOR_SUCCESS));
      }
    }
  }

  @Override
  public void tick(
      float deltaTime,
      int index,
      @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    long currentTimeMs = System.currentTimeMillis();

    DogStateComponent dogStateComponent =
        chunk.getComponent(index, DogStateComponent.getComponentType());
    if (dogStateComponent == null || dogStateComponent.getState() != DogState.ATTACKING) {
      return;
    }

    Ref<EntityStore> dogRef = chunk.getReferenceTo(index);
    Ref<EntityStore> targetRef = DogCombatUtils.getCurrentTarget(dogRef, store);

    if (targetRef == null || !DogCombatUtils.isTargetAlive(targetRef, store)) {
      return;
    }

    TransformComponent dogTransform =
        chunk.getComponent(index, TransformComponent.getComponentType());
    TransformComponent targetTransform =
        store.getComponent(targetRef, TransformComponent.getComponentType());

    if (dogTransform == null || targetTransform == null) {
      return;
    }

    Vector3d dogPos = dogTransform.getPosition();
    Vector3d targetPos = targetTransform.getPosition();
    double dx = dogPos.x - targetPos.x;
    double dy = dogPos.y - targetPos.y;
    double dz = dogPos.z - targetPos.z;
    double distanceSquared = dx * dx + dy * dy + dz * dz;

    if (distanceSquared > ATTACK_RANGE_SQUARED) {
      return;
    }

    Long lastAttackTime = dogStateComponent.getLastAttackTime();
    if (lastAttackTime != null && (currentTimeMs - lastAttackTime) < ATTACK_COOLDOWN_MS) {
      return;
    }

    Damage damage =
        new Damage(new DogDamageSource(dogRef), resolveBiteDamageCause(), DOG_ATTACK_DAMAGE);

    // Cache target name before queueing strike (in case entity dies before execution)
    String targetName = DogEntityNameUtils.getEntityName(targetRef, store);
    pendingStrikes.offer(
        new PendingStrike(dogRef, targetRef, targetName, damage, currentTimeMs + STRIKE_DELAY_MS));
    dogStateComponent.setLastAttackTime(currentTimeMs);

    LOGGER.at(Level.FINE).log(
        "Dog attack queued (damage: %.1f, distance: %.2fm)",
        DOG_ATTACK_DAMAGE, Math.sqrt(distanceSquared));
  }

  private static class PendingStrike {
    final Ref<EntityStore> dogRef;
    final Ref<EntityStore> targetRef;
    final String targetName;
    final Damage damage;
    final long applyAtMs;

    PendingStrike(
        Ref<EntityStore> dogRef,
        Ref<EntityStore> targetRef,
        String targetName,
        Damage damage,
        long applyAtMs) {
      this.dogRef = dogRef;
      this.targetRef = targetRef;
      this.targetName = targetName;
      this.damage = damage;
      this.applyAtMs = applyAtMs;
    }
  }
}
