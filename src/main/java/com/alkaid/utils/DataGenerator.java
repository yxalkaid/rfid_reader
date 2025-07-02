package com.alkaid.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.alkaid.proto.Data;

public class DataGenerator {

    private static final Logger logger = Logger.getLogger(DataGenerator.class);

    /**
     * 标签数据
     */
    class TagData {

        int phase;
        int channel;

        public TagData() {
            this.phase = 0;
            this.channel = -1;
        }
    }

    class Buffer {
        final int[][] sumData;
        final int[][] recordData;
        int totalCount;

        public Buffer(int antennaCount, int tagCount) {
            sumData = new int[antennaCount][tagCount];
            recordData = new int[antennaCount][tagCount];
            totalCount = 0;
        }

        public void reset() {
            int length = sumData.length;
            for (int i = 0; i < length; i++) {
                java.util.Arrays.fill(sumData[i], 0);
                java.util.Arrays.fill(recordData[i], 0);
            }
            totalCount = 0;
        }
    }

    class BufferPool {
        private final Buffer[] buffers = new Buffer[2];
        private int index = 0;

        public BufferPool(int antennaCount, int tagCount) {
            buffers[0] = new Buffer(antennaCount, tagCount);
            buffers[1] = new Buffer(antennaCount, tagCount);
        }

        public Buffer get() {
            return buffers[index];
        }

        public synchronized Buffer swap() {
            index = 1 - index;
            return buffers[index];
        }

        public void reset() {
            for (Buffer buffer : buffers) {
                buffer.reset();
            }
        }
    }

    /**
     * 标签名-索引映射
     */
    private final Map<String, Integer> tagMap;

    /**
     * 天线号-索引映射
     */
    private final Map<Integer, Integer> antennaMap;

    /**
     * 缓存池
     */
    private BufferPool bufferPool;

    /**
     * 数据缓存
     */
    private AtomicReference<Buffer> buffer;

    /**
     * 标签数据
     */
    private final TagData[][] rawData;

    /**
     * 锁
     */
    private ReentrantLock lock;

    /**
     * 数据序号
     */
    private long dataIndex;

    /**
     * UDP发送器
     */
    private UdpSender udpSender;

    /**
     * 定时器
     */
    private ScheduledExecutorService scheduler;

    /**
     * 时间间隔
     */
    private int timeInterval;

    /**
     * 构造函数
     * 
     * @param tagMap
     * @param antennaMap
     * @param timeInterval
     */
    public DataGenerator(String[] tagList, int[] antennaList, int timeInterval) {

        if (tagList == null || tagList.length == 0) {
            throw new IllegalArgumentException("tagList can not be empty");
        }

        if (antennaList == null || antennaList.length == 0) {
            throw new IllegalArgumentException("antennaList can not be empty");
        }

        if (timeInterval <= 0) {
            throw new IllegalArgumentException("timeInterval must be greater than 0");
        }

        this.timeInterval = timeInterval;
        this.dataIndex = 0;
        int antennaCount = antennaList.length;
        int tagCount = tagList.length;

        this.antennaMap = new HashMap<>();
        for (int i = 0; i < antennaCount; i++) {
            antennaMap.put(antennaList[i], i);
        }

        this.tagMap = new HashMap<>();
        for (int i = 0; i < tagCount; i++) {
            // 去除标签名中的下划线
            String tagName = tagList[i].replace("_", "");
            tagMap.put(tagName, i);
        }

        this.lock = new ReentrantLock();
        this.bufferPool = new BufferPool(antennaCount, tagCount);
        this.buffer = new AtomicReference<>(bufferPool.get());

        // 初始化标签数据
        this.rawData = new TagData[antennaCount][tagCount];
        for (int a = 0; a < antennaCount; a++) {
            for (int t = 0; t < tagCount; t++) {
                this.rawData[a][t] = new TagData();
            }
        }
    }

    /**
     * 添加数据
     * 
     * @param antenna
     * @param tag
     * @param channel
     * @param phase
     */
    public void addData(int antenna, String tag, int channel, int phase) {
        Integer antennaIndex = antennaMap.get(antenna);
        Integer tagIndex = tagMap.get(tag);
        if (antennaIndex == null || tagIndex == null) {
            return;
        }

        lock.lock();
        try {
            Buffer current = this.buffer.get();
            TagData tagData = this.rawData[antennaIndex][tagIndex];
            if (tagData.channel == channel) {
                int diff = phase - tagData.phase;
                diff = correctPhaseDiff(diff);

                current.sumData[antennaIndex][tagIndex] += diff;
                current.recordData[antennaIndex][tagIndex] += 1;
            }
            current.totalCount += 1;

            tagData.phase = phase;
            tagData.channel = channel;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 校正相位差
     * 
     * @param phaseDiff
     * @return
     */
    private int correctPhaseDiff(int phaseDiff) {

        if (phaseDiff >= 2048) {
            phaseDiff -= 4096;
        } else if (phaseDiff < -2048) {
            phaseDiff += 4096;
        }
        return phaseDiff;
    }

    /**
     * 定时发送数据
     */
    private void scheduledSend() {
        Buffer oldBuffer = this.buffer.getAndSet(bufferPool.swap());

        logger.info("scheduledSend: " + oldBuffer.totalCount);
        if (oldBuffer.totalCount == 0) {
            return;
        }

        try {
            Data.DataPoint dataPoint = buildDataPoint(oldBuffer);
            oldBuffer.reset();
            byte[] buffer = dataPoint.toByteArray();
            if (udpSender != null) {
                udpSender.sendMessage(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 构建数据包
     * 
     * @param buffer
     * @return
     */
    private Data.DataPoint buildDataPoint(Buffer buffer) {
        final long timestamp = System.currentTimeMillis();
        final int totalRecords = buffer.totalCount;

        final int antennaCount = antennaMap.size();
        final int tagCount = tagMap.size();

        Data.DataPoint.Builder dataBuilder = Data.DataPoint.newBuilder()
                .setIndex(dataIndex++)
                .setTime(timestamp)
                .setCSIZE(antennaCount)
                .setXSIZE(tagCount);

        for (int a = 0; a < antennaCount; a++) {
            int[] antennaTotal = buffer.sumData[a];
            for (int t = 0; t < tagCount; t++) {

                // 计算均值
                double value = (double) antennaTotal[t];
                dataBuilder.addData(value / totalRecords);
            }
        }

        return dataBuilder.build();
    }

    /**
     * 初始化发送器
     * 
     * @param host
     * @param port
     */
    public void initSender(String host, int port) {
        if (this.udpSender == null) {
            try {
                this.udpSender = new UdpSender(host, port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动
     * 
     * @param timeInterval
     */
    public void start() {
        if (scheduler == null) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(
                    this::scheduledSend,
                    timeInterval,
                    timeInterval,
                    TimeUnit.MILLISECONDS);
        } else {
            logger.error("DataGenerator is already started");
        }
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
    }

    /**
     * 关闭
     */
    public void close() {

        this.stop();
        if (udpSender != null) {
            udpSender.close();
            udpSender = null;
        }
    }
}