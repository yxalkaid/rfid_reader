package com.alkaid;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.llrp.ltk.generated.parameters.EPCData;
import org.llrp.ltk.generated.parameters.EPC_96;
import org.llrp.ltk.generated.parameters.TagReportData;
import org.llrp.ltk.types.LLRPParameter;

/**
 * EPC记录器，读取所有出现的EPC
 */
public class EPCRecorder extends BaseRecorder {

    private static Logger logger = Logger.getLogger(CsvRecorder.class);

    /**
     * 已记录数
     */
    int recordCount;

    HashSet<String> epcSet;

    /**
     * 构造函数
     * 
     * @param csvWriter
     */
    public EPCRecorder() {
        super();
    }

    @Override
    public void start() {
        if (epcSet == null) {
            this.recordCount = 0;
            this.epcSet = new HashSet<String>();
            super.start();
        } else {
            logger.error("Cannot start LogRecorder when it is already running");
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (epcSet != null) {
            epcSet.clear();
            epcSet = null;
        }
    }

    @Override
    public void close() {
        super.close();
        if (epcSet != null) {
            epcSet.clear();
            epcSet = null;
        }
    }

    @Override
    protected void logOneTagReport(TagReportData tr) {
        LLRPParameter epcp = (LLRPParameter) tr.getEPCParameter();

        
        // 获取EPC
        if (epcp != null) {
            String EPC=null;
            if (epcp.getName().equals("EPC_96")) {
                EPC_96 epc96 = (EPC_96) epcp;
                EPC = epc96.getEPC().toString();
            } else if (epcp.getName().equals("EPCData")) {
                EPCData epcData = (EPCData) epcp;
                EPC = epcData.getEPC().toString();
            }

            if (this.epcSet.contains(EPC)) {
                
            }else{
                this.epcSet.add(EPC);
                System.out.println(EPC);
            }
        } else {
            logger.error("Could not find EPC in Tag Report");
            System.exit(1);
        }
        
        this.recordCount += 1;
    }
}
