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

package de.markusbordihn.dogscompanion.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.DogsCompanion;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.data.DogStateData;
import javax.annotation.Nonnull;

public class DogStateComponent implements Component<EntityStore> {

  public static final String STATE_TAG = "State";
  public static final String PREVIOUS_STATE_TAG = "PreviousState";
  public static final String LAST_ATTACK_TIME_TAG = "LastAttackTime";

  private static final EnumCodec<DogState> STATE_CODEC = new EnumCodec<>(DogState.class);

  @Nonnull
  public static final BuilderCodec<DogStateComponent> CODEC =
      BuilderCodec.builder(DogStateComponent.class, DogStateComponent::new)
          .append(
              new KeyedCodec<>(STATE_TAG, STATE_CODEC),
              (component, value) -> component.data = component.data.withStateNoPrevious(value),
              component -> component.data.state())
          .documentation("The current state of the dog (SITTING, FOLLOWING).")
          .add()
          .append(
              new KeyedCodec<>(PREVIOUS_STATE_TAG, STATE_CODEC),
              (component, value) -> {
                if (value != null) {
                  component.data = new DogStateData(component.data.state(), value);
                }
              },
              component ->
                  component.data.previousState() != null
                      ? component.data.previousState()
                      : component.data.state())
          .documentation("The previous state before entering ATTACKING mode.")
          .add()
          .build();

  @Nonnull private DogStateData data;
  private Long lastAttackTime;

  public DogStateComponent() {
    this.data = DogStateData.defaultState();
    this.lastAttackTime = null;
  }

  public DogStateComponent(@Nonnull DogStateData data) {
    this.data = data;
  }

  public DogStateComponent(@Nonnull DogState state) {
    this.data = DogStateData.of(state);
  }

  public static ComponentType<EntityStore, DogStateComponent> getComponentType() {
    return DogsCompanion.getInstance().dogStateComponentType;
  }

  @Nonnull
  public DogStateData getData() {
    return data;
  }

  public void setData(@Nonnull DogStateData data) {
    this.data = data;
  }

  @Nonnull
  public DogState getState() {
    return data.state();
  }

  public void setState(@Nonnull DogState state) {
    this.data = data.withState(state);
  }

  public Long getLastAttackTime() {
    return lastAttackTime;
  }

  public void setLastAttackTime(Long lastAttackTime) {
    this.lastAttackTime = lastAttackTime;
  }

  @Override
  @Nonnull
  public DogStateComponent clone() {
    DogStateComponent cloned = new DogStateComponent();
    cloned.data = this.data;
    return cloned;
  }
}
