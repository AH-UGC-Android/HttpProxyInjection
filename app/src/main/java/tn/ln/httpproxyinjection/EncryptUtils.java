package tn.ln.httpproxyinjection;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * HttpDns 加密、解密工具类
 * Created by yuanxx on 2017/9/22.
 */

public class EncryptUtils {

    /**
     * 加密
     *
     * @param encKey   加密密码
     * @param hostName 需要加密的域名
     * @return
     */
    public static String encrypt(String encKey, String hostName) {
        String encryptedString = "";
        try {
            //初始化密钥
            SecretKeySpec keySpec = new SecretKeySpec(encKey.getBytes("utf-8"), "DES");
            //选择使用DES算法，ECB方式，填充方式为PKCS5Padding
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            //初始化
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            //获取加密后的字符串
            encryptedString = bytesToHex(cipher.doFinal(hostName.getBytes("utf-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedString;
    }

    /**
     * 解密
     *
     * @param encKey          加密密码
     * @param encryptedString 需要解密的内容
     * @return
     */
    public static String decrypt(String encKey, String encryptedString) {
        String decryptedString = "";
        try {
            //初始化密钥
            SecretKeySpec keySpec = new SecretKeySpec(encKey.getBytes("utf-8"), "DES");
            //选择使用DES算法，ECB方式，填充方式为PKCS5Padding
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            //初始化
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            //获取解密后的字符串
            decryptedString = new String(cipher.doFinal(hexToBytes(encryptedString)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedString;
    }

    private static String bytesToHex(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}
