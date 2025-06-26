package com.alkaid;

import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.Scanner;

import org.apache.log4j.PropertyConfigurator;

import lombok.extern.log4j.Log4j;

@Log4j
public class Main {

    public static void main(String[] args) {

        // 获取资源文件夹路径
        String RESOURCE_PATH = Constant.RESOURCE_PATH;
        try {
            URL resourceUrl = Main.class.getClassLoader().getResource("log4j.properties");
            if (resourceUrl != null) {
                Path resourcePath = Path.of(resourceUrl.toURI());
                RESOURCE_PATH = resourcePath.getParent().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // BasicConfigurator.configure();
        PropertyConfigurator.configure(Path.of(RESOURCE_PATH, "log4j.properties").toString());

        final LogRecorder recorder = new LogRecorder("./output",false);
        String configPath=Path.of(RESOURCE_PATH, "SET_READER_CONFIG.xml").toString();
        String roSpecPath=Path.of(RESOURCE_PATH, "ADD_ROSPEC.xml").toString();
        recorder.initialize(
            Constant.HOST, 
            configPath,
            roSpecPath
        );


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

        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            if (recorder != null) {
                recorder.close();
            }
            System.out.println("Shutdown complete.");
        }));

        try {

            // 单次采集时长
            long duration=1000*20;

            Scanner input = new Scanner(System.in);
            boolean flag=true;
            while (flag) {
                flag=false;
                System.out.println("Please enter \"P\" to start collection");
                String line = input.nextLine();
                if (line.toUpperCase().equals("P")) {
                    flag=true;
                }

                if (flag){
                    log.info(String.format("Start collecting, expected to last for %ds", duration/1000));
                    recorder.start();
                    Thread.sleep(duration);
                    recorder.stop();
                    log.info("End collection");
                }
            }
            input.close();
        } catch (InterruptedException ex) {
            log.error("Sleep Interrupted");
        }

        System.exit(0);
    }
}
