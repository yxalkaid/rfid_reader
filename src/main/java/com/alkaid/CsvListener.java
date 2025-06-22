package com.alkaid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.csvreader.CsvWriter;
import com.impinj.octane.AntennaConfigGroup;
import com.impinj.octane.ImpinjReader;
import com.impinj.octane.OctaneSdkException;
import com.impinj.octane.ReportConfig;
import com.impinj.octane.ReportMode;
import com.impinj.octane.Settings;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;

@Deprecated
public class CsvListener implements TagReportListener {

    /**
     * csv写入
     */
    private CsvWriter csvWriter;

    /**
     * 构造函数
     * @param parentDir
     */
    public CsvListener(String parentDir) {
        this.initCsvWriter(parentDir);
    }

    /**
     * 初始化csv写入
     * 
     * @param parentDir
     */
    private void initCsvWriter(String parentDir) {
        try {

            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String fileName = "CSV_" + df.format(localDateTime) + ".csv";

            File csvFile = new File(parentDir + File.separator + fileName);

            File parent = csvFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            csvFile.createNewFile();
            this.csvWriter = new CsvWriter(csvFile.getAbsolutePath(), ',', Charset.forName("GBK"));

            this.csvWriter.writeRecord(
                    Arrays.asList(
                            "time",
                            "id",
                            "channel",
                            "phase",
                            "rssi",
                            "antenna").toArray(new String[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭
     */
    public void close() { 

        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }
    }

    /**
     * 写入数据到csv
     */
    private void writeToCsv(String... data) {
        try {
            csvWriter.writeRecord(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTagReported(ImpinjReader reader, TagReport tagReport) {
        for (Tag tag : tagReport.getTags()) {
            StringBuilder logString=new StringBuilder();
            
            String time=tag.getFirstSeenTime().getLocalDateTime().toString();
            String id=tag.getEpc().toString();
            String channel=String.valueOf(tag.getChannelInMhz());
            String phase=String.valueOf(tag.getPhaseAngleInRadians());
            String rssi=String.valueOf(tag.getPeakRssiInDbm());
            String antenna=String.valueOf(tag.getAntennaPortNumber());

            logString
                .append(time).append(",")
                .append(id).append(",")
                .append(channel).append(",")
                .append(phase).append(",")
                .append(rssi).append(",")
                .append(antenna).append("\n");

            System.out.println(logString.toString());

            // 写入 CSV
            this.writeToCsv(time, id, channel,phase, rssi, antenna);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Must pass reader hostname or IP as argument 1");
            return;
        }

        try {
            ImpinjReader reader = new ImpinjReader();

            // 连接读写器
            String hostname = args[0];
            System.out.println("Connecting to reader: " + hostname);
            reader.connect(hostname);

            // 配置读写器
            Settings settings = reader.queryDefaultSettings();
            AntennaConfigGroup antennas = settings.getAntennas();
            antennas.getAntenna(1).setIsMaxRxSensitivity(true); // 使用最大接收灵敏度
            antennas.getAntenna(1).setIsMaxTxPower(true);       // 使用最大发射功率

            ReportConfig report = settings.getReport();
            report.setIncludeAntennaPortNumber(true);           // 包含天线端口号
            report.setMode(ReportMode.Individual);              // 单个标签触发报告

            reader.applySettings(settings);

            CsvListener csvListener=new CsvListener("./output");

            // 注册标签读取事件监听器
            reader.setTagReportListener(csvListener);

            // 启动读取
            System.out.println("Starting tag reading...");
            reader.start();

            // 模拟运行一段时间
            Thread.sleep(30000);

            // 停止读取并断开连接
            System.out.println("Stopping tag reading...");
            csvListener.close();
            reader.stop();
            reader.disconnect();
        } catch (OctaneSdkException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
