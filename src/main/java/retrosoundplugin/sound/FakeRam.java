package retrosoundplugin.sound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class FakeRam {

    private static byte[] mem = new byte[0xFFFF];

    static{
        String why = "298ED52C8402FEFF830B549244406DF7DDBBFF4DAD9A2A25DD0A93004000A454556BDB05F6DFFAFF97FD5F4BED4AAB220801024088204AFF0500ADEEBEFFEF5FF5FF7F55A55422020110046900005655B5AA5ABFBBBFBFBFF2FFFFAA5255A58A201088A010222411DD0540ADAABBED6EF7FD76B7DDDD766DB56A1201A22AB5000241884492A4AA5655ABAA6EEFDEEFEDF7F7EEFB4B55A5AA4A55A5AA4A25042108920842041189A45655ADAA5A55B5AA6ADFDDDFBB7677B7DDB6DD5A6BB5AA4A55954A4A9224894424A2442249929494AA4A4A5595D65A6B6D5BDBB66DDB6ADBDAB6B6B5D6AA5555AD5255A94A4A292525A5A4A854A55456492955AA54AA5255955A55B56AB556ABB6B5DAAAD66AB556AD5A5555555555AA964A4A959292929292";
        int index = 0;
        //System.out.println(Arrays.toString(getParts(why, 2).toArray()));
        for(String hex: getParts(why, 2)){
            mem[index + 0xC000] = (byte) Integer.parseInt(hex, 16);

           // System.out.println(mem[index + 0xC000]);
            index ++;
        }
        File file = new File("C:/Users/parke/OneDrive/Documents/MIPS/Projects/bad-apple/bad-apple-dmc-sample.bin");
        try {
            OutputStream out = new FileOutputStream(file);
            out.write(mem, 0xC000, getParts(why, 2).size());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static List<String> getParts(String string, int partitionSize) {
        List<String> parts = new ArrayList<String>();
        int len = string.length();
        for (int i=0; i<len; i+=partitionSize)
        {
            parts.add(string.substring(i, Math.min(len, i + partitionSize)));
        }
        return parts;
    }

    public static int read(int address){
        //System.out.println(Integer.toHexString(address) + ": " + (((int)mem[address]) & 0xFF));
        return (((int)mem[address]) & 0xFF);

    }
}
