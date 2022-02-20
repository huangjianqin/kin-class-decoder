package org.kin.agent;

/**
 * 正常来说, 实现类应该从权限管理服务器获取密钥, 使用某种解密算法对{@link ClassDecoder}实现类class文件内容进行解密
 * @author huangjianqin
 * @date 2022/2/19
 */
@FunctionalInterface
public interface ClassDecoderDecoder {
    /**
     * 解密{@link ClassDecoder}实现类class文件逻辑
     * @param decoderClassfileBuffer 加密后{@link ClassDecoder}class文件内容
     * @return 真正合法的{@link ClassDecoder}class文件内容
     */
    byte[] decode(byte[] decoderClassfileBuffer) throws Exception;
}
