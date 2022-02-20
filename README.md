# kin-class-decoder
类解密agent逻辑抽象, 只需要简单几步即可实现对Application jar包进行加密.

## 模块说明
* `kin-class-decoder-agent`, 解密流程抽象, 自定义agent时, 需要依赖该模块, 详细请看该module下的README.md
* `kin-class-decoder-agent-demo`, 自定义agent实现demo
* `kin-class-decoder-demo`, 模拟Application的模块
* `kin-class-decoder-dependencies`, 项目maven依赖定义

## 运行命令
`java -javaagent:runJar/kin-class-decoder-agent-demo-0.1.0.0.jar -cp .:./runJar/* org.kin.jclass.decoder.demo.Class1`

说明:
* `-javaagent:runJar/kin-class-decoder-agent-demo-0.1.0.0.jar`, 定义解密实现的agent
* `-cp .:./runJar/*`, 定义classpath, 该目录应该包含所有支撑Application运行的jar包, 还有agent执行需要的依赖
* `org.kin.jclass.decoder.demo.Class1`, 即main class