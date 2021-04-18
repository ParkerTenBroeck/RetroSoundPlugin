package retrosoundplugin;

import org.parker.mips.plugin.syscall.SystemCallPlugin;

import retrosoundplugin.sound.APU;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RetroSoundPlugin extends SystemCallPlugin{

    APU apu;

    public RetroSoundPlugin() {

        registerSystemCall(new PRSystemCall("SID_REGISTER_WRITE_BYTE") { //make sure that the name entered here and the name in ExampleSystemCallPlugin match this is for verification
            @Override
            public void handleSystemCall() {
                apu.write(getRegister(4), getRegister(5));
            }
        });
        registerSystemCall(new PRSystemCall("SID_REGISTER_WRITE_WORD") {
            @Override
            public void handleSystemCall() {
                apu.write(getRegister(4)        , (getRegister(5) >> 24) & 0xFF);
                apu.write(getRegister(4) + 1, (getRegister(5) >> 16) & 0xFF);
                apu.write(getRegister(4) + 2, (getRegister(5) >> 8) & 0xFF);
                apu.write(getRegister(4) + 3, getRegister(5) & 0xFF);
                //exampleFrame.opExampleFrame();
                //exampleFrame.setMins(getRegister(4));
            }
        });
        registerSystemCall(new PRSystemCall("SID_REGISTER_READ_BYTE") {
            @Override
            public void handleSystemCall() {
                setRegister(2, apu.read(getRegister(4)));
                //exampleFrame.opExampleFrame();
                //setRegister(2, exampleFrame.getHours());
            }
        });
        registerSystemCall(new PRSystemCall("SID_REGISTER_READ_WORD") {
            @Override
            public void handleSystemCall() {
                //exampleFrame.opExampleFrame();
                //setRegister(2, exampleFrame.getMins());
            }
        });
        registerSystemCall(new PRSystemCall("SID_SET_DMC_STARTING_ADDRESS") {
            @Override
            public void handleSystemCall() {
                apu.setStartAddress(getRegister(4));
            }
        });
        registerSystemCall(new PRSystemCall("SID_RESUME") {
            @Override
            public void handleSystemCall() {
                apu.resume();
            }
        });
        registerSystemCall(new PRSystemCall("SID_STOP") {
            @Override
            public void handleSystemCall() {
                apu.pause();
            }
        });

        //
        registerInternalExamples(new Node("Root",
                new Node[]{
                        new Node("Test 1 Folder",
                                new Node[]{
                                        new Node("File 3", new ResourceActionLoader("exampleProgram1.asm")),
                                        new Node("File 4", new ResourceActionLoader("exampleProgram2.asm"))
                                }),
                        new Node("File 1", new ResourceActionLoader("exampleProgram1.asm")),
                        new Node("File 2", new ResourceActionLoader("exampleProgram2.asm"))
                }));

        //

        /*
        registerGeneralListeners(new Node("Root",
                new Node[]{
                        new Node("Example 1", (ActionListener) (ActionEvent ae) -> {
                            LOGGER.log(Level.INFO, "Example 1");
                        }),
                        new Node("Example SubFolder",
                                new Node[]{
                                        new Node("Example 3", (ActionListener) (ActionEvent ae) -> {
                                            LOGGER.log(Level.INFO,"[Example SubFolder] Example 3");
                                        }),
                                        new Node("Example 4", (ActionListener) (ActionEvent ae) -> {
                                            LOGGER.log(Level.INFO,"[Example SubFolder] Example 4");
                                        })}),
                        new Node("Example 2", (ActionListener) (ActionEvent ae) -> {
                            LOGGER.log(Level.INFO,"Example 2");
                        })}));

         */
    }

    @Override
    public void onLoad() {
        //this can be used for any initiation after the constructor if needed
        apu = new APU(this);
        apu.begin();
    }

    @Override
    public boolean onUnload() {

        apu.destroy();
        apu = null;
        return true;
    }

    public int readMemory(int address){

        System.out.println(address);
        return ((int)getByte(address)) & 0xFF;
        //LOGGER.log(Level.WARNING, address);
    }
}
