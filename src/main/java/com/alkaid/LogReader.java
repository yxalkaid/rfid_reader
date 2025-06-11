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

public class LogReader extends BaseReader {

    private static Logger logger = Logger.getLogger(LogReader.class);

    /**
     * csv写入
     */
    private CsvWriter csvWriter;

    int phaseCount;
    String currentReadTime;
    String currentEPC;
    String currentChannelIndex;
    String currentRfPhase;
    String currentPeakRSSI;
    String currentAntennaID;

    /**
     * 构造函数
     * 
     * @param csvWriter
     */
    public LogReader(String parentDir) {
        super();
        this.initCsvWriter(parentDir);
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

        StringBuilder logString=new StringBuilder();
        logString.append("EPC: ");

        if (epcp != null) {
            if (epcp.getName().equals("EPC_96")) {
                EPC_96 epc96 = (EPC_96) epcp;
                currentEPC = epc96.getEPC().toString();
            } else if (epcp.getName().equals("EPCData")) {
                EPCData epcData = (EPCData) epcp;
                currentEPC = epcData.getEPC().toString();
            }
            logString.append(currentEPC);
        } else {
            logger.error("Could not find EPC in Tag Report");
            System.exit(1);
        }

        if (tr.getAntennaID() != null) {
            currentAntennaID = tr.getAntennaID().getAntennaID().toString();
            logString.append(" Antenna: ").append(currentAntennaID);
        }

        if (tr.getChannelIndex() != null) {
            currentChannelIndex = tr.getChannelIndex().getChannelIndex().toString();
            logString.append(" ChanIndex: ").append(currentChannelIndex);
        }

        if (tr.getFirstSeenTimestampUTC() != null) {
            currentReadTime = tr.getFirstSeenTimestampUTC().getMicroseconds().toString();
            logString.append(" FirstSeen: ").append(currentReadTime);
        }

        if (tr.getPeakRSSI() != null) {
            logString.append(" RSSI: ").append(tr.getPeakRSSI().getPeakRSSI().toString());
        }

        List<Custom> clist = tr.getCustomList();
        for (Custom cd : clist) {
            if (cd.getClass() == ImpinjRFPhaseAngle.class) {
                currentRfPhase = ((ImpinjRFPhaseAngle) cd).getPhaseAngle().toString();
                logString.append(" ImpinjPhase: ").append(currentRfPhase);
            }
            if (cd.getClass() == ImpinjPeakRSSI.class) {
                currentPeakRSSI = ((ImpinjPeakRSSI) cd).getRSSI().toString();
                logString.append(" ImpinjPeakRSSI: ").append(currentPeakRSSI);
            }

        }

        System.out.println(logString.toString());
        System.out.println("----count ---------" + phaseCount);
        phaseCount += 1;

        this.writeToCsv();
    }

    /**
     * 初始化csv写入
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
            this.csvWriter=new CsvWriter(csvFile.getAbsolutePath(), ',',Charset.forName("GBK"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入数据到csv
     */
    private void writeToCsv() {
        try {
            csvWriter.writeRecord(
                Arrays.asList(
                    currentReadTime,
                    currentEPC,
                    currentChannelIndex,
                    currentRfPhase,
                    currentPeakRSSI,
                    currentAntennaID
                ).toArray(new String[0])
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
