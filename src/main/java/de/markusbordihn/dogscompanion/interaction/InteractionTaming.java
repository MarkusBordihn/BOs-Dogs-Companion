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

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogTamingProgressComponent;
import de.markusbordihn.dogscompanion.data.DogType;
import de.markusbordihn.dogscompanion.inventory.InventoryHelper;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.manager.DogsNamesManager;
import de.markusbordihn.dogscompanion.ui.DogTamingSuccessPage;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class InteractionTaming {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final Random RANDOM = new Random();
  private static final int MIN_REQUIRED_FEEDINGS = 2;
  private static final int MAX_REQUIRED_FEEDINGS = 5;
  private static final long FEEDING_COOLDOWN_MS = 4000;

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {
    String itemName = heldItem != null ? heldItem.getItemId() : null;

    if (!isTamingItem(itemName)) {
      role.getStateSupport().setState(entityRef, "Wild", "Rejection", store);
      return false;
    }

    if (player == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame dog - player is null");
      return false;
    }

    UUID playerUUID = player.getUuid();
    String username = player.getPlayerRef().getUsername();
    if (playerUUID == null || username == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame dog - UUID or username not found");
      return false;
    }

    DogsManager dogsManager = DogsManager.getInstance();
    if (dogsManager == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame dog - DogsManager is null");
      return false;
    }

    int currentDogCount = dogsManager.getDogCountByOwner(playerUUID, store);
    if (currentDogCount >= Constants.DEFAULT_DOG_LIMIT) {
      player
          .getPlayerRef()
          .sendMessage(
              Message.translation("dogs_companion.interactions.taming.limit_reached")
                  .param("current", String.valueOf(currentDogCount))
                  .param("limit", String.valueOf(Constants.DEFAULT_DOG_LIMIT))
                  .color(Constants.COLOR_ERROR));
      return false;
    }

    DogTamingProgressComponent progressComponent =
        store.getComponent(entityRef, DogTamingProgressComponent.getComponentType());

    if (progressComponent == null || !progressComponent.hasProgress()) {
      progressComponent =
          new DogTamingProgressComponent(
              MIN_REQUIRED_FEEDINGS
                  + RANDOM.nextInt(MAX_REQUIRED_FEEDINGS - MIN_REQUIRED_FEEDINGS + 1));
      store.putComponent(
          entityRef, DogTamingProgressComponent.getComponentType(), progressComponent);
      LOGGER.at(Level.INFO).log(
          "Started taming progress - requires %d feedings",
          progressComponent.getRequiredFeedings());
    }

    if (progressComponent.hasProgress()
        && System.currentTimeMillis() - progressComponent.getLastFedTimestamp()
            < FEEDING_COOLDOWN_MS) {
      role.getStateSupport().setState(entityRef, "Wild", "Rejection", store);
      player
          .getPlayerRef()
          .sendMessage(
              Message.translation("dogs_companion.interactions.taming.cooldown")
                  .color(Constants.COLOR_WARNING));
      return true;
    }

    progressComponent.incrementFeeding();
    store.putComponent(entityRef, DogTamingProgressComponent.getComponentType(), progressComponent);
    InventoryHelper.consumeActiveHotbarItem(player, heldItem);

    role.getStateSupport().setState(entityRef, "Wild", "Feeding", store);

    LOGGER.at(Level.INFO).log(
        "Dog fed by %s: %d/%d",
        username, progressComponent.getFeedingCount(), progressComponent.getRequiredFeedings());

    if (progressComponent.isComplete()) {
      completeTaming(
          entityRef, role, store, player, playerUUID, username, itemName, progressComponent);
      return true;
    }

    player
        .getPlayerRef()
        .sendMessage(
            Message.translation("dogs_companion.interactions.taming.in_progress")
                .param(
                    "remaining",
                    String.valueOf(
                        progressComponent.getRequiredFeedings()
                            - progressComponent.getFeedingCount()))
                .color(Constants.COLOR_INFO));

    return true;
  }

  private static void completeTaming(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      UUID playerUUID,
      String username,
      String itemName,
      DogTamingProgressComponent progressComponent) {

    String dogName = DogsNamesManager.getRandomName();

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity != null) {
      try {
        Role currentRole = npcEntity.getRole();
        if (currentRole == null) {
          LOGGER.at(Level.WARNING).log("Failed to request role change: currentRole is null");
        } else {
          String tamedRoleName = DogType.fromRoleName(currentRole.getRoleName()).getTamedRoleName();
          if (tamedRoleName.isEmpty()) {
            LOGGER.at(Level.WARNING).log(
                "Failed to get tamed role name for DogType %s",
                DogType.fromRoleName(currentRole.getRoleName()));
          } else if (NPCPlugin.get().getIndex(tamedRoleName) < 0) {
            LOGGER.at(Level.WARNING).log("Failed to find role index for %s", tamedRoleName);
          } else {
            RoleChangeSystem.requestRoleChange(
                entityRef,
                currentRole,
                NPCPlugin.get().getIndex(tamedRoleName),
                true,
                null,
                null,
                store);
            LOGGER.at(Level.INFO).log(
                "Dog role change requested from %s to %s",
                currentRole.getRoleName(), tamedRoleName);
          }
        }
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).log("Failed to change dog role", e);
      }
    }

    DogsManager.getInstance().assignOwner(entityRef, playerUUID, username, dogName, store);
    store.removeComponent(entityRef, DogTamingProgressComponent.getComponentType());

    Ref<EntityStore> playerEntityRef = role.getStateSupport().getInteractionIterationTarget();
    if (playerEntityRef != null && playerEntityRef.isValid()) {
      PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
      if (playerRef != null) {
        UUIDComponent dogUuidComponent =
            store.getComponent(entityRef, UUIDComponent.getComponentType());
        UUID dogUuid = dogUuidComponent != null ? dogUuidComponent.getUuid() : null;
        String roleName =
            npcEntity != null && npcEntity.getRole() != null
                ? npcEntity.getRole().getRoleName()
                : null;
        DogTamingSuccessPage successPage =
            new DogTamingSuccessPage(
                playerRef, dogUuid != null ? dogUuid : playerUUID, roleName, dogName);
        player.getPageManager().openCustomPage(playerEntityRef, store, successPage);
      }
    }

    player
        .getPlayerRef()
        .sendMessage(
            Message.translation("dogs_companion.interactions.taming.success")
                .param("item", itemName)
                .param("dogName", dogName)
                .color(Constants.COLOR_SUCCESS));

    LOGGER.at(Level.INFO).log(
        "Dog successfully tamed by player %s with item %s (dogs: %d/%d)",
        username,
        itemName,
        DogsManager.getInstance().getDogCountByOwner(playerUUID, store),
        Constants.DEFAULT_DOG_LIMIT);
  }

  private static boolean isTamingItem(String itemName) {
    if (itemName == null) {
      return false;
    }
    return itemName.equals("Food_Wildmeat_Cooked") || itemName.equals("Food_Wildmeat_Raw");
  }
}
