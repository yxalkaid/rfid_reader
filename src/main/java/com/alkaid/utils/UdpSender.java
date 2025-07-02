package com.alkaid.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP发送器
 */
public class UdpSender {

    private DatagramSocket socket;

    /**
     * 目标地址
     */
    private InetAddress address;

    /**
     * 目标端口
     */
    private int port;

    public UdpSender(String host, int port) throws IOException {
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(host);
        this.port = port;
    }


    /**
     * 发送数据
     * @param buffer
     */
    public void sendMessage(byte[] buffer) {
        try {
            if (buffer == null||buffer.length==0) {
                throw new IllegalArgumentException("buffer is null or empty");
            }
            if (buffer.length > 65507) {
                throw new IllegalArgumentException("Data too long for UDP packet");
            }

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.address, this.port);
            socket.send(packet);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
            address = null;
            port = 0;
        }
    }
}