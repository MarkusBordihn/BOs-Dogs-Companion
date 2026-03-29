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

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.codecs.UUIDBinaryCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.math.vector.Vector3i;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bson.BsonDocument;
import org.bson.BsonValue;

public record DogDataEntry(
    @Nonnull UUID uuid,
    @Nullable UUID ownerUuid,
    @Nullable String ownerName,
    @Nonnull DogType dogType,
    @Nullable String name,
    @Nonnull DogState state,
    @Nullable Vector3i position,
    @Nonnull DogStatus status) {

  private static final String UUID_TAG = "UUID";
  private static final String OWNER_UUID_TAG = "OwnerUUID";
  private static final String OWNER_NAME_TAG = "OwnerName";
  private static final String TYPE_TAG = "Type";
  private static final String NAME_TAG = "Name";
  private static final String STATE_TAG = "State";
  private static final String POSITION_TAG = "Position";
  private static final String STATUS_TAG = "Status";

  private static final UUIDBinaryCodec UUID_CODEC = new UUIDBinaryCodec();
  private static final StringCodec STRING_CODEC = new StringCodec();

  public static final Codec<DogDataEntry> CODEC =
      new Codec<>() {
        @Override
        public DogDataEntry decode(@Nonnull BsonValue bson, @Nonnull ExtraInfo info) {
          BsonDocument doc = bson.asDocument();
          return new DogDataEntry(
              UUID_CODEC.decode(doc.get(UUID_TAG), info),
              doc.containsKey(OWNER_UUID_TAG)
                  ? UUID_CODEC.decode(doc.get(OWNER_UUID_TAG), info)
                  : null,
              doc.containsKey(OWNER_NAME_TAG)
                  ? STRING_CODEC.decode(doc.get(OWNER_NAME_TAG), info)
                  : null,
              DogType.CODEC.decode(doc.get(TYPE_TAG), info),
              doc.containsKey(NAME_TAG) ? STRING_CODEC.decode(doc.get(NAME_TAG), info) : null,
              DogState.CODEC.decode(doc.get(STATE_TAG), info),
              doc.containsKey(POSITION_TAG)
                  ? Vector3i.CODEC.decode(doc.get(POSITION_TAG), info)
                  : null,
              doc.containsKey(STATUS_TAG)
                  ? DogStatus.CODEC.decode(doc.get(STATUS_TAG), info)
                  : DogStatus.SPAWNED);
        }

        @Override
        public BsonValue encode(@Nonnull DogDataEntry entry, @Nonnull ExtraInfo info) {
          BsonDocument doc = new BsonDocument();
          doc.put(UUID_TAG, UUID_CODEC.encode(entry.uuid, info));
          if (entry.ownerUuid != null)
            doc.put(OWNER_UUID_TAG, UUID_CODEC.encode(entry.ownerUuid, info));
          if (entry.ownerName != null)
            doc.put(OWNER_NAME_TAG, STRING_CODEC.encode(entry.ownerName, info));
          doc.put(TYPE_TAG, DogType.CODEC.encode(entry.dogType, info));
          if (entry.name != null) doc.put(NAME_TAG, STRING_CODEC.encode(entry.name, info));
          doc.put(STATE_TAG, DogState.CODEC.encode(entry.state, info));
          if (entry.position != null)
            doc.put(POSITION_TAG, Vector3i.CODEC.encode(entry.position, info));
          doc.put(STATUS_TAG, DogStatus.CODEC.encode(entry.status, info));
          return doc;
        }

        @Override
        public Schema toSchema(@Nonnull SchemaContext context) {
          return Schema.anyOf();
        }
      };

  public static DogDataEntry empty() {
    return new DogDataEntry(
        UUID.randomUUID(),
        null,
        null,
        DogType.UNKNOWN,
        null,
        DogState.FOLLOWING,
        null,
        DogStatus.UNKNOWN);
  }

  public boolean hasOwner() {
    return ownerUuid != null;
  }

  public boolean isSpawned() {
    return status == DogStatus.SPAWNED;
  }

  public boolean isDead() {
    return status == DogStatus.DEATH;
  }

  public DogDataEntry withUuid(@Nonnull UUID newUuid) {
    return new DogDataEntry(newUuid, ownerUuid, ownerName, dogType, name, state, position, status);
  }

  public DogDataEntry withOwnerUuid(@Nullable UUID newOwnerUuid) {
    return new DogDataEntry(uuid, newOwnerUuid, ownerName, dogType, name, state, position, status);
  }

  public DogDataEntry withOwnerName(@Nullable String newOwnerName) {
    return new DogDataEntry(uuid, ownerUuid, newOwnerName, dogType, name, state, position, status);
  }

  public DogDataEntry withOwner(@Nullable UUID newOwnerUuid, @Nullable String newOwnerName) {
    return new DogDataEntry(
        uuid, newOwnerUuid, newOwnerName, dogType, name, state, position, status);
  }

  public DogDataEntry withDogType(@Nonnull DogType newDogType) {
    return new DogDataEntry(uuid, ownerUuid, ownerName, newDogType, name, state, position, status);
  }

  public DogDataEntry withName(@Nullable String newName) {
    return new DogDataEntry(uuid, ownerUuid, ownerName, dogType, newName, state, position, status);
  }

  public DogDataEntry withState(@Nonnull DogState newState) {
    return new DogDataEntry(uuid, ownerUuid, ownerName, dogType, name, newState, position, status);
  }

  public DogDataEntry withPosition(@Nullable Vector3i newPosition) {
    return new DogDataEntry(uuid, ownerUuid, ownerName, dogType, name, state, newPosition, status);
  }

  public DogDataEntry withStatus(@Nonnull DogStatus newStatus) {
    return new DogDataEntry(uuid, ownerUuid, ownerName, dogType, name, state, position, newStatus);
  }
}
