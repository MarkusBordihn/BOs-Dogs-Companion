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

public final class DogCommands extends AbstractCommandCollection {
  public DogCommands() {
    super("dog", "Dog management commands");
    this.addAliases("dogs");

    // Basic information and management
    this.addSubCommand(new DogInfoCommand());
    this.addSubCommand(new DogNameCommand());
    this.addSubCommand(new DogListCommand());
    this.addSubCommand(new DogOwnerCommand());
    
    // Behavior commands
    this.addSubCommand(new DogSitCommand());
    this.addSubCommand(new DogFollowCommand());
    this.addSubCommand(new DogWaitCommand());
    this.addSubCommand(new DogWanderCommand());
    this.addSubCommand(new DogPlayCommand());
    this.addSubCommand(new DogSleepCommand());
    
    // Combat commands
    this.addSubCommand(new DogAttackCommand());
    
    // Lifecycle commands
    this.addSubCommand(new DogSpawnCommand());
    this.addSubCommand(new DogDespawnCommand());
    this.addSubCommand(new DogReleaseCommand());
  }
}
