package com.alkaid;

import java.nio.file.Path;

import org.apache.log4j.PropertyConfigurator;

import lombok.extern.log4j.Log4j;

@Log4j
public class Main {

    public static void main(String[] args) {

        // 资源文件目录
        final String RESOURCE_PATH=Constant.RESOURCE_PATH;

        // BasicConfigurator.configure();
        PropertyConfigurator.configure(Path.of(RESOURCE_PATH, "log4j.properties").toString());

        final LogReader reader = new LogReader("./output");
        reader.initialize(
            Constant.HOST, 
        Path.of(RESOURCE_PATH, "SET_READER_CONFIG.xml").toString(), 
        Path.of(RESOURCE_PATH, "ADD_ROSPEC.xml").toString()
        );

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            if (reader != null) {
                reader.close();
            }
            System.out.println("Shutdown complete.");
        }));

        try {
            reader.start();
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            log.error("Sleep Interrupted");
        }

        /*
         * 特别注意
         * 添加ShutdownHook后，
         * 以下情况会正常调用关闭钩子
         * 1. 程序自然运行结束
         * 2. 主动调用System.exit(int status)
         * 3. 通过 Ctrl+C 发送中断信号
         * 4. 抛出未捕获的运行时异常
         * 
         * 以下情况不会触发关闭钩子
         * 1. 使用 kill -9 或任务管理器强制终止进程
         * 2. 通过 IDE 点击“Stop”按钮终止程序（实际上类似使用 kill -9）
         * 3. JVM本身崩溃
         */

        // reader.close();
        System.exit(0);
    }
}
