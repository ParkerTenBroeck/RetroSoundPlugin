/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 */
package retrosoundplugin.sound;

public class TriangleTimer extends Timer {

    private int divider = 0;
    private final  int periodadd = 1;
    private final  int[] triangle = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6,
            5, 4, 3, 2, 1, 0};

    public TriangleTimer() {
        period = 0;
        position = 0;
    }

    @Override
    public final void reset() {
        //no way to reset the triangle
    }

    @Override
    public final void clock() {
        if (period == 0) {
            return;
        }
        ++divider;
        // note: stay away from negative division to avoid rounding problems
        int periods = (divider + period + periodadd) / (period + periodadd);
        if (periods < 0) {
            periods = 0; // can happen if period or periodadd were made smaller
        }
        position = (position + periods) & 0x1F;
        divider -= (period + periodadd) * periods;
    }

    @Override
    public final void clock(final int cycles) {
        if (period == 0) {
            return;
        }
        divider += cycles;
        // note: stay away from negative division to avoid rounding problems
        int periods = (divider + period + periodadd) / (period + periodadd);
        if (periods < 0) {
            periods = 0; // can happen if period or periodadd were made smaller
        }
        position = (position + periods) & 0x1F;
        divider -= (period + periodadd) * periods;
    }

    @Override
    public final int getval() {
        return (period == 0) ? 7 : triangle[position];
        //needed to avoid screech when period is zero
    }

    @Override
    public final void setperiod(final int newperiod) {
        period = newperiod;
        if (period == 0) {
            position = 7;
        }
    }

    @Override
    public void setduty(int duty) {
        throw new UnsupportedOperationException("Triangle counter has no duty setting.");
    }

    @Override
    public void setduty(int[] duty) {
        throw new UnsupportedOperationException("Triangle counter has no duty setting.");
    }
}
