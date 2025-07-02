package com.alkaid.Listener;

import java.util.Scanner;

import com.alkaid.Recorder.BaseRecorder;

import lombok.extern.log4j.Log4j;

/**
 * 命令行监听控制器
 */
@Log4j
public class CommandListener implements Runnable {

    // 单次采集时长
    private final long duration;

    /**
     * RFID 记录器
     */
    private final BaseRecorder recorder;

    /**
     * 是否运行中
     */
    private volatile boolean isRunning = true;

    public CommandListener(BaseRecorder recorder, long duration) {
        this.recorder = recorder;
        this.duration = duration;
    }

    @Override
    public void run() {
        Scanner input = null;
        try {
            input = new Scanner(System.in);
            while (isRunning && !Thread.interrupted()) {
                isRunning = false;
                System.out.println("Please enter \"P\" to start collection");
                String line = input.nextLine();
                if (line.toUpperCase().equals("P")) {
                    isRunning = true;
                }

                if (isRunning) {
                    log.info(String.format("Start collecting, expected to last for %ds", duration / 1000));
                    recorder.start();
                    Thread.sleep(duration);
                    recorder.stop();
                    log.info("End collection");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public void stop() {
        isRunning = false;
    }
}
