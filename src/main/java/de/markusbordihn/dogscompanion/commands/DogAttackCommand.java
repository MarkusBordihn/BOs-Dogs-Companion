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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import javax.annotation.Nonnull;

final class DogAttackCommand extends DogCommand {

  @Nonnull private final EntityWrappedArg dogArg;
  @Nonnull private final EntityWrappedArg targetArg;

  public DogAttackCommand() {
    super("attack", "Commands your dog to attack a target");
    this.dogArg = this.withOptionalArg("dog", "The dog entity", ArgTypes.ENTITY_ID);
    this.targetArg = this.withOptionalArg("target", "The entity to attack", ArgTypes.ENTITY_ID);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    var targetRefOpt = getEntityFromArgument(this.targetArg, store, context);
    if (targetRefOpt.isEmpty()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.attack.no_target")
              .color(Constants.COLOR_ERROR));
      return;
    }

    Ref<EntityStore> targetRef = targetRefOpt.get();
    var dogRefOpt = getEntityFromArgument(this.dogArg, store, context);

    if (dogRefOpt.isEmpty()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_dog").color(Constants.COLOR_ERROR));
      return;
    }

    Ref<EntityStore> dogRef = dogRefOpt.get();
    if (!checkOwnership(dogRef, store, context)) {
      return;
    }

    DogOwnerComponent ownerComponent =
        store.getComponent(dogRef, DogOwnerComponent.getComponentType());
    if (ownerComponent != null && ownerComponent.getOwnerUUID() != null) {
      UUIDComponent targetUUID = store.getComponent(targetRef, UUIDComponent.getComponentType());
      if (targetUUID != null && ownerComponent.getOwnerUUID().equals(targetUUID.getUuid())) {
        context.sendMessage(
            Message.translation("dogs_companion.commands.attack.cannot_attack_owner")
                .color(Constants.COLOR_ERROR));
        return;
      }
    }

    DogStateComponent targetDogState =
        store.getComponent(targetRef, DogStateComponent.getComponentType());
    if (targetDogState != null) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.attack.cannot_attack_dog")
              .color(Constants.COLOR_ERROR));
      return;
    }

    EntityStatMap targetStats = store.getComponent(targetRef, EntityStatMap.getComponentType());
    if (targetStats != null) {
      EntityStatValue healthStat = targetStats.get(DefaultEntityStatTypes.getHealth());
      if (healthStat != null && healthStat.get() <= 0) {
        context.sendMessage(
            Message.translation("dogs_companion.commands.attack.target_dead")
                .color(Constants.COLOR_ERROR));
        return;
      }
    }

    DogsManager.getInstance().updateDogState(dogRef, DogState.ATTACKING, store);

    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      npcEntity.getRole().getMarkedEntitySupport().setMarkedEntity("LockedTarget", targetRef);
      npcEntity.getRole().getStateSupport().setState(dogRef, "Pet", "Attacking", store);

      DogsManager.getInstance()
          .sendMessageToOwner(
              dogRef,
              store,
              Message.translation("dogs_companion.combat.attacking")
                  .param("dog", getDogDisplayName(dogRef, store))
                  .param("target", getTargetName(targetRef, store))
                  .color(Constants.COLOR_INFO));

      context.sendMessage(
          Message.translation("dogs_companion.commands.attack.success")
              .param("name", getDogDisplayName(dogRef, store))
              .color(Constants.COLOR_SUCCESS));
    } else {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_dog").color(Constants.COLOR_INFO));
    }
  }

  @Nonnull
  private String getTargetName(
      @Nonnull Ref<EntityStore> targetRef, @Nonnull Store<EntityStore> store) {

    if (!targetRef.isValid()) {
      return "target";
    }

    DogNameComponent dogNameComponent =
        store.getComponent(targetRef, DogNameComponent.getComponentType());
    if (dogNameComponent != null
        && dogNameComponent.getName() != null
        && !dogNameComponent.getName().isEmpty()) {
      return dogNameComponent.getName();
    }

    DisplayNameComponent displayNameComponent =
        store.getComponent(targetRef, DisplayNameComponent.getComponentType());
    if (displayNameComponent != null && displayNameComponent.getDisplayName() != null) {
      String displayName = displayNameComponent.getDisplayName().getRawText();
      if (displayName != null && !displayName.isEmpty()) {
        return displayName;
      }
    }

    NPCEntity npcEntity = store.getComponent(targetRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      String roleName = npcEntity.getRole().getRoleName();
      if (roleName != null && !roleName.isEmpty()) {
        return roleName;
      }
    }

    return "target";
  }
}
