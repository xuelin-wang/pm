package dv.enc;


import org.junit.Assert;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by xuelin on 1/21/17.
 */
public class AESTest {
    @Test
    public void testAes()
            throws UnsupportedEncodingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
            NoSuchPaddingException, InvalidAlgorithmParameterException
    {
        String[] testKeys = new String[]{"ab12cde456", "124", "bsfwfwfcq", "3243565474746758"};
        String[] texts = new String[]{"", "aaaaa", "bbbbb", "abcdefabcdef", "123ersdfcvsdfsdf f w fwf"};

        for (String key: testKeys) {
            AES aes = new AES(key);
            for (String text: texts) {
                String cypher;
                cypher = aes.encrypt(text);
                Assert.assertEquals(text, aes.decrypt(cypher));
            }
        }
    }
}
