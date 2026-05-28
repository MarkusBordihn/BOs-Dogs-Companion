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
import com.hypixel.hytale.server.npc.asset.builder.BuilderFactory;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogCycleState;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionBase;
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
import java.util.function.Supplier;
import java.util.logging.Level;

public class NPCSetupHandler {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @SuppressWarnings("unchecked")
  private static final Class<? extends BuilderActionDogInteractionBase>[] DOG_INTERACTION_BUILDERS =
      new Class[] {
        BuilderActionDogInteractionWild.class,
        BuilderActionDogInteractionOwner.class,
        BuilderActionDogInteractionStranger.class
      };

  private static final String[] SENSOR_IDS = {
    BuilderSensorIsDogTamed.SENSOR_ID,
    BuilderSensorIsOwner.SENSOR_ID,
    BuilderSensorOwnerAttacked.BUILDER_ID,
    BuilderSensorIsHoldingFood.SENSOR_ID,
    BuilderSensorIsHoldingEmptyHand.SENSOR_ID,
    BuilderSensorIsHoldingDogWhistle.SENSOR_ID,
  };

  @SuppressWarnings("rawtypes")
  private static final Supplier[] SENSOR_SUPPLIERS = {
    BuilderSensorIsDogTamed::new,
    BuilderSensorIsOwner::new,
    BuilderSensorOwnerAttacked::new,
    BuilderSensorIsHoldingFood::new,
    BuilderSensorIsHoldingEmptyHand::new,
    BuilderSensorIsHoldingDogWhistle::new,
  };

  private boolean actionsRegistered = false;
  private boolean sensorsRegistered = false;

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
    if (actionsRegistered) {
      LOGGER.at(Level.INFO).log("Custom actions already registered - skipping");
      return;
    }

    LOGGER.at(Level.INFO).log("NPC Plugin setup detected - registering custom actions");

    BuilderFactory<Action> actionFactory = npcPlugin.getBuilderManager().getFactory(Action.class);

    int registeredCount = 0;
    for (Class<? extends BuilderActionDogInteractionBase> builderClass : DOG_INTERACTION_BUILDERS) {
      try {
        BuilderActionDogInteractionBase builder =
            builderClass.getDeclaredConstructor().newInstance();
        String builderId = builder.getBuilderId();
        actionFactory.add(
            builderId,
            () -> {
              try {
                return builderClass.getDeclaredConstructor().newInstance();
              } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to instantiate action builder: " + builderClass.getSimpleName(), e);
              }
            });
        registeredCount++;
        LOGGER.at(Level.INFO).log("Registered action: %s", builderId);
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).log(
            "Failed to register action builder: %s", builderClass.getSimpleName(), e);
      }
    }

    try {
      actionFactory.add(BuilderActionDogCycleState.BUILDER_ID, BuilderActionDogCycleState::new);
      registeredCount++;
      LOGGER.at(Level.INFO).log("Registered action: %s", BuilderActionDogCycleState.BUILDER_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register action: %s", BuilderActionDogCycleState.BUILDER_ID, e);
    }

    try {
      actionFactory.add(
          BuilderActionDogReturnToPreviousState.BUILDER_ID,
          BuilderActionDogReturnToPreviousState::new);
      registeredCount++;
      LOGGER.at(Level.INFO).log(
          "Registered action: %s", BuilderActionDogReturnToPreviousState.BUILDER_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register action: %s", BuilderActionDogReturnToPreviousState.BUILDER_ID, e);
    }

    try {
      actionFactory.add(
          BuilderActionDogMoodParticles.BUILDER_ID, BuilderActionDogMoodParticles::new);
      registeredCount++;
      LOGGER.at(Level.INFO).log("Registered action: %s", BuilderActionDogMoodParticles.BUILDER_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register action: %s", BuilderActionDogMoodParticles.BUILDER_ID, e);
    }

    try {
      actionFactory.add(BuilderActionDogSearchReturn.BUILDER_ID, BuilderActionDogSearchReturn::new);
      registeredCount++;
      LOGGER.at(Level.INFO).log("Registered action: %s", BuilderActionDogSearchReturn.BUILDER_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register action: %s", BuilderActionDogSearchReturn.BUILDER_ID, e);
    }

    actionsRegistered = true;
    LOGGER.at(Level.INFO).log("Registered %d dog interaction actions", registeredCount);
  }

  @SuppressWarnings("unchecked")
  private void registerDogSensors(NPCPlugin npcPlugin) {
    if (sensorsRegistered) {
      LOGGER.at(Level.INFO).log("Custom sensors already registered - skipping");
      return;
    }

    LOGGER.at(Level.INFO).log("Registering custom dog sensors...");

    BuilderFactory<Sensor> sensorFactory = npcPlugin.getBuilderManager().getFactory(Sensor.class);

    int registeredCount = 0;
    for (int i = 0; i < SENSOR_IDS.length; i++) {
      String sensorId = SENSOR_IDS[i];
      try {
        sensorFactory.add(sensorId, SENSOR_SUPPLIERS[i]);
        registeredCount++;
        LOGGER.at(Level.INFO).log("Registered sensor: %s", sensorId);
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).log("Failed to register sensor: %s", sensorId, e);
      }
    }

    sensorsRegistered = true;
    LOGGER.at(Level.INFO).log("Registered %d dog sensors", registeredCount);
  }
}
