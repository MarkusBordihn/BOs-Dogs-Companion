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

package de.markusbordihn.dogscompanion.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.ui.DogActionHelper;
import de.markusbordihn.dogscompanion.utils.DogCombatUtils;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public class InteractionDogWhistle extends SimpleInteraction {

  public static final String ID = "UseDogWhistle";

  @Nonnull
  public static final BuilderCodec<InteractionDogWhistle> CODEC =
      BuilderCodec.builder(
              InteractionDogWhistle.class, InteractionDogWhistle::new, SimpleInteraction.CODEC)
          .build();

  private static final ConcurrentHashMap<UUID, Long> cooldownMap = new ConcurrentHashMap<>();

  public InteractionDogWhistle() {}

  private static void commandAllDogsToAttack(
      @Nonnull Set<Ref<EntityStore>> dogRefs,
      @Nonnull Ref<EntityStore> targetRef,
      @Nonnull Player player,
      @Nonnull Store<EntityStore> store) {

    String targetName = getEntityDisplayName(targetRef, store);
    int count = 0;
    String lastDogName = "Dog";

    for (Ref<EntityStore> dogRef : dogRefs) {
      if (!dogRef.isValid()) {
        continue;
      }
      NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
      if (npcEntity == null || npcEntity.getRole() == null) {
        continue;
      }
      DogsManager.getInstance().updateDogState(dogRef, DogState.ATTACKING, store);
      npcEntity.getRole().getMarkedEntitySupport().setMarkedEntity("LockedTarget", targetRef);
      npcEntity.getRole().getStateSupport().setState(dogRef, "Pet", "Attacking", store);
      lastDogName = getDogDisplayName(dogRef, store);
      count++;
    }

    PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());
    if (playerRef == null) {
      return;
    }

    if (count == 1) {
      playerRef.sendMessage(
          Message.translation("dogs_companion.interactions.whistle.attack")
              .param("dogName", lastDogName)
              .param("target", targetName)
              .color(Constants.COLOR_INFO));
    } else if (count > 1) {
      playerRef.sendMessage(
          Message.translation("dogs_companion.interactions.whistle.attack.all")
              .param("target", targetName)
              .color(Constants.COLOR_INFO));
    }
  }

  private static void recallAllDogs(
      @Nonnull Set<Ref<EntityStore>> dogRefs,
      @Nonnull Player player,
      @Nonnull Store<EntityStore> store) {

    for (Ref<EntityStore> dogRef : dogRefs) {
      if (!dogRef.isValid()) {
        continue;
      }
      DogCombatUtils.clearTarget(dogRef, store);
      DogActionHelper.follow(dogRef, store);
    }

    PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());
    if (playerRef != null) {
      playerRef.sendMessage(
          Message.translation("dogs_companion.interactions.whistle.recall")
              .color(Constants.COLOR_SUCCESS));
    }
  }

  private static boolean isValidAttackTarget(
      @Nonnull Ref<EntityStore> targetRef,
      @Nonnull UUID playerUUID,
      @Nonnull Store<EntityStore> store) {

    DogOwnerComponent ownerComponent =
        store.getComponent(targetRef, DogOwnerComponent.getComponentType());
    if (ownerComponent != null && playerUUID.equals(ownerComponent.getOwnerUUID())) {
      return false;
    }

    EntityStatMap targetStats = store.getComponent(targetRef, EntityStatMap.getComponentType());
    if (targetStats != null) {
      EntityStatValue healthStat = targetStats.get(DefaultEntityStatTypes.getHealth());
      if (healthStat != null && healthStat.get() <= 0) {
        return false;
      }
    }

    return true;
  }

  @Nonnull
  private static String getEntityDisplayName(
      @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
    Nameplate nameplate = store.getComponent(entityRef, Nameplate.getComponentType());
    if (nameplate != null && nameplate.getText() != null && !nameplate.getText().isEmpty()) {
      return nameplate.getText();
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

  @Nonnull
  private static String getDogDisplayName(
      @Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    Nameplate nameplate = store.getComponent(dogRef, Nameplate.getComponentType());
    if (nameplate != null && nameplate.getText() != null && !nameplate.getText().isEmpty()) {
      return nameplate.getText();
    }
    return "Dog";
  }

  public static boolean handleOnDog(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Role role,
      @Nonnull Store<EntityStore> store,
      @Nonnull Player player) {

    DogStateComponent stateComponent =
        store.getComponent(dogRef, DogStateComponent.getComponentType());

    Nameplate nameplate = store.getComponent(dogRef, Nameplate.getComponentType());
    String dogName =
        (nameplate != null && nameplate.getText() != null) ? nameplate.getText() : "Dog";

    if (stateComponent != null && stateComponent.getState() == DogState.ATTACKING) {
      DogCombatUtils.clearTarget(dogRef, store);
      DogActionHelper.follow(dogRef, store);
      PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());
      if (playerRef != null) {
        playerRef.sendMessage(
            Message.translation("dogs_companion.interactions.whistle.cancel")
                .param("dogName", dogName)
                .color(Constants.COLOR_SUCCESS));
      }
    } else {
      PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());
      if (playerRef != null) {
        playerRef.sendMessage(
            Message.translation("dogs_companion.interactions.whistle.cancel.not_attacking")
                .param("dogName", dogName)
                .color(Constants.COLOR_GRAY));
      }
    }

    return true;
  }

  @Override
  protected void tick0(
      boolean firstRun,
      float time,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nonnull CooldownHandler cooldownHandler) {

    if (!firstRun) {
      return;
    }

    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
    if (commandBuffer == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    Ref<EntityStore> playerRef = context.getEntity();
    Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
    if (player == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    UUIDComponent uuidComponent =
        commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
    if (uuidComponent == null || uuidComponent.getUuid() == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }
    UUID playerUUID = uuidComponent.getUuid();

    long now = System.currentTimeMillis();
    Long lastUsed = cooldownMap.get(playerUUID);
    if (lastUsed != null && (now - lastUsed) < Constants.DOG_WHISTLE_COOLDOWN_MS) {
      PlayerRef playerRefComponent =
          commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
      if (playerRefComponent != null) {
        playerRefComponent.sendMessage(
            Message.translation("dogs_companion.interactions.whistle.cooldown")
                .color(Constants.COLOR_WARNING));
      }
      context.getState().state = InteractionState.Finished;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    TransformComponent playerTransform =
        commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
    Vector3d playerPos = playerTransform != null ? playerTransform.getPosition() : null;

    @Nullable Ref<EntityStore> targetRef = context.getTargetEntity();
    cooldownMap.put(playerUUID, now);

    commandBuffer.run(
        store -> {
          Set<Ref<EntityStore>> allOwnedDogs =
              DogsManager.getInstance().getDogsByOwner(playerUUID, store);

          Set<Ref<EntityStore>> nearbyDogs = ConcurrentHashMap.newKeySet();
          if (playerPos != null) {
            double rangeSquared = Constants.DOG_WHISTLE_DOG_RANGE * Constants.DOG_WHISTLE_DOG_RANGE;
            for (Ref<EntityStore> dogRef : allOwnedDogs) {
              if (!dogRef.isValid()) {
                continue;
              }
              TransformComponent dogTransform =
                  store.getComponent(dogRef, TransformComponent.getComponentType());
              if (dogTransform != null) {
                Vector3d dogPos = dogTransform.getPosition();
                double dx = dogPos.x - playerPos.x;
                double dy = dogPos.y - playerPos.y;
                double dz = dogPos.z - playerPos.z;
                if ((dx * dx + dy * dy + dz * dz) <= rangeSquared) {
                  nearbyDogs.add(dogRef);
                }
              }
            }
          } else {
            nearbyDogs.addAll(allOwnedDogs);
          }

          if (nearbyDogs.isEmpty()) {
            PlayerRef playerRefComponent =
                store.getComponent(player.getReference(), PlayerRef.getComponentType());
            if (playerRefComponent != null) {
              playerRefComponent.sendMessage(
                  Message.translation("dogs_companion.interactions.whistle.no_dogs")
                      .color(Constants.COLOR_WARNING));
            }
            return;
          }

          if (targetRef != null
              && targetRef.isValid()
              && isValidAttackTarget(targetRef, playerUUID, store)) {
            commandAllDogsToAttack(nearbyDogs, targetRef, player, store);
          } else {
            recallAllDogs(nearbyDogs, player, store);
          }
        });

    context.getState().state = InteractionState.Finished;
    super.tick0(firstRun, time, type, context, cooldownHandler);
  }

  @Override
  protected void simulateTick0(
      boolean firstRun,
      float time,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nonnull CooldownHandler cooldownHandler) {}
}
