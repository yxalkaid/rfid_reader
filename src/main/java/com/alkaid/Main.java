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

        LogReader reader = new LogReader("./output");
        reader.initialize(
            Constant.HOST, 
        Path.of(RESOURCE_PATH, "SET_READER_CONFIG.xml").toString(), 
        Path.of(RESOURCE_PATH, "ADD_ROSPEC.xml").toString()
        );

        try {
            reader.start();
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            log.error("Sleep Interrupted");
        }
        reader.close();
        System.exit(0);
    }
}
