package com.alkaid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.llrp.ltk.generated.custom.parameters.ImpinjPeakRSSI;
import org.llrp.ltk.generated.custom.parameters.ImpinjRFPhaseAngle;
import org.llrp.ltk.generated.parameters.Custom;
import org.llrp.ltk.generated.parameters.EPCData;
import org.llrp.ltk.generated.parameters.EPC_96;
import org.llrp.ltk.generated.parameters.TagReportData;
import org.llrp.ltk.types.LLRPParameter;

import com.csvreader.CsvWriter;

/**
 * CSV记录器, 将数据记录到csv文件
 */
public class CsvRecorder extends BaseRecorder {

    private static Logger logger = Logger.getLogger(CsvRecorder.class);

    /**
     * csv写入
     */
    private CsvWriter csvWriter;

    /**
     * 父目录
     */
    private String parentDir;

    /**
     * 是否正在写入
     */
    private boolean isWriting;

    /**
     * 是否详细输出
     */
    private boolean verbose;

    /**
     * 已记录数
     */
    int recordCount;

    String currentTime;
    String currentEPC;
    String currentChannel;
    String currentPhase;
    String currentRSSI;
    String currentAntenna;

    /**
     * 构造函数
     * 
     * @param csvWriter
     */
    public CsvRecorder(String parentDir,Boolean verbose) {
        super();
        this.parentDir=parentDir;
        this.verbose = verbose;
        this.isWriting = false;
    }

    @Override
    public void start() {
        if (csvWriter == null) {
            this.recordCount = 0;
            this.initCsvWriter(parentDir);
            this.isWriting = true;
            super.start();
        } else {
            logger.error("Cannot start LogRecorder when it is already running");
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.isWriting = false;
        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }
    }

    @Override
    public void close() {
        super.close();
        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }
    }

    @Override
    protected void logOneTagReport(TagReportData tr) {
        LLRPParameter epcp = (LLRPParameter) tr.getEPCParameter();

        
        // 获取EPC
        if (epcp != null) {
            if (epcp.getName().equals("EPC_96")) {
                EPC_96 epc96 = (EPC_96) epcp;
                currentEPC = epc96.getEPC().toString();
            } else if (epcp.getName().equals("EPCData")) {
                EPCData epcData = (EPCData) epcp;
                currentEPC = epcData.getEPC().toString();
            }
            
        } else {
            logger.error("Could not find EPC in Tag Report");
            System.exit(1);
        }

        // 获取AntennaID
        if (tr.getAntennaID() != null) {
            currentAntenna = tr.getAntennaID().getAntennaID().toString();
        }

        // 获取ChannelIndex
        if (tr.getChannelIndex() != null) {
            currentChannel = tr.getChannelIndex().getChannelIndex().toString();
        }

        if (tr.getFirstSeenTimestampUTC() != null) {
            currentTime = tr.getFirstSeenTimestampUTC().getMicroseconds().toString();
        }

        List<Custom> clist = tr.getCustomList();
        for (Custom cd : clist) {
            if (cd.getClass() == ImpinjRFPhaseAngle.class) {
                currentPhase = ((ImpinjRFPhaseAngle) cd).getPhaseAngle().toString();
            }
            if (cd.getClass() == ImpinjPeakRSSI.class) {
                currentRSSI = ((ImpinjPeakRSSI) cd).getRSSI().toString();
            }

        }
        
        this.recordCount += 1;

        if (this.verbose) {
            this.showDetails();    
        } else {
            System.out.print("\rRecordCount: " + recordCount);
        }

        if(this.isWriting){
            this.writeToCsv();
        }
    }

    /**
     * 初始化csv写入
     * @param parentDir
     */
    private void initCsvWriter(String parentDir) {
        try {

            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            String fileName = "RFID_" + df.format(localDateTime) + ".csv";

            File csvFile = new File(parentDir + File.separator + fileName);

            File parent = csvFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            csvFile.createNewFile();
            logger.info("Writing to " + csvFile.getAbsolutePath());

            this.csvWriter=new CsvWriter(csvFile.getAbsolutePath(), ',',Charset.forName("GBK"));
            this.csvWriter.writeRecord(
                Arrays.asList(
                    "time",
                    "id", 
                    "channel", 
                    "phase", 
                    "rssi",
                    "antenna"
                ).toArray(new String[0])
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 显示详细数据
     */
    private void showDetails() {
        StringBuilder logString=new StringBuilder();
        logString.append("EPC: ").append(currentEPC);
        logString.append(" Antenna: ").append(currentAntenna);
        logString.append(" Channel: ").append(currentChannel);
        logString.append(" FirstSeen: ").append(currentTime);
        logString.append(" Phase: ").append(currentPhase);
        logString.append(" RSSI: ").append(currentRSSI);
        System.out.println(logString.toString());
        System.out.println("----count ---------" + recordCount);
    }

    /**
     * 写入数据到csv
     */
    private void writeToCsv() {
        try {
            csvWriter.writeRecord(
                Arrays.asList(
                    currentTime,
                    currentEPC,
                    currentChannel,
                    currentPhase,
                    currentRSSI,
                    currentAntenna
                ).toArray(new String[0])
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
