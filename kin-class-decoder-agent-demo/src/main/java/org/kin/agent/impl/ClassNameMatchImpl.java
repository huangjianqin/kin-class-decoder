package org.kin.agent.impl;

import org.kin.agent.ClassNameMatcher;

/**
 * @author huangjianqin
 * @date 2022/2/20
 */
public class ClassNameMatchImpl implements ClassNameMatcher {
    @Override
    public boolean match(String className) {
        //仅仅解密org.kin.jclass包下的类
        return className.startsWith("org.kin.jclass");
    }
}
