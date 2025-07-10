# RFID_READER

## 依赖
1. ltkjava-10.16.0.240
2. javacsv

```
rfid_reader
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java
   │  │  └─ com
   │  │     └─ alkaid
   │  │        ├─ Demo.java             # 实时数据处理——Demo
   │  │        ├─ Listener              # 监听器
   │  │        │  ├─ CommandListener.java   # 命令行监听器
   │  │        │  └─ UdpListener.java       # UDP监听器
   │  │        ├─ Main.java             # 主程序
   │  │        ├─ old                   # 数据采集程序——旧版本
   │  │        │  └─ DocSample4.java
   │  │        ├─ proto                 # protobuf文件
   │  │        │  └─ Data.java              # proto自动生成类
   │  │        ├─ Recorder              # 数据记录器
   │  │        │  ├─ BaseRecorder.java      # 记录器基类
   │  │        │  ├─ CsvRecorder.java       # CSV记录器
   │  │        │  ├─ EPCRecorder.java       # EPC记录器
   │  │        │  └─ UdpRecorder.java       # UDP记录器
   │  │        └─ utils                 # 工具类
   │  │           ├─ Constant.java          # 常量
   │  │           ├─ DataGenerator.java     # 数据生成器
   │  │           └─ UdpSender.java         # UDP发送器
   │  ├─ proto
   │  │  └─ data.proto
   │  └─ resources
   │     ├─ ADD_ROSPEC.backup.xml   # 备份
   │     ├─ ADD_ROSPEC.xml          # ROSPEC配置
   │     ├─ log4j.properties        # 日志配置
   │     └─ SET_READER_CONFIG.xml   # 读写器配置
   └─ test
      └─ java
```