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

import com.google.gson.JsonElement;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.utils.DogCombatUtils;
import de.markusbordihn.dogscompanion.utils.DogNameplateUtils;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class BuilderActionDogReturnToPreviousState extends BuilderActionBase {
  public static final String BUILDER_ID = "DogReturnToPreviousState";
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Nonnull
  public String getBuilderId() {
    return BUILDER_ID;
  }

  @Nonnull
  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Stable;
  }

  @Nonnull
  @Override
  public BuilderActionDogReturnToPreviousState readConfig(JsonElement config) {
    return this;
  }

  @Nonnull
  @Override
  public ActionDogReturnToPreviousState build(BuilderSupport support) {
    return new ActionDogReturnToPreviousState(this, support);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Returns dog to previous state";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Returns the dog to its previous state before entering ATTACKING mode";
  }

  public static class ActionDogReturnToPreviousState extends ActionBase {

    public ActionDogReturnToPreviousState(BuilderActionBase builder, BuilderSupport support) {
      super(builder);
    }

    @Override
    public boolean canExecute(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Role role,
        InfoProvider infoProvider,
        double deltaTime,
        @Nonnull Store<EntityStore> store) {
      return true;
    }

    @Override
    public boolean execute(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Role role,
        InfoProvider infoProvider,
        double deltaTime,
        @Nonnull Store<EntityStore> store) {

      DogStateComponent stateComponent =
          store.getComponent(entityRef, DogStateComponent.getComponentType());
      if (stateComponent == null) {
        LOGGER.at(Level.WARNING).log("DogReturnToPreviousState: No state component found");
        return false;
      }

      // Get previous state or default to FOLLOWING
      DogState previousState = stateComponent.getData().getStateOrPrevious();

      // Get corresponding NPC substate name
      String substateName = getSubstateName(previousState);

      // Update the dog's state
      DogsManager.getInstance().updateDogState(entityRef, previousState, store);

      // Update NPC role substate
      NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        npcEntity.getRole().getStateSupport().setState(entityRef, "Pet", substateName, store);

        // Clear attack target when returning to previous state
        DogCombatUtils.clearTarget(entityRef, store);

        // Update nameplate to show state symbol
        DogNameComponent nameComponent =
            store.getComponent(entityRef, DogNameComponent.getComponentType());
        if (nameComponent != null) {
          String dogName = nameComponent.getName();
          DogNameplateUtils.updateNameplateWithState(entityRef, dogName, store);
        }

        LOGGER.at(Level.FINE).log(
            "Dog returning to previous state: %s (substate: %s)", previousState, substateName);
      }

      return true;
    }

    @Nonnull
    private String getSubstateName(@Nonnull DogState state) {
      return switch (state) {
        case SITTING -> "Sitting";
        case SLEEPING -> "Sleeping";
        case PLAYING -> "Playing";
        case WAITING -> "Waiting";
        case WANDERING -> "Wandering";
        case SEARCHING -> "Searching";
        case DEFENSE -> "Defense";
        case OFFENSE -> "Offense";
        case ATTACKING -> "Attacking";
        case STRIKING -> "Attacking";
        case FOLLOWING -> "Default";
      };
    }
  }
}
