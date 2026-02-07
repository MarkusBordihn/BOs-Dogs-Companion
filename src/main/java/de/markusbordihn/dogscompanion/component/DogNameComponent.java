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

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.DogsCompanion;
import javax.annotation.Nonnull;

public class DogNameComponent implements Component<EntityStore> {

  public static final String NAME_TAG = "Name";

  @Nonnull
  public static final BuilderCodec<DogNameComponent> CODEC =
      BuilderCodec.builder(DogNameComponent.class, DogNameComponent::new)
          .append(
              new KeyedCodec<>(NAME_TAG, Codec.STRING),
              (component, value) -> component.name = value,
              component -> component.name)
          .documentation("The raw name of the dog without state symbols.")
          .addValidator(Validators.nonNull())
          .add()
          .build();

  @Nonnull private String name = "";

  public DogNameComponent() {}

  public DogNameComponent(@Nonnull String name) {
    this.name = name;
  }

  public static ComponentType<EntityStore, DogNameComponent> getComponentType() {
    return DogsCompanion.getInstance().dogNameComponentType;
  }

  @Nonnull
  public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public DogNameComponent clone() {
    DogNameComponent cloned = new DogNameComponent();
    cloned.name = this.name;
    return cloned;
  }
}
