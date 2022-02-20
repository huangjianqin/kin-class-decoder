package org.kin.agent;

/**
 * @author huangjianqin
 * @date 2022/2/20
 */
public interface ClassNameMatcher {
    /**
     * 根据类名判断是否需要解密
     * @param className 标准类命名, 以.分割
     * @return  true即需要解密
     */
    boolean match(String className);
}
