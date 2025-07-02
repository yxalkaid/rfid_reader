package com.alkaid.Listener;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.alkaid.Recorder.BaseRecorder;

import lombok.extern.log4j.Log4j;

/**
 * UDP监听控制器
 */
@Log4j
public class UdpListener implements Runnable {
    
    /**
     * 监听端口
     */
    private final int port;

    /**
     * RFID 记录器
     */
    private final BaseRecorder recorder;

    /**
     * 是否运行中
     */
    private volatile boolean isRunning = true;

    public UdpListener(BaseRecorder recorder,int port) {
        this.recorder = recorder;
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(this.port)) {
            log.info("UDP listener started on port "+this.port);

            byte[] buffer = new byte[1024];
            while (isRunning && !Thread.interrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String command = new String(packet.getData(), 0, packet.getLength()).trim();
                log.info("Received UDP command: " + command);

                // 处理命令
                if ("START".equalsIgnoreCase(command)) {
                    recorder.start();
                } else if ("STOP".equalsIgnoreCase(command)) {
                    recorder.stop();
                } else if ("CLOSE".equalsIgnoreCase(command)) {
                    recorder.stop();
                    isRunning = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
    }
}