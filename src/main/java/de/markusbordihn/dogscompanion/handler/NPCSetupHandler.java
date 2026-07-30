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

package de.markusbordihn.dogscompanion.handler;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.event.PluginSetupEvent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderFactory;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogCycleState;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionOwner;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionStranger;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionWild;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogMoodParticles;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogReturnToPreviousState;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogSearchReturn;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsDogTamed;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsHoldingDogWhistle;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsHoldingEmptyHand;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsHoldingFood;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsOwner;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorOwnerAttacked;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorOwnerPlayer;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

public class NPCSetupHandler {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final Map<String, Supplier<Builder<Action>>> ACTION_BUILDERS =
      Map.<String, Supplier<Builder<Action>>>of(
          BuilderActionDogInteractionWild.BUILDER_ID, BuilderActionDogInteractionWild::new,
          BuilderActionDogInteractionOwner.BUILDER_ID, BuilderActionDogInteractionOwner::new,
          BuilderActionDogInteractionStranger.BUILDER_ID, BuilderActionDogInteractionStranger::new,
          BuilderActionDogCycleState.BUILDER_ID, BuilderActionDogCycleState::new,
          BuilderActionDogReturnToPreviousState.BUILDER_ID,
              BuilderActionDogReturnToPreviousState::new,
          BuilderActionDogMoodParticles.BUILDER_ID, BuilderActionDogMoodParticles::new,
          BuilderActionDogSearchReturn.BUILDER_ID, BuilderActionDogSearchReturn::new);

  private static final Map<String, Supplier<Builder<Sensor>>> SENSOR_BUILDERS =
      Map.<String, Supplier<Builder<Sensor>>>of(
          BuilderSensorIsDogTamed.SENSOR_ID, BuilderSensorIsDogTamed::new,
          BuilderSensorIsOwner.SENSOR_ID, BuilderSensorIsOwner::new,
          BuilderSensorOwnerAttacked.BUILDER_ID, BuilderSensorOwnerAttacked::new,
          BuilderSensorOwnerPlayer.SENSOR_ID, BuilderSensorOwnerPlayer::new,
          BuilderSensorIsHoldingFood.SENSOR_ID, BuilderSensorIsHoldingFood::new,
          BuilderSensorIsHoldingEmptyHand.SENSOR_ID, BuilderSensorIsHoldingEmptyHand::new,
          BuilderSensorIsHoldingDogWhistle.SENSOR_ID, BuilderSensorIsHoldingDogWhistle::new);

  private boolean actionsRegistered = false;
  private boolean sensorsRegistered = false;

  private static <T> int registerBuilders(
      BuilderFactory<T> factory, Map<String, Supplier<Builder<T>>> builders, String builderKind) {
    int registeredCount = 0;
    for (Map.Entry<String, Supplier<Builder<T>>> builder : builders.entrySet()) {
      try {
        factory.add(builder.getKey(), builder.getValue());
        registeredCount++;
        LOGGER.at(Level.FINE).log("Registered %s: %s", builderKind, builder.getKey());
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).withCause(e).log(
            "Failed to register %s: %s", builderKind, builder.getKey());
      }
    }

    return registeredCount;
  }

  public void onPluginSetup(PluginSetupEvent event) {
    if (event.getPlugin() instanceof NPCPlugin npcPlugin) {
      onNpcPluginReady(npcPlugin);
    }
  }

  public void onNpcPluginReady(NPCPlugin npcPlugin) {
    registerDogInteractionActions(npcPlugin);
    registerDogSensors(npcPlugin);
  }

  private void registerDogInteractionActions(NPCPlugin npcPlugin) {
    if (this.actionsRegistered) {
      LOGGER.at(Level.FINE).log("Custom actions already registered - skipping");
      return;
    }

    BuilderFactory<Action> actionFactory = npcPlugin.getBuilderManager().getFactory(Action.class);
    int registeredCount = registerBuilders(actionFactory, ACTION_BUILDERS, "action");

    this.actionsRegistered = true;
    LOGGER.at(Level.INFO).log("Registered %d dog actions", registeredCount);
  }

  private void registerDogSensors(NPCPlugin npcPlugin) {
    if (this.sensorsRegistered) {
      LOGGER.at(Level.FINE).log("Custom sensors already registered - skipping");
      return;
    }

    BuilderFactory<Sensor> sensorFactory = npcPlugin.getBuilderManager().getFactory(Sensor.class);
    int registeredCount = registerBuilders(sensorFactory, SENSOR_BUILDERS, "sensor");

    this.sensorsRegistered = true;
    LOGGER.at(Level.INFO).log("Registered %d dog sensors", registeredCount);
  }
}
