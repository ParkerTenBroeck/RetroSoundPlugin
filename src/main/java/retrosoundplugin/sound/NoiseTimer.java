/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 */
package retrosoundplugin.sound;

import retrosoundplugin.utils;

public class NoiseTimer extends Timer {

    private int divider = 0;
    private int[] values = genvalues(1, 1);
    private int prevduty = 1;
    private final  int periodadd = 0;

    public NoiseTimer() {
        period = 1;
    }

    @Override
    public void setduty(int duty) {
        if (duty != prevduty) {
            values = genvalues(duty, values[position]);
            position = 0;
        }
        prevduty = duty;
    }

    @Override
    public final void clock() {
        ++divider;
        // note: stay away from negative division to avoid rounding problems
        int periods = (divider + period + periodadd) / (period + periodadd);
        if (periods < 0) {
            periods = 0; // can happen if period or periodadd were made smaller
        }
        position = (position + periods) % values.length;
        divider -= (period + periodadd) * periods;
    }

    @Override
    public final int getval() {
        return (values[position] & 1);
    }

    @Override
    public final void reset() {
        position = 0;
    }

    @Override
    public final void clock(final int cycles) {
        divider += cycles;
        // note: stay away from negative division to avoid rounding problems
        int periods = (divider + period + periodadd) / (period + periodadd);
        if (periods < 0) {
            periods = 0; // can happen if period or periodadd were made smaller
        }
        position = (position + periods) % values.length;
        divider -= (period + periodadd) * periods;
    }

    @Override
    public final void setperiod(final int newperiod) {
        period = newperiod;
    }

    public  int[] genvalues(int whichbit, int seed) {
        int[] tehsuck = new int[(whichbit == 1) ? 32767 : 93];
        for (int i = 0; i < tehsuck.length; ++i) {
            seed = (seed >> 1)
                    | ((((seed & (1 << whichbit)) != 0)
                    ^ ((seed & (utils.BIT0)) != 0))
                    ? 16384 : 0);
            tehsuck[i] = seed;
        }
        return tehsuck;

    }

    @Override
    public void setduty(int[] duty) {
        throw new UnsupportedOperationException("Not supported on noise channel.");
    }
}
