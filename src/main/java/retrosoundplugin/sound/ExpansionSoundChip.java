/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 */
package retrosoundplugin.sound;

/**
 *
 * @author Andrew
 */
public interface ExpansionSoundChip {

    public void clock(final int cycles);

    public void write(int register, int data);

    public int getval();
}
