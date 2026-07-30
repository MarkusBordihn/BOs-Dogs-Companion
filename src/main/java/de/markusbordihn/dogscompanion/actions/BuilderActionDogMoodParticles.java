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
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class BuilderActionDogMoodParticles extends BuilderActionBase {
  public static final String BUILDER_ID = "DogMoodParticles";

  private static final double MIN_PARTICLE_INTERVAL = 15.0;
  private static final double MAX_PARTICLE_INTERVAL = 30.0;
  private static final double PARTICLE_VIEW_RANGE = 48.0;
  private static final Vector3f PARTICLE_OFFSET = new Vector3f(0.0f, 1.2f, 0.0f);

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
    return "Accumulates deltaTime per entity and every 15-30s emits a particle system matching "
        + "the dog's current state (hearts when following, playing or wandering, sleepy while "
        + "sleeping, happy while sitting or waiting). Particles are sent directly to nearby "
        + "players, so the sub-state machine is left untouched.";
  }

  public static class ActionDogMoodParticles extends ActionBase {

    private static final Map<Ref<EntityStore>, MoodTimer> timerByEntity = new ConcurrentHashMap<>();

    public ActionDogMoodParticles(BuilderActionBase builder) {
      super(builder);
    }

    public static void clearEntity(@Nonnull Ref<EntityStore> entityRef) {
      timerByEntity.remove(entityRef);
    }

    @Nullable
    private static String toMoodParticleSystem(@Nonnull DogState state) {
      return switch (state) {
        case FOLLOWING, PLAYING, WANDERING, SITTING, WAITING -> "Hearts_Subtle";
        case SLEEPING -> "Sleepy";
        case SEARCHING -> "Question_Subtle";
        default -> null;
      };
    }

    private static void spawnMoodParticles(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String particleSystem,
        @Nonnull Store<EntityStore> store) {
      NetworkId networkId = store.getComponent(entityRef, NetworkId.getComponentType());
      TransformComponent transform =
          store.getComponent(entityRef, TransformComponent.getComponentType());
      if (networkId == null || transform == null) {
        return;
      }

      ModelParticle modelParticle = new ModelParticle();
      modelParticle.setSystemId(particleSystem);
      modelParticle.setPositionOffset(PARTICLE_OFFSET);

      SpawnModelParticles packet =
          new SpawnModelParticles(
              networkId.getId(),
              new com.hypixel.hytale.protocol.ModelParticle[] {modelParticle.toPacket()});

      Vector3d particlePosition = new Vector3d(transform.getPosition());
      SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
          store.getResource(EntityModule.get().getPlayerSpatialResourceType());
      List<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
      playerSpatialResource
          .getSpatialStructure()
          .collect(particlePosition, PARTICLE_VIEW_RANGE, nearbyPlayers);

      for (Ref<EntityStore> playerEntityRef : nearbyPlayers) {
        PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (playerRef != null) {
          playerRef.getPacketHandler().write(packet);
        }
      }
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

      MoodTimer timer =
          timerByEntity.compute(
              entityRef,
              (key, previous) ->
                  previous == null ? MoodTimer.started() : previous.advance(deltaTime));

      if (timer.elapsed() < timer.threshold()) {
        return true;
      }

      timerByEntity.put(entityRef, MoodTimer.reset());

      DogStateComponent stateComponent =
          store.getComponent(entityRef, DogStateComponent.getComponentType());
      if (stateComponent == null) {
        return true;
      }

      String particleSystem = toMoodParticleSystem(stateComponent.getState());
      if (particleSystem != null) {
        spawnMoodParticles(entityRef, particleSystem, store);
      }

      return true;
    }

    private record MoodTimer(double elapsed, double threshold) {

      static MoodTimer started() {
        // Stagger the first emission so dogs spawned together do not pulse in lockstep.
        return new MoodTimer(
            ThreadLocalRandom.current().nextDouble(0, MIN_PARTICLE_INTERVAL), nextThreshold());
      }

      static MoodTimer reset() {
        return new MoodTimer(0, nextThreshold());
      }

      private static double nextThreshold() {
        return ThreadLocalRandom.current().nextDouble(MIN_PARTICLE_INTERVAL, MAX_PARTICLE_INTERVAL);
      }

      MoodTimer advance(double deltaTime) {
        return new MoodTimer(this.elapsed + deltaTime, this.threshold);
      }
    }
  }
}
