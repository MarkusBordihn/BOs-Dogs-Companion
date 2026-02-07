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
import de.markusbordihn.dogscompanion.utils.DogNameplateUtils;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class BuilderActionDogCycleState extends BuilderActionBase {
  public static final String BUILDER_ID = "DogCycleState";
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
  public BuilderActionDogCycleState readConfig(JsonElement config) {
    return this;
  }

  @Nonnull
  @Override
  public ActionDogCycleState build(BuilderSupport support) {
    return new ActionDogCycleState(this, support);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Cycles dog through combat states";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Cycles the dog through combat states: Default → Defense → Offense → Sitting → Default";
  }

  public static class ActionDogCycleState extends ActionBase {

    public ActionDogCycleState(BuilderActionBase builder, BuilderSupport support) {
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
        LOGGER.at(Level.WARNING).log("DogCycleState: No state component found");
        return false;
      }

      DogState currentState = stateComponent.getState();
      DogState nextState = getNextState(currentState);
      String nextSubstate = getSubstateName(nextState);

      // Update the dog's state
      DogsManager.getInstance().updateDogState(entityRef, nextState, store);

      // Update NPC role substate
      NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        npcEntity.getRole().getStateSupport().setState(entityRef, "Pet", nextSubstate, store);

        // Update nameplate to show state symbol
        DogNameComponent nameComponent =
            store.getComponent(entityRef, DogNameComponent.getComponentType());
        if (nameComponent != null) {
          String dogName = nameComponent.getName();
          DogNameplateUtils.updateNameplateWithState(entityRef, dogName, store);
        }

        LOGGER.at(Level.FINE).log(
            "Dog state cycled from %s to %s (substate: %s)", currentState, nextState, nextSubstate);
      }

      return true;
    }

    @Nonnull
    private DogState getNextState(@Nonnull DogState currentState) {
      return switch (currentState) {
        case FOLLOWING -> DogState.DEFENSE;
        case DEFENSE -> DogState.OFFENSE;
        case OFFENSE -> DogState.SITTING;
        case SITTING -> DogState.FOLLOWING;
        default -> DogState.FOLLOWING;
      };
    }

    @Nonnull
    private String getSubstateName(@Nonnull DogState state) {
      return switch (state) {
        case SITTING -> "Sitting";
        case DEFENSE -> "Defense";
        case OFFENSE -> "Offense";
        case FOLLOWING -> "Default";
        default -> "Default";
      };
    }
  }
}
