/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 *
 * February 15 2021 - Modified by Parker TenBroeck
 */
package retrosoundplugin.sound;

import retrosoundplugin.RetroSoundPlugin;
import retrosoundplugin.ui.Oscilloscope;
import retrosoundplugin.ui.SwingAudioImpl;
import retrosoundplugin.utils;

import java.util.ArrayList;

public class APU {


    public  int samplerate = 44100;
    private  final Timer[] timers = {new SquareTimer(8, 2), new SquareTimer(8, 2),
            new TriangleTimer(), new NoiseTimer()};
    private  double cyclespersample;
    //public final NES nes;
    //CPU cpu;
    //CPURAM cpuram;
    public  int sprdma_count;
    private  int apucycle = 0, remainder = 0;
    private  int[] noiseperiod;
    // different for PAL
    private  long accum = 0;
    private final ArrayList<ExpansionSoundChip> expnSound = new ArrayList<>();
    private  boolean soundFiltering;
    private static final  int[] TNDLOOKUP = initTndLookup(), SQUARELOOKUP = initSquareLookup();
    private  int framectrreload;
    private  int framectrdiv = 7456;
    private  int dckiller = 0;
    private   int lpaccum = 0;
    private  boolean apuintflag = true, statusdmcint = false, statusframeint = false;
    private  int framectr = 0, ctrmode = 4;
    private  final boolean[] lenCtrEnable = {true, true, true, true};
    private  final int[] volume = new int[4];
    //dmc instance variables
    private  int[] dmcperiods;
    private  int dmcrate = 0x36, dmcpos = 0, dmcshiftregister = 0, dmcbuffer = 0,
            dmcvalue = 0, dmcsamplelength = 1, dmcsamplesleft = 0,
            dmcstartaddr = 0xc000, dmcaddr = 0xc000, dmcbitsleft = 8;
    private  boolean dmcsilence = true, dmcirq = false, dmcloop = false, dmcBufferEmpty = true;
    //length ctr instance variables
    private  final int[] lengthctr = {0, 0, 0, 0};
    private static final  int[] lenctrload = {10, 254, 20, 2, 40, 4, 80, 6,
            160, 8, 60, 10, 14, 12, 26, 14, 12, 16, 24, 18, 48, 20, 96, 22,
            192, 24, 72, 26, 16, 28, 32, 30};
    private  final boolean[] lenctrHalt = {true, true, true, true};
    //linear counter instance vars
    private  int linearctr = 0;
    private  int linctrreload = 0;
    private  boolean linctrflag = false;
    //instance variables for envelope units
    private  final int[] envelopeValue = {15, 15, 15, 15};
    private  final int[] envelopeCounter = {0, 0, 0, 0};
    private  final int[] envelopePos = {0, 0, 0, 0};
    private  final boolean[] envConstVolume = {true, true, true, true};
    private  final boolean[] envelopeStartFlag = {false, false, false, false};
    //instance variables for sweep unit
    private  final boolean[] sweepenable = {false, false},
            sweepnegate = {false, false},
            sweepsilence = {false, false},
            sweepreload = {false, false};
    private  final int[] sweepperiod = {15, 15}, sweepshift = {0, 0}, sweeppos = {0, 0};
    private  int cyclesperframe;
    private  AudioOutInterface ai;


    final private static int[][] DUTYLOOKUP = {
            {0, 1, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 0, 0, 0, 0},
            {0, 1, 1, 1, 1, 0, 0, 0},
            {1, 0, 0, 1, 1, 1, 1, 1}
    };

    private  void setvolumes() {
        volume[0] = ((lengthctr[0] <= 0 || sweepsilence[0]) ? 0 : (((envConstVolume[0]) ? envelopeValue[0] : envelopeCounter[0])));
        volume[1] = ((lengthctr[1] <= 0 || sweepsilence[1]) ? 0 : (((envConstVolume[1]) ? envelopeValue[1] : envelopeCounter[1])));
        volume[3] = ((lengthctr[3] <= 0) ? 0 : ((envConstVolume[3]) ? envelopeValue[3] : envelopeCounter[3]));
        //System.err.println("setvolumes " + volume[1]);
    }

    public void addExpnSound(ExpansionSoundChip chip) {
        expnSound.add(chip);
    }

    public void destroy() {
        ai.destroy();
    }

    public void pause() {
        ai.pause();
    }

    public void resume() {
        ai.resume();
    }

    private int getOutputLevel() {
        int vol;
        vol = SQUARELOOKUP[volume[0] * timers[0].getval()
                + volume[1] * timers[1].getval()];
        vol += TNDLOOKUP[3 * timers[2].getval()
                + 2 * volume[3] * timers[3].getval()
                + dmcvalue];
        if (!expnSound.isEmpty()) {
            vol *= 0.8;
            for (ExpansionSoundChip c : expnSound) {
                vol += c.getval();
            }
        }
        return vol; //as usual, lack of unsigned types causes unending pain.
    }

    private static int[] initTndLookup() {
        int[] lookup = new int[203];
        for (int i = 0; i < lookup.length; ++i) {
            lookup[i] = (int) ((163.67 / (24329.0 / i + 100)) * 49151);
        }
        return lookup;
    }

    private static int[] initSquareLookup() {
        //fill square, triangle volume lookup tables
        int[] lookup = new int[31];
        for (int i = 0; i < lookup.length; ++i) {
            lookup[i] = (int) ((95.52 / (8128.0 / i + 100)) * 49151);
        }
        return lookup;
    }

    public boolean bufferHasLessThan(int samples) {
        return ai.bufferHasLessThan(samples);
    }

    public final int read(final int addr) {
        switch (addr) {
            case 0x15:
                //returns channel status
                //for future ref: NEED to put those ternary operators in parentheses!
                //otherwise order of operations does the wrong thing.
                final int returnval = ((lengthctr[0] > 0) ? 1 : 0)
                        | ((lengthctr[1] > 0) ? 2 : 0)
                        | ((lengthctr[2] > 0) ? 4 : 0)
                        | ((lengthctr[3] > 0) ? 8 : 0)
                        | ((dmcsamplesleft > 0) ? 16 : 0)
                        | (statusframeint ? 64 : 0)
                        | (statusdmcint ? 128 : 0);
                if (statusframeint) {
                    //System.err.println("Frame interrupt ack at " + cpu.cycles);
                    statusframeint = false;
                }

                //System.err.println("*" + utils.hex(returnval));
                return returnval;
            case 0x16:
                //nes.getcontroller1().strobe();
                //return nes.getcontroller1().getbyte() | 0x40;
            case 0x17:
                //nes.getcontroller2().strobe();
                //return nes.getcontroller2().getbyte() | 0x40;
            default:
                return 0x40; //open bus
        }
    }

    private  int highpass_filter(int sample) {
        //for killing the dc in the signal
        sample += dckiller;
        dckiller -= sample >> 8;//the actual high pass part
        dckiller += (sample > 0 ? -1 : 1);//guarantees the signal decays to exactly zero
        return sample;
    }

    private  int lowpass_filter(int sample) {
        sample += lpaccum;
        lpaccum -= sample * 0.9;
        return lpaccum;
    }

    private RetroSoundPlugin retroSoundPlugin;
    public APU(RetroSoundPlugin retroSoundPlugin){
        this.retroSoundPlugin = retroSoundPlugin;
        soundFiltering = true;
        samplerate = 44100;

        dmcperiods = new int[]{428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54};
        noiseperiod = new int[]{4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068};
        framectrreload = 7456;
        cyclespersample = 1789773.0 / samplerate;
        cyclesperframe = 29781;

        ai = new Oscilloscope(new SwingAudioImpl( samplerate));

    }

    public void begin(){
        new Thread(){

            public synchronized void run() {
                while(true){
                    finishframe();
                    try {
                        Thread.sleep(1000/68); //idrk why 60 doesnt work but this seems fine
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    public synchronized  final void finishframe() {
        updateto(cyclesperframe);
        apucycle = 0;
        ai.flushFrame(false);
    }

    public synchronized final void updateto(final int cpucycle) {
        //still have to run this even if sound is disabled, some games rely on DMC IRQ etc.
        if (soundFiltering) {
            //linear sampling code
            //should really be a FIR filter + decimator instead
            //but I don't have the DSP experience to design something like that
            //that would be fast enough to work / not require calculating every sample
            //this works well enough at eliminating aliasing anyway.
            while (apucycle < cpucycle) {
                ++remainder;
                clockdmc();
                if (--framectrdiv <= 0) {
                    framectrdiv = framectrreload;
                    clockframecounter();
                }
                timers[0].clock();
                timers[1].clock();
                if (lengthctr[2] > 0 && linearctr > 0) {
                    timers[2].clock();
                }
                timers[3].clock();
                if (!expnSound.isEmpty()) {
                    for (ExpansionSoundChip c : expnSound) {
                        c.clock(1);
                    }
                }
                accum += getOutputLevel();

                if ((apucycle % cyclespersample) < 1) {
                    //not quite right - there's a non-integer # cycles per sample.
                    ai.outputSample(lowpass_filter(highpass_filter((int) (accum / remainder))));
                    remainder = 0;
                    accum = 0;
                }
                ++apucycle;
            }
        } else {
            //point sampling code
            while (apucycle < cpucycle) {
                ++remainder;
                clockdmc();
                if (--framectrdiv <= 0) {
                    framectrdiv = framectrreload;
                    clockframecounter();
                }
                if ((apucycle % cyclespersample) < 1) {
                    //not quite right - there's a non-integer # cycles per sample.
                    timers[0].clock(remainder);
                    timers[1].clock(remainder);
                    if (lengthctr[2] > 0 && linearctr > 0) {
                        timers[2].clock(remainder);
                    }
                    timers[3].clock(remainder);
                    int mixvol = getOutputLevel();
                    if (!expnSound.isEmpty()) {
                        for (ExpansionSoundChip c : expnSound) {
                            c.clock(remainder);
                        }
                    }
                    remainder = 0;
                    ai.outputSample(lowpass_filter(highpass_filter(mixvol)));
                }
                ++apucycle;
            }
        }
    }

    private  void clockframecounter() {
        //System.err.println("frame ctr clock " + framectr + ' ' + cpu.cycles);
        //should be ~4x a frame, 240 Hz
        //but the problem is this isn't exactly related to the video signal,
        //it's a completely separate timer, so the phase can shift in relation to the
        //video signal. also in the current implementation APU interrupts can only be fired when
        //an APU register is written/read from, or @ end of frame. So both of those need work
        if ((ctrmode == 4)
                || (ctrmode == 5 && (framectr != 3))) {
            setenvelope();
            setlinctr();
        }
        if ((ctrmode == 4 && (framectr == 1 || framectr == 3))
                || (ctrmode == 5 && (framectr == 1 || framectr == 4))) {
            setlength();
            setsweep();
        }
        if (!apuintflag && (framectr == 3) && (ctrmode == 4) && !statusframeint) {
            //++cpu.interrupt;
            //System.err.println("frame interrupt set at " + cpu.cycles);
            statusframeint = true;

        }
        ++framectr;
        framectr %= ctrmode;
        setvolumes();
    }


    public final synchronized void write(final int reg, final int data) {
        //This is how values written to any of the APU's memory
        //mapped registers change the state of the system.
        //System.err.println("Wrote " + utils.hex(data) + " to " + utils.hex(reg) + " @ cycle " + cpu.cycles);
        switch (reg) {
            case 0x0:
                //length counter 1 halt
                lenctrHalt[0] = ((data & (utils.BIT5)) != 0);
                // pulse 1 duty cycle
                timers[0].setduty(DUTYLOOKUP[data >> 6]);
                // and envelope
                envConstVolume[0] = ((data & (utils.BIT4)) != 0);
                envelopeValue[0] = data & 15;
                //setvolumes();
                break;
            case 0x1:
                //pulse 1 sweep setup
                //sweep enabled
                sweepenable[0] = ((data & (utils.BIT7)) != 0);
                //sweep divider period
                sweepperiod[0] = (data >> 4) & 7;
                //sweep negate flag
                sweepnegate[0] = ((data & (utils.BIT3)) != 0);
                //sweep shift count
                sweepshift[0] = (data & 7);
                sweepreload[0] = true;
                break;
            case 0x2:
                // pulse 1 timer low bit
                timers[0].setperiod((timers[0].getperiod() & 0xfe00) + (data << 1));
                break;
            case 0x3:
                // length counter load, timer 1 high bits
                if (lenCtrEnable[0]) {
                    lengthctr[0] = lenctrload[data >> 3];
                }
                timers[0].setperiod((timers[0].getperiod() & 0x1ff) + ((data & 7) << 9));
                // sequencer restarted
                timers[0].reset();
                //envelope also restarted
                envelopeStartFlag[0] = true;
                break;
            case 0x4:
                //length counter 2 halt
                lenctrHalt[1] = ((data & (utils.BIT5)) != 0);
                // pulse 2 duty cycle
                timers[1].setduty(DUTYLOOKUP[data >> 6]);
                // and envelope
                envConstVolume[1] = ((data & (utils.BIT4)) != 0);
                envelopeValue[1] = data & 15;
                //setvolumes();
                break;
            case 0x5:
                //pulse 2 sweep setup
                //sweep enabled
                sweepenable[1] = ((data & (utils.BIT7)) != 0);
                //sweep divider period
                sweepperiod[1] = (data >> 4) & 7;
                //sweep negate flag
                sweepnegate[1] = ((data & (utils.BIT3)) != 0);
                //sweep shift count
                sweepshift[1] = (data & 7);
                sweepreload[1] = true;
                break;
            case 0x6:
                // pulse 2 timer low bit
                timers[1].setperiod((timers[1].getperiod() & 0xfe00) + (data << 1));
                break;
            case 0x7:
                if (lenCtrEnable[1]) {
                    lengthctr[1] = lenctrload[data >> 3];
                }
                timers[1].setperiod((timers[1].getperiod() & 0x1ff) + ((data & 7) << 9));
                // sequencer restarted
                timers[1].reset();
                //envelope also restarted
                envelopeStartFlag[1] = true;
                break;
            case 0x8:
                //triangle linear counter load
                linctrreload = data & 0x7f;
                //and length counter halt
                lenctrHalt[2] = ((data & (utils.BIT7)) != 0);
                break;
            case 0x9:
                break;
            case 0xA:
                // triangle low bits of timer
                timers[2].setperiod((((timers[2].getperiod() * 1) & 0xff00) + data));
                break;
            case 0xB:
                // triangle length counter load
                // and high bits of timer
                if (lenCtrEnable[2]) {
                    lengthctr[2] = lenctrload[data >> 3];
                }
                timers[2].setperiod((((timers[2].getperiod() * 1) & 0xff) + ((data & 7) << 8)));
                linctrflag = true;
                break;
            case 0xC:
                //noise halt and envelope
                lenctrHalt[3] = ((data & (utils.BIT5)) != 0);
                envConstVolume[3] = ((data & (utils.BIT4)) != 0);
                envelopeValue[3] = data & 0xf;
                //setvolumes();
                break;
            case 0xD:
                break;
            case 0xE:
                timers[3].setduty(((data & (utils.BIT7)) != 0) ? 6 : 1);
                timers[3].setperiod(noiseperiod[data & 15]);
                break;
            case 0xF:
                //noise length counter load, envelope restart
                if (lenCtrEnable[3]) {
                    lengthctr[3] = lenctrload[data >> 3];
                }
                envelopeStartFlag[3] = true;
                break;
            case 0x10:
                dmcirq = ((data & (utils.BIT7)) != 0);
                dmcloop = ((data & (utils.BIT6)) != 0);
                dmcrate = dmcperiods[data & 0xf];
                if (!dmcirq && statusdmcint) {
                    statusdmcint = false;
                }
                //System.err.println(dmcirq ? "dmc irq on" : "dmc irq off");
                break;
            case 0x11:
                dmcvalue = data & 0x7f;
                break;
            case 0x12:
                dmcstartaddr = (data << 6) + dmcstartaddr;
                break;
            case 0x13:
                dmcsamplelength = (data << 4) + 1;
                break;
            case 0x14:
                //sprite dma
                /*
                for (int i = 0; i < 256; ++i) {
                    cpuram.write(0x2004, cpuram.read((data << 8) + i));
                }
                //account for time stolen from cpu
                sprdma_count = 2;
                 */
                break;
            case 0x15:
                //status register
                // counter enable(silence channel when bit is off)
                for (int i = 0; i < 4; ++i) {
                    lenCtrEnable[i] = ((data & (1 << i)) != 0);
                    //THIS was the channels not cutting off bug! If you toggle a channel's
                    //status on and off very quickly then the length counter should
                    //IMMEDIATELY be forced to zero.
                    if (!lenCtrEnable[i]) {
                        lengthctr[i] = 0;
                    }
                }
                if (((data & (utils.BIT4)) != 0)) {
                    if (dmcsamplesleft == 0) {
                        restartdmc();
                    }
                } else {
                    dmcsamplesleft = 0;
                    dmcsilence = true;
                }
                if (statusdmcint) {
                    statusdmcint = false;
                }
                break;
            case 0x16:
                // latch controller 1 + 2
                //nes.getcontroller1().output(((data & (utils.BIT0)) != 0));
                //nes.getcontroller2().output(((data & (utils.BIT0)) != 0));
                break;
            case 0x17:
                ctrmode = ((data & (utils.BIT7)) != 0) ? 5 : 4;
                //System.err.println("reset " + ctrmode + ' ' + cpu.cycles);
                apuintflag = ((data & (utils.BIT6)) != 0);
                //set is no interrupt, clear is an interrupt
                framectr = 0;
                framectrdiv = framectrreload + 8; //Why +8?
                if (apuintflag && statusframeint) {
                    statusframeint = false;
                    //System.err.println("Frame interrupt off at " + cpu.cycles);
                }
                if (ctrmode == 5) {
                    //everything frame counter runs is clocked no matter what
                    setenvelope();
                    setlinctr();
                    setlength();
                    setsweep();
                }
                break;
            default:
                break;
        }
    }

    private  void restartdmc() {
        dmcaddr = dmcstartaddr;
        dmcsamplesleft = dmcsamplelength;
        dmcsilence = false;
    }

    private  void setlength() {
        for (int i = 0; i < 4; ++i) {
            if (!lenctrHalt[i] && lengthctr[i] > 0) {
                --lengthctr[i];
                if (lengthctr[i] == 0) {
                    setvolumes();
                }
            }
        }
    }

    private  void setlinctr() {
        if (linctrflag) {
            linearctr = linctrreload;
        } else if (linearctr > 0) {
            --linearctr;
        }
        if (!lenctrHalt[2]) {
            linctrflag = false;
        }
    }

    private  void setenvelope() {
        //System.err.println("envelope");
        for (int i = 0; i < 4; ++i) {
            if (envelopeStartFlag[i]) {
                envelopeStartFlag[i] = false;
                envelopePos[i] = envelopeValue[i] + 1;
                envelopeCounter[i] = 15;
            } else {
                --envelopePos[i];
            }
            if (envelopePos[i] <= 0) {
                envelopePos[i] = envelopeValue[i] + 1;
                if (envelopeCounter[i] > 0) {
                    --envelopeCounter[i];
                } else if (lenctrHalt[i] && envelopeCounter[i] <= 0) {
                    envelopeCounter[i] = 15;
                }
            }
        }
    }

    private  void setsweep() {
        //System.err.println("sweep");
        for (int i = 0; i < 2; ++i) {
            sweepsilence[i] = false;
            if (sweepreload[i]) {
                sweepreload[i] = false;
                sweeppos[i] = sweepperiod[i];
            }
            ++sweeppos[i];
            final int rawperiod = (timers[i].getperiod() >> 1);
            int shiftedperiod = (rawperiod >> sweepshift[i]);
            if (sweepnegate[i]) {
                //invert bits of period
                //add 1 on second channel only
                shiftedperiod = -shiftedperiod + i;
            }
            shiftedperiod += rawperiod;
            if ((rawperiod < 8) || shiftedperiod > 0x7ff) {
                // silence channel
                sweepsilence[i] = true;
            } else if (sweepenable[i] && (sweepshift[i] != 0) && lengthctr[i] > 0
                    && sweeppos[i] > sweepperiod[i]) {
                sweeppos[i] = 0;
                timers[i].setperiod(shiftedperiod << 1);
            }
        }
    }

    private  void clockdmc() {
        if (dmcBufferEmpty && dmcsamplesleft > 0) {
            dmcfillbuffer();
        }
        dmcpos = (dmcpos + 1) % dmcrate;
        if (dmcpos == 0) {
            if (dmcbitsleft <= 0) {
                dmcbitsleft = 8;
                if (dmcBufferEmpty) {
                    dmcsilence = true;
                } else {
                    dmcsilence = false;
                    dmcshiftregister = dmcbuffer;
                    dmcBufferEmpty = true;
                }
            }
            if (!dmcsilence) {
                dmcvalue += (((dmcshiftregister & (utils.BIT0)) != 0) ? 2 : -2);
                //DMC output register doesn't wrap around
                if (dmcvalue > 0x7f) {
                    dmcvalue = 0x7f;
                }
                if (dmcvalue < 0) {
                    dmcvalue = 0;
                }
                dmcshiftregister >>= 1;
                --dmcbitsleft;

            }
        }
    }

    public void setStartAddress(int address){
        dmcstartaddr = address;
        System.out.println(address);
    }

    private  void dmcfillbuffer() {
        if (dmcsamplesleft > 0) {
            dmcbuffer = retroSoundPlugin.readMemory(dmcaddr++);//FakeRam.read(dmcaddr++);
            dmcBufferEmpty = false;
            //cpu.stealcycles(4);

            //DPCM Does steal cpu cycles - this should actually vary between 1-4
            //can't do this properly without a cycle accurate cpu/ppu
            //if (dmcaddr > 0xffff) {
            //    dmcaddr = 0x8000;
            //}
            --dmcsamplesleft;
            if (dmcsamplesleft == 0) {
                if (dmcloop) {
                    restartdmc();
                } else if (dmcirq && !statusdmcint) {
                    //this is supposed to fire after we've just READ the
                    //last byte, not when coming back AFTER reading the last byte
                    //and finding that there are no more bytes left to read.
                    //that meant all dmc timing was too long.
                    statusdmcint = true;
                    //System.err.println("dmc irq fire");
                }

            }
        } else {
            dmcsilence = true;
        }
    }
}


