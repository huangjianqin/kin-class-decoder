package org.kin.jclass.decoder.demo;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * 对jar包进行加密逻辑
 * @author huangjianqin
 * @date 2022/2/20
 */
public class JarEncrypt {
    private static final byte[] HEAD = "kin".getBytes(StandardCharsets.UTF_8);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String jarPath = "kin-class-decoder-demo/target/kin-class-decoder-demo-0.1.0.0.jar";
        //创建临时目录
        Path dir = Files.createTempDirectory(UUID.randomUUID().toString());
        File tempDir = dir.toFile();
        //如果存在就删除
        tempDir.deleteOnExit();

        File jarFile = new File(jarPath);
        //解压到临时目录
        decompress(jarFile, tempDir);

        //对临时目录内所有class文件进行加密
        Collection<File> classFiles = FileUtils.listFiles(tempDir, new String[]{"class"}, true);
        for (File classFile : classFiles) {
            byte[] bytes = FileUtils.readFileToByteArray(classFile);

            for (int i = 0; i < bytes.length; i++) {
                //按位取反
                bytes[i] = (byte) ~bytes[i];
            }

            //magic code + 原class文件内容简单按位取反
            byte[] encryptBytes = new byte[HEAD.length + bytes.length];
            System.arraycopy(HEAD, 0, encryptBytes, 0, HEAD.length);
            System.arraycopy(bytes, 0, encryptBytes, HEAD.length, bytes.length);

            Files.write(classFile.toPath(), encryptBytes);
        }

        //输出到runJar/下
        String targetDir = "runJar/";
        String encryptJarPath = targetDir + FilenameUtils.getBaseName(jarPath) + "_encrypt.jar";
        compress(new File(encryptJarPath), Objects.requireNonNull(tempDir.listFiles()));
    }

    /**
     * 解压jar包
     */
    private static void decompress(File jarFile, File dest) throws Exception {
        try(JarArchiveInputStream jais = new JarArchiveInputStream(new FileInputStream(jarFile))){
            JarArchiveEntry entry;
            while(Objects.nonNull(entry = jais.getNextJarEntry())){
                if(entry.isDirectory()){
                    continue;
                }

                File current = new File(dest, entry.getName());
                File parentFile = current.getParentFile();
                if(!parentFile.exists()){
                    if(!parentFile.mkdirs()){
                        throw new IllegalStateException("can not create directory: " + parentFile.getPath());
                    }
                }

                IOUtils.copy(jais, new FileOutputStream(current));
            }
        }
    }

    /**
     * 压缩成jar包
     */
    private static void compress(File dest, File... files) throws IOException {
        try(JarArchiveOutputStream jaos = new JarArchiveOutputStream(new FileOutputStream(dest))){
            for (File file : files) {
                compress(jaos, file, ".");
            }
        }
    }

    private static void compress(JarArchiveOutputStream jaos, File file, String dir) throws IOException {
        String name = dir + File.separator + file.getName();
        if(".".equals(dir)){
            name = file.getName();
        }

        if(file.isFile()){
            JarArchiveEntry entry = new JarArchiveEntry(name);
            jaos.putArchiveEntry(entry);
            entry.setSize(file.length());
            IOUtils.copy(new FileInputStream(file), jaos);
            jaos.closeArchiveEntry();
        }else if(file.isDirectory()){
            File[] childs = file.listFiles();
            if (Objects.nonNull(childs)) {
                for (File child : childs) {
                    compress(jaos, child, name);
                }
            }
        }
        else{
            System.err.println(file.getName() + " is not supported");
        }
    }
}
