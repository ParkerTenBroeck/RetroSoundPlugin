/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 */
package retrosoundplugin.sound;

public interface AudioOutInterface {

    public void outputSample(int sample);

    public void flushFrame(boolean waitIfBufferFull);

    public void pause();

    public void resume();

    public void destroy();

    public boolean bufferHasLessThan(int samples);
}
