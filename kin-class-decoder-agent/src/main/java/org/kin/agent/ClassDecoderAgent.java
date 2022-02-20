package org.kin.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * premain agent
 *
 * @author huangjianqin
 * @date 2022/2/19
 */
public class ClassDecoderAgent {
    private static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation inst) {
        Environment.init();
        instrumentation = inst;
        //添加解密transformer
        instrumentation.addTransformer(ClassDecoderTransformer.instance());
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
