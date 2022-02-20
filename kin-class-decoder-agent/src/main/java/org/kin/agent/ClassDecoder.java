package org.kin.agent;

/**
 * @author huangjianqin
 * @date 2022/2/19
 */
@FunctionalInterface
public interface ClassDecoder {
    /**
     * class文件内容解密
     * @param classfileBuffer   加密后的class文件内容
     * @return 真正合法的class文件内容
     */
    byte[] decode(byte[] classfileBuffer) throws Exception;
}
