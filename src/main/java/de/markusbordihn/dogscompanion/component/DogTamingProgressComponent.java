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
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.DogsCompanion;
import javax.annotation.Nonnull;

public class DogTamingProgressComponent implements Component<EntityStore> {

  public static final String FEEDING_COUNT_TAG = "FeedingCount";
  public static final String REQUIRED_FEEDINGS_TAG = "RequiredFeedings";
  public static final String LAST_FED_TIMESTAMP_TAG = "LastFedTimestamp";

  @Nonnull
  public static final BuilderCodec<DogTamingProgressComponent> CODEC =
      BuilderCodec.builder(DogTamingProgressComponent.class, DogTamingProgressComponent::new)
          .append(
              new KeyedCodec<>(FEEDING_COUNT_TAG, new IntegerCodec()),
              (component, value) -> component.feedingCount = value,
              component -> component.feedingCount)
          .documentation("The number of times the dog has been fed.")
          .add()
          .append(
              new KeyedCodec<>(REQUIRED_FEEDINGS_TAG, new IntegerCodec()),
              (component, value) -> component.requiredFeedings = value,
              component -> component.requiredFeedings)
          .documentation("The number of feedings required to tame the dog.")
          .add()
          .append(
              new KeyedCodec<>(LAST_FED_TIMESTAMP_TAG, new LongCodec()),
              (component, value) -> component.lastFedTimestamp = value,
              component -> component.lastFedTimestamp)
          .documentation("The timestamp when the dog was last fed.")
          .add()
          .build();

  private int feedingCount;
  private int requiredFeedings;
  private long lastFedTimestamp;

  public DogTamingProgressComponent() {
    this.feedingCount = 0;
    this.requiredFeedings = 0;
    this.lastFedTimestamp = 0;
  }

  public DogTamingProgressComponent(int requiredFeedings) {
    this.feedingCount = 0;
    this.requiredFeedings = requiredFeedings;
    this.lastFedTimestamp = 0;
  }

  public static ComponentType<EntityStore, DogTamingProgressComponent> getComponentType() {
    return DogsCompanion.getInstance().dogTamingProgressComponentType;
  }

  public boolean hasProgress() {
    return feedingCount > 0;
  }

  public boolean isComplete() {
    return feedingCount >= requiredFeedings;
  }

  public int getFeedingCount() {
    return feedingCount;
  }

  public int getRequiredFeedings() {
    return requiredFeedings;
  }

  public long getLastFedTimestamp() {
    return lastFedTimestamp;
  }

  public void incrementFeeding() {
    this.feedingCount++;
    this.lastFedTimestamp = System.currentTimeMillis();
  }

  public void reset() {
    this.feedingCount = 0;
    this.requiredFeedings = 0;
    this.lastFedTimestamp = 0;
  }

  @Override
  @Nonnull
  public DogTamingProgressComponent clone() {
    DogTamingProgressComponent clone = new DogTamingProgressComponent();
    clone.feedingCount = this.feedingCount;
    clone.requiredFeedings = this.requiredFeedings;
    clone.lastFedTimestamp = this.lastFedTimestamp;
    return clone;
  }
}
