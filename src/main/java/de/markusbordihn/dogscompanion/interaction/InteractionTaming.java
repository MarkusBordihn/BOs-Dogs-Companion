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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.manager.DogsNamesManager;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class InteractionTaming {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final Random RANDOM = new Random();
  private static final double BASE_TAMING_CHANCE = 0.20;
  private static final String TAMED_ROLE_NAME = "DogsCompanion_Tamed";

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {
    String itemName = heldItem != null ? heldItem.getItemId() : null;

    if (!isTamingItem(itemName)) {
      return false;
    }

    if (player == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame dog - player is null");
      return false;
    }

    UUID playerUUID = player.getUuid();
    String username = player.getDisplayName();
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
      player.sendMessage(
          Message.translation("dogs_companion.taming.limit_reached")
              .param("current", String.valueOf(currentDogCount))
              .param("limit", String.valueOf(Constants.DEFAULT_DOG_LIMIT))
              .color(Constants.COLOR_ERROR));
      return false;
    }

    if (RANDOM.nextDouble() >= BASE_TAMING_CHANCE) {
      player.sendMessage(
          Message.translation("dogs_companion.taming.failed").color(Constants.COLOR_WARNING));
      consumeItem(player, heldItem);
      return true;
    }

    String dogName = DogsNamesManager.getRandomName();

    // Set dog name in nameplate
    com.hypixel.hytale.server.core.entity.nameplate.Nameplate nameplate =
        store.ensureAndGetComponent(
            entityRef,
            com.hypixel.hytale.server.core.entity.nameplate.Nameplate.getComponentType());
    nameplate.setText(dogName);

    // Set owner component with PLAYER name, not dog name
    DogOwnerComponent ownerComponent =
        store.getComponent(entityRef, DogOwnerComponent.getComponentType());
    if (ownerComponent == null) {
      ownerComponent = new DogOwnerComponent();
      store.addComponent(entityRef, DogOwnerComponent.getComponentType(), ownerComponent);
    }
    ownerComponent.setOwner(playerUUID, username);

    DogStateComponent stateComponent =
        store.getComponent(entityRef, DogStateComponent.getComponentType());
    if (stateComponent == null) {
      stateComponent = new DogStateComponent(DogState.FOLLOWING);
      store.addComponent(entityRef, DogStateComponent.getComponentType(), stateComponent);
    } else {
      stateComponent.setState(DogState.FOLLOWING);
    }

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity != null) {
      try {
        int tamedRoleIndex = NPCPlugin.get().getIndex(TAMED_ROLE_NAME);
        if (tamedRoleIndex < 0) {
          LOGGER.at(Level.WARNING).log(
              "Failed to find role index for DogsCompanion_Tamed (index=%d)", tamedRoleIndex);
        } else {
          Role currentRole = npcEntity.getRole();
          if (currentRole != null) {
            RoleChangeSystem.requestRoleChange(
                entityRef, currentRole, tamedRoleIndex, true, null, null, store);
            LOGGER.at(Level.INFO).log(
                "Dog role change requested from %s to %s", currentRole, TAMED_ROLE_NAME);
          } else {
            LOGGER.at(Level.WARNING).log(
                "Failed to request role change: role=%s, index=%d", currentRole, tamedRoleIndex);
          }
        }
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).log("Failed to change dog role to Dogs_Tamed", e);
      }
    }

    // Register dog with DogsManager
    DogsManager.getInstance().assignOwner(entityRef, playerUUID, username, dogName, store);

    player.sendMessage(
        Message.translation("dogs_companion.taming.success")
            .param("item", itemName)
            .param("dogName", dogName)
            .color(Constants.COLOR_SUCCESS));

    consumeItem(player, heldItem);

    LOGGER.at(Level.INFO).log(
        "Dog successfully tamed by player %s with item %s (dogs: %d/%d)",
        username, itemName, currentDogCount + 1, Constants.DEFAULT_DOG_LIMIT);

    return true;
  }

  private static boolean isTamingItem(String itemName) {
    if (itemName == null) {
      return false;
    }
    return itemName.equals("Food_Wildmeat_Cooked") || itemName.equals("Food_Wildmeat_Raw");
  }

  private static void consumeItem(Player player, ItemStack heldItem) {
    if (player == null || heldItem == null) {
      return;
    }

    Inventory inventory = player.getInventory();
    if (inventory == null) {
      return;
    }

    int slot = inventory.getActiveHotbarSlot();
    ItemStack currentItem = inventory.getHotbar().getItemStack((short) slot);
    if (currentItem != null && currentItem.getItemId().equals(heldItem.getItemId())) {
      int newQuantity = currentItem.getQuantity() - 1;
      if (newQuantity <= 0) {
        inventory.getHotbar().removeItemStackFromSlot((short) slot);
      } else {
        inventory
            .getHotbar()
            .setItemStackForSlot((short) slot, currentItem.withQuantity(newQuantity), false);
      }
    }
  }
}
