# kin-class-decoder-agent

类解密agent逻辑抽象, 只需要简单几步即可实现对Application jar包进行加密.

## 自定义实现agent步骤

1. 实现`ClassDecoder`定义解密逻辑
2. 实现`ClassDecoderDecoder`定义对`ClassDecoder`的解密逻辑
3. 根据需要实现`ClassNameMatcher`, 告诉框架那些类需要解密. 强烈建议实现, 不然所有类都会进行解密, 一些项目依赖的框架类可能会出现问题
4. 对于`ClassDecoder`实现class文件加密并方到`META-INF/classDecoder/`目录下. 注意, 文件名需要该实现类全限定类名并且class文件有且仅有一个.
5. 创建`META-INF/service/org.kin.agent.ClassDecoderDecoder`文件并将`ClassDecoderDecoder`实现类全限定类名写进该文件, 有且仅有一个.
6. (可选)创建`META-INF/service/org.kin.agent.ClassNameMatcher`文件并将`ClassNameMatcher`实现类全限定类名写进该文件, 可以多个.
7. mvn打包时带上配置
    ```xml
    <configuration>
       <archive>
           <manifestEntries>
                <!-- manifest内自定义的key value-->
                <Premain-Class>org.kin.agent.ClassDecoderAgent</Premain-Class>>
                <Can-Redefine-Classes>true</Can-Redefine-Classes>
                <Can-Retransform-Classes>true</Can-Retransform-Classes>
           </manifestEntries>
       </archive>
    </configuration>
    ```