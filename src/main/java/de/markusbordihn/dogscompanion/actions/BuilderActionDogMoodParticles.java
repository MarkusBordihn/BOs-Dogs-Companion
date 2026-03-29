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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuilderActionDogMoodParticles extends BuilderActionBase {
  public static final String BUILDER_ID = "DogMoodParticles";

  private static final double MIN_PARTICLE_INTERVAL = 15.0;
  private static final double MAX_PARTICLE_INTERVAL = 30.0;

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
  public BuilderActionDogMoodParticles readConfig(JsonElement config) {
    return this;
  }

  @Nonnull
  @Override
  public ActionDogMoodParticles build(BuilderSupport support) {
    return new ActionDogMoodParticles(this);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Periodically shows mood particles based on the dog's current state";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Accumulates deltaTime per entity and every 15-30s sets a transient mood sub-state "
        + "(MoodHappy when following/playing, MoodSleeping when sleeping). "
        + "JSON SpawnParticles blocks react to the state and reset it via Timeout.";
  }

  public static class ActionDogMoodParticles extends ActionBase {

    private static final HashMap<Ref<EntityStore>, Double> elapsedByEntity = new HashMap<>();

    public ActionDogMoodParticles(BuilderActionBase builder) {
      super(builder);
    }

    @Nullable
    private static String toMoodSubState(@Nonnull DogState state) {
      return switch (state) {
        case FOLLOWING, PLAYING -> "MoodHappy";
        case SLEEPING -> "MoodSleeping";
        default -> null;
      };
    }

    @Override
    public boolean canExecute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {
      return store.getComponent(entityRef, DogStateComponent.getComponentType()) != null;
    }

    @Override
    public boolean execute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {

      double elapsedSeconds =
          elapsedByEntity.compute(
              entityRef,
              (key, previous) ->
                  previous == null
                      ? ThreadLocalRandom.current().nextDouble(0, MAX_PARTICLE_INTERVAL)
                      : previous + deltaTime);

      if (elapsedSeconds < MIN_PARTICLE_INTERVAL) {
        return true;
      }

      double particleThreshold =
          ThreadLocalRandom.current().nextDouble(MIN_PARTICLE_INTERVAL, MAX_PARTICLE_INTERVAL);
      if (elapsedSeconds < particleThreshold) {
        return true;
      }

      elapsedByEntity.put(entityRef, 0.0);

      if (role == null) {
        return true;
      }

      DogStateComponent stateComponent =
          store.getComponent(entityRef, DogStateComponent.getComponentType());
      if (stateComponent == null) {
        return true;
      }

      String moodSubState = toMoodSubState(stateComponent.getState());
      if (moodSubState != null) {
        role.getStateSupport().setState(entityRef, "Pet", moodSubState, store);
      }

      return true;
    }
  }
}
