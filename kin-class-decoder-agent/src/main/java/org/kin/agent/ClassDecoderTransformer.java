package org.kin.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * class文件内容解密{@link ClassFileTransformer}实现.
 * 每次加载class文件后都会调用注册的{@link ClassFileTransformer}实例对class文件额外处理
 * @author huangjianqin
 * @date 2022/2/19
 */
public final class ClassDecoderTransformer implements ClassFileTransformer {
    /** 单例 */
    private static final ClassDecoderTransformer INSTANCE = new ClassDecoderTransformer();

    public static ClassDecoderTransformer instance(){
        return INSTANCE;
    }

    /** 类名匹配实例, 用于过滤不需要解密的类 */
    private final List<ClassNameMatcher> matchers;

    private ClassDecoderTransformer() {
        //基于java spi机制加载类名匹配实例
        ServiceLoader<ClassNameMatcher> loader = ServiceLoader.load(ClassNameMatcher.class);
        Iterator<ClassNameMatcher> iterator = loader.iterator();
        if(iterator.hasNext()){
            List<ClassNameMatcher> matchers = new ArrayList<>();
            while(iterator.hasNext()){
                matchers.add(iterator.next());
            }
            this.matchers = Collections.unmodifiableList(matchers);
        }
        else{
            matchers = Collections.emptyList();
        }
    }

    /**
     * 解密逻辑
     * @param className         不是标准的类命名, 即不是以.分割, 而是/
     * @param classfileBuffer   class文件内容
     */
    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        //转换成标准类命名
        String standardClassName = className.replaceAll("/", "\\.");

        //检查类名是否匹配, 是否需要解密
        boolean needDecrypt = false;
        for (ClassNameMatcher matcher : matchers) {
            if (matcher.match(standardClassName)) {
                //只要有一个匹配即表示该类需要解密
                needDecrypt = true;
                break;
            }
        }
        if(needDecrypt){
            //不匹配
            return classfileBuffer;
        }

        //如果agent jar包中存在该类解密后的class文件内容, 则返回
        byte[] classBytes = EncryptedClassManager.getRealClassBytes(standardClassName);
        if (Objects.isNull(classBytes)) {
            //agent jar包中没有该类解密后的class文件内容
            try {
                //解密
                classBytes = EncryptedClassManager.getClassDecoder().decode(classfileBuffer);
            } catch (Exception e) {
                throw new IllegalStateException(String.format("ClassDecoder decode class '%s' error", className), e);
            }
        }
        return classBytes;
    }
}
