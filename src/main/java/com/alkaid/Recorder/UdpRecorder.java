package com.alkaid.Recorder;

import java.util.List;

import org.apache.log4j.Logger;
import org.llrp.ltk.generated.custom.parameters.ImpinjRFPhaseAngle;
import org.llrp.ltk.generated.parameters.Custom;
import org.llrp.ltk.generated.parameters.EPCData;
import org.llrp.ltk.generated.parameters.EPC_96;
import org.llrp.ltk.generated.parameters.TagReportData;
import org.llrp.ltk.types.LLRPParameter;

import com.alkaid.utils.DataGenerator;

/**
 * UDP记录器, 将数据记录到通过UDP发送
 */
public class UdpRecorder extends BaseRecorder {

    private static Logger logger = Logger.getLogger(UdpRecorder.class);

    /**
     * 已记录数
     */
    private int recordCount;


    /**
     * 数据生成器
     */
    private DataGenerator dataGenerator;

    /**
     * 构造函数
     * 
     * @param csvWriter
     */
    public UdpRecorder(DataGenerator dataGenerator) {
        super();

        this.dataGenerator = dataGenerator;
    }

    @Override
    public void start() {
        this.recordCount = 0;
        super.start();
        if (this.dataGenerator != null) {
            this.dataGenerator.start();
        } else{
            logger.error("Cannot start UdpRecorder when dataGenerator is null");
        }
    }

    @Override
    public void stop() {
        super.stop();
        logger.info("TotalRecordCount: "+recordCount);
        if(this.dataGenerator != null){
            this.dataGenerator.stop();
        }
    }

    @Override
    public void close() {
        super.close();
        if(this.dataGenerator != null){
            this.dataGenerator.close();
            this.dataGenerator = null;
        }
    }

    @Override
    protected void logOneTagReport(TagReportData tr) {
        LLRPParameter epcp = (LLRPParameter) tr.getEPCParameter();

        
        // 获取EPC
        String EPC=null;
        if (epcp != null) {
            if (epcp.getName().equals("EPC_96")) {
                EPC_96 epc96 = (EPC_96) epcp;
                EPC = epc96.getEPC().toString();
            } else if (epcp.getName().equals("EPCData")) {
                EPCData epcData = (EPCData) epcp;
                EPC = epcData.getEPC().toString();
            }
            
        } else {
            logger.error("Could not find EPC in Tag Report");
            System.exit(1);
        }

        // 获取AntennaID
        int antenna=-1;
        if (tr.getAntennaID() != null) {
            antenna = tr.getAntennaID().getAntennaID().toInteger();
        }

        // 获取ChannelIndex
        int channel=-1;
        if (tr.getChannelIndex() != null) {
            channel = tr.getChannelIndex().getChannelIndex().toInteger();
        }

        int phase=0;
        List<Custom> clist = tr.getCustomList();
        for (Custom cd : clist) {
            if (cd.getClass() == ImpinjRFPhaseAngle.class) {
                phase = ((ImpinjRFPhaseAngle) cd).getPhaseAngle().toInteger();
                break;
            }
        }
        
        this.recordCount += 1;
        this.dataGenerator.addData(antenna, EPC, channel, phase);
    }
}
