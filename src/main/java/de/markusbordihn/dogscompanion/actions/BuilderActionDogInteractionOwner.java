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

package de.markusbordihn.dogscompanion.actions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.interaction.ItemInteractionOwner;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class BuilderActionDogInteractionOwner extends BuilderActionDogInteractionBase {
  public static final String BUILDER_ID = "DogInteractionOwner";

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Override
  public String getBuilderId() {
    return BUILDER_ID;
  }

  @Nonnull
  @Override
  public ActionDogInteractionOwner build(BuilderSupport support) {
    return new ActionDogInteractionOwner(this, support);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Handles owner dog interaction events";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Custom action that handles when the owner interacts with their tamed dog NPC";
  }

  public static class ActionDogInteractionOwner extends ActionDogInteractionBase {

    public ActionDogInteractionOwner(
        BuilderActionDogInteractionBase builder, BuilderSupport support) {
      super(builder);
    }

    @Override
    public boolean canExecute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {
      if (!isDogTamed(entityRef, store)) {
        LOGGER.at(Level.FINE).log("Dog is not tamed");
        return false;
      }
      Player player = getPlayerFromInfoProvider(role, infoProvider, store);
      if (player == null) {
        LOGGER.at(Level.FINE).log("No player found");
        return false;
      }
      if (!isOwner(entityRef, getPlayerUUID(player), store)) {
        LOGGER.at(Level.FINE).log("Player is not the owner");
        return false;
      }
      LOGGER.at(Level.FINE).log("Owner can interact with dog");
      return true;
    }

    @Override
    public boolean execute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {
      Player player = getPlayerFromInfoProvider(role, infoProvider, store);
      if (player == null) {
        LOGGER.at(Level.WARNING).log("No player found in execute");
        return false;
      }

      ItemStack heldItem = getHeldItem(player);

      if (heldItem != null) {
        LOGGER.at(Level.FINE).log(
            "Owner holding item: %s - routing to item interaction", heldItem.getItemId());
        return ItemInteractionOwner.handle(entityRef, role, store, player, heldItem);
      }

      DogStateComponent stateComponent =
          store.getComponent(entityRef, DogStateComponent.getComponentType());

      if (stateComponent == null) {
        LOGGER.at(Level.WARNING).log("DogStateComponent not found for dog");
        return false;
      }

      DogState currentState = stateComponent.getState();
      DogState newState = currentState == DogState.SITTING ? DogState.FOLLOWING : DogState.SITTING;
      String newSubState = (newState == DogState.SITTING) ? "Sitting" : "Default";

      LOGGER.at(Level.FINE).log(
          "Dog state change: %s -> %s (substate: %s)", currentState, newState, newSubState);

      DogsManager.getInstance().updateDogState(entityRef, newState, store);

      NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        npcEntity.getRole().getStateSupport().setState(entityRef, "Pet", newSubState, store);
      } else {
        LOGGER.at(Level.WARNING).log("NPCEntity or Role not found - state update incomplete");
      }

      return true;
    }
  }
}
