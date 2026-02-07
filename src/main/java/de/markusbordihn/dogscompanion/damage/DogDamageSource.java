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

package de.markusbordihn.dogscompanion.damage;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class DogDamageSource implements Damage.Source {

  private final Ref<EntityStore> dogRef;

  public DogDamageSource(Ref<EntityStore> dogRef) {
    this.dogRef = dogRef;
  }

  @Nonnull
  public Ref<EntityStore> getDogRef() {
    return this.dogRef;
  }

  @Nonnull
  @Override
  public Message getDeathMessage(
      @Nonnull Damage info,
      @Nonnull Ref<EntityStore> targetRef,
      @Nonnull ComponentAccessor<EntityStore> componentAccessor) {

    Message dogMessage = Message.raw("a dog");

    DisplayNameComponent displayNameComponent =
        componentAccessor.getComponent(this.dogRef, DisplayNameComponent.getComponentType());

    if (displayNameComponent != null) {
      Message displayName = displayNameComponent.getDisplayName();
      if (displayName != null) {
        dogMessage = displayName;
      }
    }

    return Message.translation("server.general.killedBy").param("damageSource", dogMessage);
  }
}
