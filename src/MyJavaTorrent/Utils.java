package MyJavaTorrent;


import java.util.*;
import java.lang.*;
import java.io.*;
import java.nio.*;
import java.security.*;

public class Utils {
    public static byte[] hash(ByteBuffer hashThis) {
        try {
            byte[] hash = new byte[20];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(hashThis);
            return md.digest();
        }
        catch (NoSuchAlgorithmException e) {
        	 System.err.println("SHA-1 algorithm is not available.");
        }
        return null;
    }
    public static byte[] hash(byte[] hashThis) {
        try {
            byte[] hash = new byte[20];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            hash = md.digest(hashThis);
            return hash;
        } 
        catch (NoSuchAlgorithmException nsae) {
            System.err.println("SHA-1 algorithm is not available...");
        }
        return null;
    }
    public static boolean[] byteArray2BitArray(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length*8];
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 -(i % 8)))) > 0)
                bits[i] = true;
        }
        return bits;
    }
    //Convert byte array to a URL String
    //Copy from others, note that some special characters should be added with %
    public static String byteArrayToURLString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0)
            return null;

        String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                          "A", "B", "C", "D", "E", "F"};
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            // First check to see if we need ASCII or HEX
            if ((in[i] >= '0' && in[i] <= '9')
                || (in[i] >= 'a' && in[i] <= 'z')
                || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$'
                || in[i] == '-' || in[i] == '_' || in[i] == '.'
                || in[i] == '!') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0); // Strip off high nibble
                ch = (byte) (ch >>> 4); // shift the bits down
                ch = (byte) (ch & 0x0F); // must do this is high order bit is
                // on!
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                ch = (byte) (in[i] & 0x0F); // Strip off low nibble
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                i++;
            }
        }

        String rslt = new String(out);

        return rslt;

    }
    
    public static String byteArrayToByteString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0)
            return null;

        String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                          "A", "B", "C", "D", "E", "F"};
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0); // Strip off high nibble
            ch = (byte) (ch >>> 4); // shift the bits down
            ch = (byte) (ch & 0x0F); // must do this is high order bit is on!
            out.append(pseudo[(int) ch]); // convert the nibble to a String
            // Character
            ch = (byte) (in[i] & 0x0F); // Strip off low nibble
            out.append(pseudo[(int) ch]); // convert the nibble to a String
            // Character
            i++;
        }

        String rslt = new String(out);

        return rslt;
    }
    public static int byteArrayToInt(byte[] b) {
        if(b.length == 4)
            return b[0] << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 |
                    (b[3] & 0xff);
        else if(b.length == 2)
            return 0x00 << 24 | 0x00 << 16 | (b[0] & 0xff) << 8 | (b[1] & 0xff);

        return 0;
    }
    public static byte[] concat(byte[] b1, byte[] b2) {
        byte[] b3 = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, b3, 0, b1.length);
        System.arraycopy(b2, 0, b3, b1.length, b2.length);
        return b3;
    }
    
    public static byte[] concat(byte[] b1, byte b2) {
        byte[] b3 = new byte[b1.length + 1];
        byte[] temp = new byte[] {b2};
        System.arraycopy(b1, 0, b3, 0, b1.length);
        System.arraycopy(temp, 0, b3, b1.length, 1);
        return b3;
    }
    
    public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }
    
    public static boolean bytesCompare(byte[] a, byte[] b){
        if(a.length != b.length)
            return false;
        for(int i = 0; i < a.length; i++)
            if(a[i] != b[i])
                return false;
        return true;
    }
    
    public static int byteToUnsignedInt(byte b){
        return 0x00 << 24 | b & 0xff;
    }
    
    public static byte[] makeArray(byte[] b, int offset, int length){
        byte[] arr = new byte[length];
        for(int i = offset; i < offset + length; i++)
            arr[i-offset] = b[i];
        return arr;
    }
    
    public static byte[] generateID() {
        byte[] id = new byte[12];
        Random r = new Random(System.currentTimeMillis());
        r.nextBytes(id);
        return Utils.concat("-JT0001-".getBytes(),id);
    }
}
