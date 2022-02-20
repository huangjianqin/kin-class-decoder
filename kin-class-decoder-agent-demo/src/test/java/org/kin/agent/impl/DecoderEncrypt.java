package org.kin.agent.impl;

import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * 加密ClassDecoder class文件并输出到指定目录
 * @author huangjianqin
 * @date 2022/2/20
 */
public class DecoderEncrypt {
    public static void main(String[] args) throws Exception {
        byte[] contentBytes = FileUtils.readFileToByteArray(new File("kin-class-decoder-agent-demo/target/classes/org/kin/agent/impl/ClassDecoderImpl.class"));
        System.out.println(Arrays.toString(contentBytes));
        System.out.println(new String(contentBytes, StandardCharsets.UTF_8));

        byte[] encryptBytes = encrypt(contentBytes);
        System.out.println(Arrays.toString(encryptBytes));

//        byte[] decryptBytes = decrypt(encryptBytes);
//        System.out.println(Arrays.toString(decryptBytes));
//        System.out.println(Arrays.equals(contentBytes, decryptBytes));

        File outFile = new File("kin-class-decoder-agent-demo/src/main/resources/META-INF/classDecoder/org.kin.agent.impl.ClassDecoderImpl.class");
        if (outFile.exists()) {
            outFile.delete();
        }
        outFile.getParentFile().mkdirs();
        outFile.createNewFile();
        FileUtils.writeByteArrayToFile(outFile, encryptBytes);
    }

    /**
     * 利用jwt public key加密
     */
    private static byte[] encrypt(byte[] contentBytes) throws Exception {
        byte[] pubKeyBytes = FileUtils.readFileToByteArray(new File("key/jwt_rsa.pub"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyBytes);
        //以x509编码
        //rsa public key 只能以X509EncodedKeySpec和DSAPublicKeySpec编码
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        int keySize = pubKey.getModulus().bitLength() / 8 - 11;
        ByteArrayOutputStream baos = new ByteArrayOutputStream((contentBytes.length + keySize - 1) / keySize * (keySize + 11));
        int left;
        for (int i = 0; i < contentBytes.length;) {
            left = contentBytes.length - i;
            if (left > keySize) {
                cipher.update(contentBytes, i, keySize);
                i += keySize;
            } else {
                cipher.update(contentBytes, i, left);
                i += left;
            }
            baos.write(cipher.doFinal());
        }

        baos.close();
        return baos.toByteArray();
    }

    /**
     * 利用jwt private key加密
     */
    private static byte[] decrypt(byte[] encryptBytes) throws Exception {
        byte[] priKeyBytes = FileUtils.readFileToByteArray(new File("key/jwt_rsa.key"));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(priKeyBytes);
        //以pkc8编码
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        int keySize = priKey.getModulus().bitLength() / 8;
        ByteArrayOutputStream baos = new ByteArrayOutputStream((encryptBytes.length + keySize - 12) / (keySize - 11) * keySize);
        int left = 0;
        for (int i = 0; i < encryptBytes.length;) {
            left = encryptBytes.length - i;
            if (left > keySize) {
                cipher.update(encryptBytes, i, keySize);
                i += keySize;
            } else {
                cipher.update(encryptBytes, i, left);
                i += left;
            }
            baos.write(cipher.doFinal());
        }
        baos.close();
        return baos.toByteArray();
    }
}
