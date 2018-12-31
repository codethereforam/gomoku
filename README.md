# gomoku
基于Java NIO实现五子棋游戏

> 声明：该项目是基于本人以前用BIO+UDP实现的五子棋游戏快速修改而成，因此代码有点丑，请见谅。

### 组织结构
```
├── client -- 游戏客户端
├── common -- 公共部分
├── net -- 网络工具类
├── server -- 游戏服务器
└── util -- 工具类
```

### 打包文件
- gomoku-server.jar: 游戏服务器
- gomoku-client.jar: 游戏客户端

## 项目运行
1. 下载：git clone
1. 修改配置：修改`GomokuClient.java`中的服务器IP
1. 打包：mvn clean package
1. 复制服务器端jar包到服务器：scp gomoku-server.jar
1. 启动服务器：nohup java -jar gomoku-server.jar > gomoku.log 2>&1 &
1. 查看服务器日志：tail -20f gomoku.log
1. 运行多个客户端：java -jar gomoku-client.jar

## 运行截图

![gomoku](/images/gomoku1.png)

## 许可证

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)
