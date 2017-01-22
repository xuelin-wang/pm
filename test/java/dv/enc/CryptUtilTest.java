package dv.enc;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by xuelin on 1/22/17.
 */
public class CryptUtilTest {
    @Test
    public void testHexToBytes()
    {
        String[] strs = new String[]{
                "0123456789abcdef", "abcdef0123", "ABCDEFabcdef", "aAbBcCddeEfF"
        };
        for (String str: strs) {
            byte[] bytes = CryptUtil.hexStringToBytes(str);
            String hexStr = CryptUtil.toHex(bytes);
            Assert.assertTrue(str.equalsIgnoreCase(hexStr));
        }

        byte[][] bytesArr = new byte[][]{
                new byte[]{0, 11, 22, 33, 44, 99, 127, -1, -11, -22, -33, -44, -99, -128},
                new byte[]{1, 2, 3, 4, 5, 0, -1, -2, -3, -4, -5},
                new byte[]{12, 23, 34, -128, -127, -126, -125},
                new byte[]{-1, 0, 1},
        };
        for (byte[] bytes: bytesArr) {
            String str = CryptUtil.toHex(bytes);
            byte[] bytes1 = CryptUtil.hexStringToBytes(str);
            Assert.assertEquals(bytes.length, bytes1.length);
            for (int ii = 0; ii < bytes.length; ii++) {
                Assert.assertEquals(bytes[ii], bytes1[ii]);
            }
        }
    }
}
