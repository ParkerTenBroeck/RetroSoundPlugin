package retrosoundplugin;

import retrosoundplugin.sound.APU;

import java.io.*;
import java.util.HashMap;

public class Main {

    public static String[] notes1s = {
            "C-3",
            "",
            "stop",
            "D#3",
            "",
            "stop",
            "F-3",
            "",
            "G-3",
            "",
            "",
            "",
            "D#3",
            "stop",
            "D-3",
            "",
            "C-3",
            "",
            "stop",
            "C-3",
            "",
            "stop",
            "C-3",
            "",
            "stop",
            "",
            "A#2",
            "",
            "stop",
            "B-2",
            "",
            "stop",
            "C-3",
            "",
            "stop",
            "D#3",
            "",
            "stop",
            "F-3",
            "",
            "G-3",
            "",
            "",
            "",
            "D#3",
            "stop",
            "F-3",
            "",
            "G-3",
            "",
            "stop",
            "F-3",
            "",
            "stop",
            "A#3",
            "",
            "stop",
            "",
            "A#3",
            "",
            "stop",
            "B-3",
            "",
            "stop"
    };

    public static String notes2s[] = {
            "C-1",
            "stop",
            "",
            "C-2",
            "stop",
            "",
            "C-1",
            "",
            "stop",
            "",
            "C-2",
            "C-1",
            "stop",
            "",
            "D#1",
            "stop",
            "F-1",
            "",
            "stop",
            "F-1",
            "",
            "stop",
            "F-1",
            "",
            "D#1",
            "F-1",
            "F-2",
            "D#1",
            "",
            "stop",
            "D-1",
            "",
            "C-1",
            "stop",
            "",
            "C-2",
            "stop",
            "",
            "C-1",
            "",
            "",
            "stop",
            "C-2",
            "C-1",
            "stop",
            "",
            "D#1",
            "stop",
            "F-1",
            "",
            "stop",
            "F-1",
            "",
            "stop",
            "F-1",
            "",
            "D#1",
            "F-1",
            "F-2",
            "D#1",
            "stop",
            "D-1",
            "",
            ""
    };

    public static HashMap<String, Integer> squareNoteMap = new HashMap<>();

    public static void main(String[] args){

        squareNoteMap.put("C-1", 0xAD06);
        squareNoteMap.put("C#1", 0x4D06);
        squareNoteMap.put("D-1", 0xF305);
        squareNoteMap.put("D#1", 0x9D05);
        squareNoteMap.put("E-1", 0x4C05);
        squareNoteMap.put("F-1", 0x0005);
        squareNoteMap.put("F#1", 0xB804);
        squareNoteMap.put("G-1", 0x7404);
        squareNoteMap.put("G#1", 0x3404);
        squareNoteMap.put("A-1", 0xF803);
        squareNoteMap.put("A#1", 0xBf03);
        squareNoteMap.put("B-1", 0x8903);

        squareNoteMap.put("C-2", 0x5603);
        squareNoteMap.put("C#2", 0x2603);
        squareNoteMap.put("D-2", 0xF902);
        squareNoteMap.put("D#2", 0xCE02);
        squareNoteMap.put("E-2", 0xA602);
        squareNoteMap.put("F-2", 0x8002);
        squareNoteMap.put("F#2", 0x5C02);
        squareNoteMap.put("G-2", 0x3A02);
        squareNoteMap.put("G#2", 0x1A02);
        squareNoteMap.put("A-2", 0xFB01);
        squareNoteMap.put("A#2", 0xDF01);
        squareNoteMap.put("B-2", 0xC401);

        squareNoteMap.put("C-3", 0xAB01);
        squareNoteMap.put("C#3", 0x9301);
        squareNoteMap.put("D-3", 0x7C01);
        squareNoteMap.put("D#3", 0x6701);
        squareNoteMap.put("E-3", 0x5201);
        squareNoteMap.put("F-3", 0x3F01);
        squareNoteMap.put("F#3", 0x2D01);
        squareNoteMap.put("G-3", 0x1C01);
        squareNoteMap.put("G#3", 0x0C01);
        squareNoteMap.put("A-3", 0xFD00);
        squareNoteMap.put("A#3", 0xEF00);
        squareNoteMap.put("B-3", 0xE100);

        APU apu = new APU(null);

        apu.begin();

        for(int i = 0; i < 5; i ++){
            //apu.write(i * 4, 0x30);
            //apu.write(i * 4 + 1, 0x08);
            //apu.write(i * 4 + 2, 0x00);
            //apu.write(i * 4 + 3, 0x00);
        }

        //apu.write(0x15, 0x0f);
        //apu.write(0x17, 0x40);


        File file = new File("C:/GitHub/Sound/test.bin");
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] newData = new byte[0x18 * 2];
        while(true){
            try {
                if (in.available() <= 0) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                for(int i = 0; i < newData.length ; i ++){
                    newData[i] = (byte) in.read();
                }

                for(int i = 0; i < 0x18; i ++){

                    if(newData[i * 2] > 0) {
                        apu.write(i, ((int) newData[i * 2 + 1]) & 0xFF);
                    }
                }

                try {
                    long start = System.nanoTime();
                    Thread.sleep(16);

                } catch (Exception e) {

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for(int p = 0; p < Math.pow(2, 11) ; p ++) {

            for(int i = 0; i < notes1s.length; i ++) {


                if(notes1s[i].equals("")){

                }else if(notes1s[i].equals("stop")){
                    apu.write(0x00, 0x30); //mute

                } else {

                    int val = squareNoteMap.get(notes1s[i]);

                    apu.write(0x02, (val >> 8) & 0xFF);
                    apu.write(0x03, val & 0xFF);
                    apu.write(0x00, 0x3F); //unmute
                }

                if(notes2s[i].equals("")){

                }else if(notes2s[i].equals("stop")){
                    apu.write(0x04, 0x30); //mute
                    apu.write(0x08, 0x88); //mute

                } else {

                    int val = squareNoteMap.get(notes2s[i]);

                    apu.write(0x06, (val >> 8) & 0xFF);
                    apu.write(0x07, val & 0xFF);
                    apu.write(0x04, 0x3F); //unmute

                    apu.write(0x0A, (val >> 8) & 0xFF);
                    apu.write(0x0B, val & 0xFF);
                    apu.write(0x08, 0x81); //unmute
                }

                try {
                    Thread.sleep(100);
                } catch (Exception e) {

                }
            }
        }


    }
}
