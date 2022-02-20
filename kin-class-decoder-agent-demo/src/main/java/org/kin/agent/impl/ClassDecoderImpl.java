package org.kin.agent.impl;

import org.kin.agent.ClassDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 去掉开头的magic code后, 对类class内容按位取反解密
 * @author huangjianqin
 * @date 2022/2/20
 */
public class ClassDecoderImpl implements ClassDecoder {
    private static final byte[] HEAD = "kin".getBytes(StandardCharsets.UTF_8);

    @Override
    public byte[] decode(byte[] classfileBuffer) {
        int headLen = HEAD.length;
        byte[] head = new byte[headLen];
        System.arraycopy(classfileBuffer, 0, head, 0, headLen);

        if (!Arrays.equals(head, HEAD)) {
            throw new IllegalStateException("head is not right");
        }

        byte[] ret = new byte[classfileBuffer.length - headLen];
        for (int i = headLen; i < classfileBuffer.length; i++) {
            //按位取反
            ret[i - headLen] = (byte) ~classfileBuffer[i];
        }

        return ret;
    }
}
