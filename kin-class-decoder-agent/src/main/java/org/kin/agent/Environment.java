package org.kin.agent;

import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2022/2/19
 */
public final class Environment {
    private Environment() {
    }

    /** 加密后的ClassDecoder class文件存在目录 */
    public static final String DECODER_DIR = "META-INF/classDecoder/";
    /**
     * 其他需要解密的class文件,
     * 比如java app需要使用spring, 但java app业务类加密了, 而spring加载class是spring框架实现的(ClassReader),
     * 所以, 需要重新实现ClassReader, 那么就需要将重写的ClassReader class文件放到该目录, 用于替换掉原来的实现.
     * 重写的逻辑注释要加载class文件内容那块, 可以调用Environment.getClassDecoder().decode(classfileBuffer)来解密
     * 注意, 这里面的内容也是使用{@link ClassDecoder}实现类来解密的, 而不是{@link ClassDecoderDecoder}
     */
    public static final String EXT_DIR = "META-INF/classDecoderExt/";
    /** class文件后缀定义 */
    public static final String CLASS_SUFFIX = "class";
    /** {@link ClassDecoder}实现类 */
    public static ClassDecoder classDecoder;
    /** key -> 其他需要解密的class类名, value -> {@link ClassDecoder}解密后的class文件内容 */
    private static final Map<String, byte[]> EXT_CLASS_BUFFER_MAP = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void init() {
        ClassDecoderClassLoader classDecoderClassLoader = new ClassDecoderClassLoader();
        classDecoder = classDecoderClassLoader.getClassDecoderInstance();

        URI extDirUri = null;
        FileSystem zipFs = null;
        try {
            URL extDirUrl = Thread.currentThread().getContextClassLoader().getResource(EXT_DIR);
            if (Objects.isNull(extDirUrl)) {
                return;
            }

            extDirUri = extDirUrl.toURI();
            String scheme = extDirUri.getScheme();
            if (!scheme.equalsIgnoreCase("file")) {
                //非file schema, 则需要手动加载其file system, 否则解析不出path, 然后就无法遍历目录了
                //比如jar, 想读取其他jar内的内容, 也不能通过new File读取
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                zipFs = FileSystems.newFileSystem(extDirUri, env);
            }

            List<Path> extPaths = Files.list(Paths.get(extDirUri)).collect(Collectors.toList());
            for (Path extPath : extPaths) {
                if (!extPath.endsWith(".class")) {
                    continue;
                }

                byte[] extClassBytes = Files.readAllBytes(extPath);
                String extClassName = extPath.getFileName().toString();
                extClassName = extClassName.substring(0, extClassName.lastIndexOf("."));
                byte[] realExtClassBytes;
                try {
                    realExtClassBytes = classDecoder.decode(extClassBytes);
                } catch (Exception e) {
                    throw new IllegalStateException(String.format("ClassDecoder decode class '%s' error, class path = '%s'", extClassName, extPath), e);
                }

                EXT_CLASS_BUFFER_MAP.put(extClassName, realExtClassBytes);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (Objects.nonNull(zipFs)) {
                Environment.removeFromZipProvider(extDirUri, zipFs);
            }
        }
    }

    /**
     * 因为需要扫描整个classpath的所有resources, 并且解析后, 这些数据是可以丢弃的,
     * 如果可以释放, 可以减少内存占用. 但是ZipFileSystemProvider并没有把相关方法开放出来(并不想开发者调用??),
     * 故使用反射从其FileSystem缓存中移除
     */
    @SuppressWarnings("JavaReflectionInvocation")
    public static void removeFromZipProvider(URI uri, FileSystem fileSystem) {
        try {
            Class<ZipFileSystemProvider> providerClass = ZipFileSystemProvider.class;
            //获取移除缓存方法
            Method removeMethod = providerClass.getDeclaredMethod("removeFileSystem", Path.class, ZipFileSystem.class);
            if (!removeMethod.isAccessible()) {
                removeMethod.setAccessible(true);
            }
            //该方法将URI转换成的Path, 并作为缓存的key
            Method uriToPathMethod = providerClass.getDeclaredMethod("uriToPath", URI.class);
            if (!uriToPathMethod.isAccessible()) {
                uriToPathMethod.setAccessible(true);
            }
            for (FileSystemProvider installedProvider : FileSystemProvider.installedProviders()) {
                if (installedProvider.getClass().equals(providerClass)) {
                    //remove
                    Path path = (Path) uriToPathMethod.invoke(installedProvider, uri);
                    removeMethod.invoke(installedProvider, path.toRealPath(), fileSystem);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] getExtClassBytes(String className) {
        return EXT_CLASS_BUFFER_MAP.get(className);
    }

    public static ClassDecoder getClassDecoder() {
        return classDecoder;
    }
}
