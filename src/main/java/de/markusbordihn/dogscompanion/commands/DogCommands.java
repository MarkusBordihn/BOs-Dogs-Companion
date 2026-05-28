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

package de.markusbordihn.dogscompanion.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DogCommands extends AbstractCommandCollection {

  private final List<DogCommand> dogSubCommands = new ArrayList<>();

  public DogCommands() {
    super("dog", "Dog management commands");
    this.addAliases("dogs");

    register(new DogInfoCommand());
    register(new DogNameCommand());
    register(new DogListCommand());
    register(new DogOwnerCommand());

    register(new DogSitCommand());
    register(new DogFollowCommand());
    register(new DogWaitCommand());
    register(new DogWanderCommand());
    register(new DogPlayCommand());
    register(new DogSleepCommand());
    register(new DogSearchCommand());

    register(new DogAttackCommand());

    register(new DogSpawnCommand());
    register(new DogDespawnCommand());
    register(new DogReleaseCommand());

    register(new DogReloadCommand());
  }

  private void register(DogCommand cmd) {
    this.addSubCommand(cmd);
    this.dogSubCommands.add(cmd);
  }

  public Set<String> buildPlayerPermissionNodes() {
    Set<String> nodes =
        dogSubCommands.stream()
            .filter(cmd -> !cmd.requiresOp())
            .map(DogCommand::getPermission)
            .collect(Collectors.toCollection(HashSet::new));
    nodes.add(this.getPermission());
    return nodes;
  }
}
