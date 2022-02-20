package org.kin.agent;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仅仅加载{@link ClassDecoder}实例的{@link ClassLoader}实现
 *
 * @author huangjianqin
 * @date 2022/2/19
 */
public final class ClassDecoderClassLoader extends ClassLoader {
    private String decoderClassName;

    @SuppressWarnings("unchecked")
    public ClassDecoder getClassDecoderInstance() {
        //基于java spi机制加载解密ClassDecoder class文件的ClassDecoderDecoder实例
        ServiceLoader<ClassDecoderDecoder> classDecoderDecoderLoader = ServiceLoader.load(ClassDecoderDecoder.class);
        Iterator<ClassDecoderDecoder> iterator = classDecoderDecoderLoader.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("can not find ClassDecoderDecoder service");
        }
        //只取第一个
        ClassDecoderDecoder classDecoderDecoder = iterator.next();

        //读取ClassDecoder class文件
        //ClassDecoder实现类类名
        String decoderClassName;
        //ClassDecoder class文件目录 uri
        URI decoderDirUri = null;
        //jar(zip)file system, 一般说是需要读取实现类jar包META-INF下的文件
        FileSystem zipFs = null;
        //ClassDecoder class文件内容
        byte[] decoderClassBytes;
        try {
            //读取ClassDecoder class文件目录resource
            URL decoderDirUrl = Thread.currentThread().getContextClassLoader().getResource(Environment.DECODER_DIR);
            if (Objects.isNull(decoderDirUrl)) {
                throw new IllegalStateException("can not find decoder dir resource");
            }
            decoderDirUri = decoderDirUrl.toURI();
            //ClassDecoder class文件目录 scheme, 一般来说是jar
            String scheme = decoderDirUri.getScheme();
            if (!"file".equalsIgnoreCase(scheme)) {
                //非file schema, 则需要手动加载其file system, 否则解析不出path, 然后就无法遍历目录了
                //比如jar, 想读取其他jar内的内容, 也不能通过new File读取
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                zipFs = FileSystems.newFileSystem(decoderDirUri, env);
            }

            //ClassDecoder class文件目录 path
            Path decoderDirPath = Paths.get(decoderDirUri);
            //ClassDecoder class文件目录下所有的文件
            List<Path> decoderDirPaths = Files.list(decoderDirPath).collect(Collectors.toList());
            if (decoderDirPaths.isEmpty()) {
                //没有任何文件
                throw new IllegalStateException("can not find ClassDecoder class file");
            }
            if (decoderDirPaths.size() > 1) {
                //多于1个文件
                throw new IllegalStateException(String.format("'%s' have more than one class file", Environment.DECODER_DIR));
            }
            //取第一个
            Path decoderClassPath = decoderDirPaths.get(0);
            //读取class内容
            decoderClassBytes = Files.readAllBytes(decoderClassPath);
            //获取ClassDecoder实现类类名
            decoderClassName = decoderClassPath.getFileName().toString();
            decoderClassName = decoderClassName.substring(0, decoderClassName.lastIndexOf("."));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("read ClassDecoder class file error", e);
        } finally {
            //释放zip file system扫描目录后缓存的数据
            if (Objects.nonNull(zipFs)) {
                Environment.removeFromZipProvider(decoderDirUri, zipFs);
            }
        }

        //解密后的ClassDecoder class内容
        byte[] realDecoderClassBytes;
        try {
            //使用ClassDecoderDecoder实例解密
            realDecoderClassBytes = classDecoderDecoder.decode(decoderClassBytes);
        } catch (Exception e) {
            throw new IllegalStateException("ClassDecoderDecoder decode ClassDecoder class file error", e);
        }

        //加载ClassDecoder
        Class<ClassDecoder> classDecoderClass = (Class<ClassDecoder>)
                defineClass(decoderClassName, realDecoderClassBytes, 0, realDecoderClassBytes.length);
        //加载成功, 更新ClassDecoder实现类类名
        this.decoderClassName = decoderClassName;
        try {
            //利用反射创建ClassDecoder实现类实例
            return classDecoderClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("construct ClassDecoder instance error", e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //根据类名判断, 仅仅返回指定ClassDecoder实现类
        if (!name.equals(decoderClassName)) {
            throw new IllegalArgumentException("this class loader just load ClassDecoder class");
        }
        return super.findClass(name);
    }
}
