package org.kin.agent.impl;

import org.apache.commons.io.FileUtils;
import org.kin.agent.ClassDecoderDecoder;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * 基于jwt解密
 * @author huangjianqin
 * @date 2022/2/20
 */
public class ClassDecoderDecoderImpl implements ClassDecoderDecoder {
    @Override
    public byte[] decode(byte[] decoderClassfileBuffer) throws Exception {
        byte[] priKeyBytes = FileUtils.readFileToByteArray(new File("key/jwt_rsa.key"));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(priKeyBytes);
        //以pkc8编码
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        int keySize = priKey.getModulus().bitLength() / 8;
        int bufferSize = decoderClassfileBuffer.length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream((bufferSize + keySize - 12) / (keySize - 11) * keySize);
        int left = 0;
        for (int i = 0; i < bufferSize;) {
            left = bufferSize - i;
            if (left > keySize) {
                cipher.update(decoderClassfileBuffer, i, keySize);
                i += keySize;
            } else {
                cipher.update(decoderClassfileBuffer, i, left);
                i += left;
            }
            baos.write(cipher.doFinal());
        }
        baos.close();
        return baos.toByteArray();
    }
}
