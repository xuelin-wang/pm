package dv.enc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AES {
  private final SecretKey _key;

  public AES(String key) throws UnsupportedEncodingException, NoSuchAlgorithmException {
      byte[] keyBytes = CryptUtil.toHash(key, 16);
      _key = new SecretKeySpec(keyBytes, "AES");
  }

  public String encrypt(String msg)
          throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
          UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, _key, new IvParameterSpec(new byte[16]));
    return CryptUtil.toHex(cipher.doFinal(msg.getBytes("UTF-8")));
  }

  public String decrypt(String cypherText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, _key,  new IvParameterSpec(new byte[16]));
    return new String(cipher.doFinal(CryptUtil.hexStringToBytes(cypherText)));
  }
}
