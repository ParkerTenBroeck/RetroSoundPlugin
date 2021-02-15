/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 */
package retrosoundplugin.sound;

public abstract class Timer {

    protected int period;
    protected int position;

    public final int getperiod() {
        return period;
    }

    public abstract void setperiod(final int newperiod);

    public abstract void setduty(int duty);

    public abstract void setduty(int[] duty);

    public abstract void reset();

    public abstract void clock();

    public abstract void clock(final int cycles);

    public abstract int getval();
}
