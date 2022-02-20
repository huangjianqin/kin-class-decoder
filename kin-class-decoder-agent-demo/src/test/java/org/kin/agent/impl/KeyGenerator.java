package org.kin.agent.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * 生成jwt密钥
 *
 * @author huangjianqin
 * @date 2022/2/20
 */
public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        String dir = "key/";
        File publicKeyFile = new File(dir, "jwt_rsa.pub");
        if (publicKeyFile.exists()) {
            //公钥存在就不管了
            return;
        }

        //生成的是原始key, 需要套编码才能使用或者组合成KeyPair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        Key pub = keyPair.getPublic();
        Key pvt = keyPair.getPrivate();
        try (OutputStream out = new FileOutputStream(new File(dir, "jwt_rsa.key"))) {
            out.write(pvt.getEncoded());
        }
        try (OutputStream out2 = new FileOutputStream(publicKeyFile)) {
            out2.write(pub.getEncoded());
        }
    }
}
