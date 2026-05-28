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
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.damage.DogDamageSource;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.utils.DogCombatUtils;
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
  private static final int TICK_INTERVAL = 5;

  private final ConcurrentLinkedQueue<PendingStrike> pendingStrikes = new ConcurrentLinkedQueue<>();
  private int tickCounter = 0;

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return QUERY;
  }

  @Override
  public void tick(
      float deltaTime,
      int index,
      @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    long currentTimeMs = System.currentTimeMillis();

    PendingStrike strike = pendingStrikes.peek();
    if (strike != null && currentTimeMs >= strike.applyAtMs) {
      pendingStrikes.poll();

      if (!strike.dogRef.isValid() || !DogCombatUtils.isTargetAlive(strike.targetRef, store)) {
        return;
      }

      // NPC state Striking for visual only; DogStateComponent stays ATTACKING
      NPCEntity npcEntity = store.getComponent(strike.dogRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        npcEntity.getRole().getStateSupport().setState(strike.dogRef, "Pet", "Striking", store);
      }

      DamageSystems.executeDamage(strike.targetRef, commandBuffer, strike.damage);

      if (!DogCombatUtils.isTargetAlive(strike.targetRef, store)) {
        sendOwnerMessage(
            strike.dogRef,
            store,
            Message.translation("dogs_companion.combat.defeated")
                .param("dog", getDogName(strike.dogRef, store))
                .param("target", strike.targetName)
                .color(Constants.COLOR_SUCCESS));
      }
    }

    if (++tickCounter % TICK_INTERVAL != 0) {
      return;
    }

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
        new Damage(new DogDamageSource(dogRef), DamageCause.PHYSICAL, DOG_ATTACK_DAMAGE);

    // Cache target name before queueing strike (in case entity dies before execution)
    String targetName = getEntityName(targetRef, store);
    pendingStrikes.offer(
        new PendingStrike(dogRef, targetRef, targetName, damage, currentTimeMs + STRIKE_DELAY_MS));
    dogStateComponent.setLastAttackTime(currentTimeMs);

    LOGGER.at(Level.FINE).log(
        "Dog attack queued (damage: %.1f, distance: %.2fm)",
        DOG_ATTACK_DAMAGE, Math.sqrt(distanceSquared));
  }

  private void sendOwnerMessage(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull Message message) {
    DogsManager.getInstance().sendMessageToOwner(dogRef, store, message);
  }

  @Nonnull
  private String getDogName(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogNameComponent nameComponent =
        store.getComponent(dogRef, DogNameComponent.getComponentType());
    return nameComponent != null && nameComponent.getName() != null
        ? nameComponent.getName()
        : "Your dog";
  }

  @Nonnull
  private String getEntityName(
      @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
    if (entityRef == null || !entityRef.isValid()) {
      return "target";
    }

    DogNameComponent dogNameComponent =
        store.getComponent(entityRef, DogNameComponent.getComponentType());
    if (dogNameComponent != null
        && dogNameComponent.getName() != null
        && !dogNameComponent.getName().isEmpty()) {
      return dogNameComponent.getName();
    }

    DisplayNameComponent displayNameComponent =
        store.getComponent(entityRef, DisplayNameComponent.getComponentType());
    if (displayNameComponent != null && displayNameComponent.getDisplayName() != null) {
      String displayName = displayNameComponent.getDisplayName().getRawText();
      if (displayName != null && !displayName.isEmpty()) {
        return displayName;
      }
    }

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      String roleName = npcEntity.getRole().getRoleName();
      if (roleName != null && !roleName.isEmpty()) {
        return roleName;
      }
    }

    return "target";
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
