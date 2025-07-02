package com.alkaid;

import java.net.URL;
import java.nio.file.Path;

import org.apache.log4j.PropertyConfigurator;

import com.alkaid.Listener.CommandListener;
import com.alkaid.Recorder.UdpRecorder;
import com.alkaid.utils.Constant;
import com.alkaid.utils.DataGenerator;

import lombok.extern.log4j.Log4j;

@Log4j
public class Demo {

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

        DataGenerator dataGenerator=new DataGenerator(Constant.TAG_LIST,Constant.ANTENNA_LIST, 125);
        dataGenerator.initSender("localhost", 8090);

        final UdpRecorder recorder = new UdpRecorder(dataGenerator);

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

            // UDP控制
            // Runnable listener = new UdpListener(recorder,8081);

            // 命令行控制
            Runnable listener = new CommandListener(recorder,1000*60);

            Thread listenerThread = new Thread(listener);
            listenerThread.start();
            listenerThread.join();
            // listenerThread.interrupt();
        } catch (InterruptedException e) {
            log.error("Sleep Interrupted");
        }

        System.exit(0);
    }
}
