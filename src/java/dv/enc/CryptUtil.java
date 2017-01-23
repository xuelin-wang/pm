package dv.enc;

// import com.google.common.base.Preconditions;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by xuelin on 1/21/17.
 */
public class CryptUtil {
    public static byte[] toHash256(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] keyBytes = str.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(keyBytes);
    }

    public static byte[] hexStringToBytes(String hexStr)
    {
//        Preconditions.checkArgument(hexStr.length() % 2 == 0);

        int len = hexStr.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexStr.charAt(i), 16) << 4)
                    + Character.digit(hexStr.charAt(i+1), 16));
        }
        return data;
    }

    public static String toHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte bb: bytes) {
            sb.append(String.format("%02x", bb));
        }
        return sb.toString();
    }
}
