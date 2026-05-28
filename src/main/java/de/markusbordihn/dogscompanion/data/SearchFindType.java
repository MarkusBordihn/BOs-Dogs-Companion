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

package de.markusbordihn.dogscompanion.data;

import java.util.concurrent.ThreadLocalRandom;

public enum SearchFindType {
  BONE("Deco_Bone_Full", "Deco_Bone_Spike"),
  FEATHER("Ingredient_Feathers_Light", "Ingredient_Feathers_Blue"),
  BERRY("Plant_Fruit_Berries_Red", "Plant_Fruit_Apple"),
  CRYSTAL("Ingredient_Crystal_White", "Ingredient_Crystal_Green"),
  HIDE("Ingredient_Hide_Soft", "Ingredient_Hide_Light"),
  FLOWER("Plant_Flower_Common_White", "Plant_Flower_Common_Yellow");

  private final String[] itemIds;

  SearchFindType(String... itemIds) {
    this.itemIds = itemIds;
  }

  public static SearchFindType random() {
    SearchFindType[] values = values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }

  public String getItemId() {
    return itemIds[ThreadLocalRandom.current().nextInt(itemIds.length)];
  }
}
