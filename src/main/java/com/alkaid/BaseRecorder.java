package com.alkaid;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.llrp.ltk.exceptions.InvalidLLRPMessageException;
import org.llrp.ltk.generated.custom.messages.IMPINJ_ENABLE_EXTENSIONS;
import org.llrp.ltk.generated.custom.messages.IMPINJ_ENABLE_EXTENSIONS_RESPONSE;
import org.llrp.ltk.generated.enumerations.AISpecStopTriggerType;
import org.llrp.ltk.generated.enumerations.AirProtocols;
import org.llrp.ltk.generated.enumerations.GetReaderCapabilitiesRequestedData;
import org.llrp.ltk.generated.enumerations.GetReaderConfigRequestedData;
import org.llrp.ltk.generated.enumerations.ROSpecStartTriggerType;
import org.llrp.ltk.generated.enumerations.ROSpecState;
import org.llrp.ltk.generated.enumerations.ROSpecStopTriggerType;
import org.llrp.ltk.generated.enumerations.StatusCode;
import org.llrp.ltk.generated.interfaces.SpecParameter;
import org.llrp.ltk.generated.messages.ADD_ROSPEC;
import org.llrp.ltk.generated.messages.ADD_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.CLOSE_CONNECTION;
import org.llrp.ltk.generated.messages.CLOSE_CONNECTION_RESPONSE;
import org.llrp.ltk.generated.messages.DISABLE_ROSPEC;
import org.llrp.ltk.generated.messages.DISABLE_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.ENABLE_ROSPEC;
import org.llrp.ltk.generated.messages.ENABLE_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.GET_READER_CAPABILITIES;
import org.llrp.ltk.generated.messages.GET_READER_CAPABILITIES_RESPONSE;
import org.llrp.ltk.generated.messages.GET_READER_CONFIG;
import org.llrp.ltk.generated.messages.GET_READER_CONFIG_RESPONSE;
import org.llrp.ltk.generated.messages.READER_EVENT_NOTIFICATION;
import org.llrp.ltk.generated.messages.RO_ACCESS_REPORT;
import org.llrp.ltk.generated.messages.SET_READER_CONFIG;
import org.llrp.ltk.generated.messages.SET_READER_CONFIG_RESPONSE;
import org.llrp.ltk.generated.messages.START_ROSPEC;
import org.llrp.ltk.generated.messages.START_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.STOP_ROSPEC;
import org.llrp.ltk.generated.messages.STOP_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.parameters.AISpec;
import org.llrp.ltk.generated.parameters.AISpecStopTrigger;
import org.llrp.ltk.generated.parameters.AntennaConfiguration;
import org.llrp.ltk.generated.parameters.Custom;
import org.llrp.ltk.generated.parameters.GeneralDeviceCapabilities;
import org.llrp.ltk.generated.parameters.InventoryParameterSpec;
import org.llrp.ltk.generated.parameters.ROBoundarySpec;
import org.llrp.ltk.generated.parameters.ROSpec;
import org.llrp.ltk.generated.parameters.ROSpecStartTrigger;
import org.llrp.ltk.generated.parameters.ROSpecStopTrigger;
import org.llrp.ltk.generated.parameters.TagReportData;
import org.llrp.ltk.generated.parameters.TransmitPowerLevelTableEntry;
import org.llrp.ltk.generated.parameters.UHFBandCapabilities;
import org.llrp.ltk.net.LLRPConnection;
import org.llrp.ltk.net.LLRPConnectionAttemptFailedException;
import org.llrp.ltk.net.LLRPConnector;
import org.llrp.ltk.net.LLRPEndpoint;
import org.llrp.ltk.types.Bit;
import org.llrp.ltk.types.LLRPMessage;
import org.llrp.ltk.types.SignedShort;
import org.llrp.ltk.types.UnsignedByte;
import org.llrp.ltk.types.UnsignedInteger;
import org.llrp.ltk.types.UnsignedShort;
import org.llrp.ltk.types.UnsignedShortArray;
import org.llrp.ltk.util.Util;

/**
 * 基础阅读器
 */
public class BaseRecorder implements LLRPEndpoint, Closeable {

    private static Logger logger  = Logger.getLogger(BaseRecorder.class);

    private boolean isClosed = false;

    /**
     * 读写器连接
     */
    private LLRPConnection connection;

    /**
     * 消息ID
     */
    private int MessageID = 23;

    /**
     * ROSpec
     */
    private ROSpec rospec;

    
    private UnsignedInteger modelName;
    UnsignedShort maxPowerIndex;
    SignedShort maxPower;
    UnsignedShort channelIndex;
    UnsignedShort hopTableID;

    /**
     * 构造函数
     */
    public BaseRecorder() {
    }

    /**
     * 获取一个唯一消息ID
     * @return
     */
    private UnsignedInteger getUniqueMessageID() {
        return new UnsignedInteger(MessageID++);
    }

    /**
     * 初始化
     * @param host
     * @param configPath
     * @param ROSpecPath
     */
    public void initialize(String host,String configPath,String ROSpecPath) {
        this.connect(host); // 连接读写器
        this.disableAllROSpecs(); // 禁用所有ROSpec
        this.enableImpinjExtensions(); // 启用Impinj扩展功能
        this.factoryDefault(); // 恢复出厂设置
        this.getReaderCapabilities(); // 获取读写器能力信息
        this.getReaderConfiguration(); // 获取读写器配置信息

        this.setReaderConfiguration(configPath); // 设置读写器配置
        this.addRoSpec(ROSpecPath); // 添加ROSpec任务
        this.enable(); // 启用ROSpec
    }

    /**
     * 关闭
     */
    @Override
    public void close() {
        if (this.isClosed){
            return;
        }
        
        // this.stop();
        this.disableAllROSpecs();
        this.disconnect();
    }

    /**
     * 连接读写器
     * @param ip
     */
    private void connect(String host) {

        logger.info("Connecting to " + host);
        this.connection = new LLRPConnector(this, host);

        try {
            logger.info("Initiate LLRP connection to reader");

            ((LLRPConnector) connection).connect();
            this.isClosed = false;
        } catch (LLRPConnectionAttemptFailedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 断开连接
     */
    private void disconnect() {

        if(this.isClosed){
            return;
        }

        LLRPMessage response;
        
        try {
            logger.info("CLOSE_CONNECTION ...");

            CLOSE_CONNECTION close = new CLOSE_CONNECTION();
            close.setMessageID(getUniqueMessageID());

            response = connection.transact(close, 4000);

            // 
            StatusCode status = ((CLOSE_CONNECTION_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("CLOSE_CONNECTION was successful");
                this.isClosed = true;
            }
            else {
                logger.info(response.toXMLString());
                logger.info("CLOSE_CONNECTION Failed ... continuing anyway");
            }

        } catch (InvalidLLRPMessageException ex) {
            logger.error("CLOSE_CONNECTION: Received invalid response message");
        } catch (TimeoutException ex) {
            logger.info("CLOSE_CONNECTION Timeouts ... continuing anyway");
        }
    }

    /**
     * 停止所有ROSpec
     */
    private void disableAllROSpecs() {
        LLRPMessage response;

        try {
            logger.info("DISABLING all ROSpecs ...");

            DISABLE_ROSPEC disable = new DISABLE_ROSPEC();
            disable.setMessageID(getUniqueMessageID());
            disable.setROSpecID(new UnsignedInteger(0)); // 0 means all ROSpecs

            response = connection.transact(disable, 10000);

            StatusCode status = ((DISABLE_ROSPEC_RESPONSE) response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("Successfully disabled all ROSpecs");
            } else {
                logger.warn("Failed to disable all ROSpecs: " + status.toString());
            }
        } catch (Exception e) {
            logger.warn("Could not disable existing ROSpecs", e);
        }
    }

    /**
     * 启用扩展功能
     */
    private void enableImpinjExtensions() {
        LLRPMessage response;

        try {
            logger.info("IMPINJ_ENABLE_EXTENSIONS ...");

            IMPINJ_ENABLE_EXTENSIONS ena = new IMPINJ_ENABLE_EXTENSIONS();
            ena.setMessageID(getUniqueMessageID());

            response =  connection.transact(ena, 10000);

            StatusCode status = ((IMPINJ_ENABLE_EXTENSIONS_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("IMPINJ_ENABLE_EXTENSIONS was successful");
            }
            else {
                logger.info(response.toXMLString());
                logger.info("IMPINJ_ENABLE_EXTENSIONS Failure");
                System.exit(1);
            }
        } catch (InvalidLLRPMessageException ex) {
            logger.error("Could not process IMPINJ_ENABLE_EXTENSIONS response");
            System.exit(1);
        } catch (TimeoutException ex) {
            logger.error("Timeout Waiting for IMPINJ_ENABLE_EXTENSIONS response");
            System.exit(1);
        }
    }

    /**
     * 恢复出厂设置
     */
    private void factoryDefault() {
        LLRPMessage response;

        try {
            logger.info("SET_READER_CONFIG with factory default ...");

            SET_READER_CONFIG set = new SET_READER_CONFIG();
            set.setMessageID(getUniqueMessageID());
            set.setResetToFactoryDefault(new Bit(true)); // 设置为恢复出厂设置

            response =  connection.transact(set, 10000);

            StatusCode status = ((SET_READER_CONFIG_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("SET_READER_CONFIG Factory Default was successful");
            }
            else {
                logger.info(response.toXMLString());
                logger.info("SET_READER_CONFIG Factory Default Failure");
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 获取读写器能力信息
     */
    private void getReaderCapabilities() {
        LLRPMessage response;
        GET_READER_CAPABILITIES_RESPONSE gresp;

        GET_READER_CAPABILITIES get = new GET_READER_CAPABILITIES();
        GetReaderCapabilitiesRequestedData data = 
                new GetReaderCapabilitiesRequestedData(
                        GetReaderCapabilitiesRequestedData.All);
        get.setRequestedData(data);
        get.setMessageID(getUniqueMessageID());

        logger.info("Sending GET_READER_CAPABILITIES message  ...");
        try {
            response =  connection.transact(get, 10000);

            // check whether GET_CAPABILITIES addition was successful
            gresp = (GET_READER_CAPABILITIES_RESPONSE)response;
            StatusCode status = gresp.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("GET_READER_CAPABILITIES was successful");

                // get the info we need
                GeneralDeviceCapabilities dev_cap = gresp.getGeneralDeviceCapabilities();
                if ((dev_cap == null) ||
                    (!dev_cap.getDeviceManufacturerName().equals(new UnsignedInteger(25882)))) {
                    logger.error("DocSample4 must use Impinj model Reader, not " +
                        dev_cap.getDeviceManufacturerName().toString());
                    System.exit(1);
                }

                modelName = dev_cap.getModelName();
                logger.info("Found Impinj reader model " + modelName.toString());

                // get the max power level
                if( gresp.getRegulatoryCapabilities() != null) {
                    UHFBandCapabilities band_cap =
                        gresp.getRegulatoryCapabilities().getUHFBandCapabilities();

                    List<TransmitPowerLevelTableEntry> pwr_list = 
                        band_cap.getTransmitPowerLevelTableEntryList();

                    TransmitPowerLevelTableEntry entry =
                        pwr_list.get(pwr_list.size() - 1);

                    maxPowerIndex = entry.getIndex();
                    maxPower = entry.getTransmitPowerValue();
                    // LLRP sends power in dBm * 100
                    double d = ((double) maxPower.intValue())/100;

                    logger.info("Max power " + d +
                                " dBm at index " + maxPowerIndex.toString());
                }
            }
            else {
                logger.info(response.toXMLString());
                logger.info("GET_READER_CAPABILITIES failures");
                System.exit(1);
            }
        } catch (InvalidLLRPMessageException ex) {
            logger.error("Could not display response string");
        } catch (TimeoutException ex) {
            logger.error("Timeout waiting for GET_READER_CAPABILITIES response");
            System.exit(1);
        }
    }

    /**
     * 获取读写器配置信息
     */
    private void getReaderConfiguration() {
        LLRPMessage response;
        GET_READER_CONFIG_RESPONSE gresp;

        GET_READER_CONFIG get = new GET_READER_CONFIG();
        GetReaderConfigRequestedData data =
                new GetReaderConfigRequestedData(
                        GetReaderConfigRequestedData.All);
        get.setRequestedData(data);
        get.setMessageID(getUniqueMessageID());
        get.setAntennaID(new UnsignedShort(0));
        get.setGPIPortNum(new UnsignedShort(0));
        get.setGPOPortNum(new UnsignedShort(0));

        logger.info("Sending GET_READER_CONFIG message  ...");
        try {
            response =  connection.transact(get, 10000);

            // check whether GET_CAPABILITIES addition was successful
            gresp = (GET_READER_CONFIG_RESPONSE)response;
            StatusCode status = gresp.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("GET_READER_CONFIG was successful");

                List<AntennaConfiguration> alist = gresp.getAntennaConfigurationList();

                if(!alist.isEmpty()) {
                    AntennaConfiguration a_cfg = alist.get(0);
                    channelIndex  = a_cfg.getRFTransmitter().getChannelIndex();
                    hopTableID =  a_cfg.getRFTransmitter().getHopTableID();
//                    UnsignedShort p =  a_cfg.getRFTransmitter().getTransmitPower();
                    logger.info("ChannelIndex " + channelIndex.toString() +
                                " hopTableID " + hopTableID.toString());
                } else {
                    logger.error("Could not find antenna configuration");
                    System.exit(1);
                }
            }
            else {
                logger.info(response.toXMLString());
                logger.info("GET_READER_CONFIG failures");
                System.exit(1);
            }
        } catch (InvalidLLRPMessageException ex) {
            logger.error("Could not display response string");
        } catch (TimeoutException ex) {
            logger.error("Timeout waiting for GET_READER_CONFIG response");
            System.exit(1);
        }
    }

    /**
     * 通过Objects构建ROSpec
     * @param xml
     */
    private ADD_ROSPEC buildROSpecFromObjects() {
        logger.info("Building ADD_ROSPEC message from scratch ...");
        ADD_ROSPEC addRoSpec = new ADD_ROSPEC();
        addRoSpec.setMessageID(getUniqueMessageID());

        rospec = new ROSpec();

        // set up the basic info for the RO Spec.
        rospec.setCurrentState(new ROSpecState(ROSpecState.Disabled));
        rospec.setPriority(new UnsignedByte(0));
        rospec.setROSpecID(new UnsignedInteger(12345));

        // set the start and stop conditions for the ROSpec.
        // For now, we will start and stop manually 
        ROBoundarySpec boundary = new ROBoundarySpec();
        ROSpecStartTrigger start = new ROSpecStartTrigger();
        ROSpecStopTrigger stop = new ROSpecStopTrigger();
        start.setROSpecStartTriggerType(new ROSpecStartTriggerType(ROSpecStartTriggerType.Null));
        stop.setROSpecStopTriggerType(new ROSpecStopTriggerType(ROSpecStopTriggerType.Null));
        stop.setDurationTriggerValue(new UnsignedInteger(0));
        boundary.setROSpecStartTrigger(start);
        boundary.setROSpecStopTrigger(stop);
        rospec.setROBoundarySpec(boundary);

        // set up what we want to do in the ROSpec. In this case
        // build the simples inventory on all channels using defaults
        AISpec aispec = new AISpec();

        // what antennas to use.
        UnsignedShortArray ants = new UnsignedShortArray();
        ants.add(new UnsignedShort(0)); // 0 means all antennas
        aispec.setAntennaIDs(ants);

        // set up the AISpec stop condition and options for inventory
        AISpecStopTrigger aistop = new AISpecStopTrigger();
        aistop.setAISpecStopTriggerType(new AISpecStopTriggerType(AISpecStopTriggerType.Null));
        aistop.setDurationTrigger(new UnsignedInteger(0));
        aispec.setAISpecStopTrigger(aistop);

        // set up any override configuration.  none in this case
        InventoryParameterSpec ispec = new InventoryParameterSpec();
        ispec.setAntennaConfigurationList(null);
        ispec.setInventoryParameterSpecID(new UnsignedShort(23));
        ispec.setProtocolID(new AirProtocols(AirProtocols.EPCGlobalClass1Gen2));
        List<InventoryParameterSpec> ilist = new ArrayList<InventoryParameterSpec>();
        ilist.add(ispec);

        aispec.setInventoryParameterSpecList(ilist);
        List<SpecParameter> slist = new ArrayList<SpecParameter>();
        slist.add(aispec);
        rospec.setSpecParameterList(slist);

        addRoSpec.setROSpec(rospec);

        return addRoSpec;
    }

    /**
     * 通过配置文件构建ROSpec
     * @return
     */
    private ADD_ROSPEC buildROSpecFromFile(String path) {
        logger.info("Loading ADD_ROSPEC message from file ADD_ROSPEC.xml ...");
        try {
            LLRPMessage addRospec = Util.loadXMLLLRPMessage(
                new File(path));
            // TODO make sure this is an ADD_ROSPEC message 

            return (ADD_ROSPEC) addRospec;
        } catch (FileNotFoundException ex) {
            logger.error("Could not find file");
            System.exit(1);
        } catch (IOException ex) {
            logger.error("IO Exception on file");
            System.exit(1);
        } catch (JDOMException ex) {
            logger.error("Unable to convert LTK-XML to DOM");
            System.exit(1);
        } catch (InvalidLLRPMessageException ex) {
            logger.error("Unable to convert LTK-XML to Internal Object");
            System.exit(1);
        }
        return null;
    }

    /**
     * 设置Reader配置
     */
    private void setReaderConfiguration(String path) {
        LLRPMessage response;
        
        logger.info("Loading SET_READER_CONFIG message from file SET_READER_CONFIG.xml ...");

        try {
            LLRPMessage setConfigMsg = Util.loadXMLLLRPMessage(
            new File(path));
            // TODO make sure this is an SET_READER_CONFIG message

            response = connection.transact(setConfigMsg, 10000);
            
            // check whetherSET_READER_CONFIG addition was successful
            StatusCode status = ((SET_READER_CONFIG_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                    logger.info("SET_READER_CONFIG was successful");
            }
            else {
                    logger.info(response.toXMLString());
                    logger.info("SET_READER_CONFIG failures");
                    System.exit(1);
            }

        } catch (TimeoutException ex) {
            logger.error("Timeout waiting for SET_READER_CONFIG response");
            System.exit(1);
        } catch (FileNotFoundException ex) {
            logger.error("Could not find file");
            System.exit(1);
        } catch (IOException ex) {
            logger.error("IO Exception on file");
            System.exit(1);
        } catch (JDOMException ex) {
            logger.error("Unable to convert LTK-XML to DOM");
            System.exit(1);
        } catch (InvalidLLRPMessageException ex) {
            logger.error("Unable to convert LTK-XML to Internal Object");
            System.exit(1);
        }
    }

    /**
     * 添加ROSpec
     * @param xml
     */
    private void addRoSpec(String path) {
        LLRPMessage response;

        ADD_ROSPEC addRospec = null;

        if(path != null) {
            addRospec = buildROSpecFromFile(path);
        } else {
            addRospec = buildROSpecFromObjects();
        }

        addRospec.setMessageID(getUniqueMessageID());
        rospec = addRospec.getROSpec();
        
        logger.info("Sending ADD_ROSPEC message  ...");
        try {
            response =  connection.transact(addRospec, 10000);

            // check whether ROSpec addition was successful
            StatusCode status = ((ADD_ROSPEC_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("ADD_ROSPEC was successful");

                // 打印ROSpec信息
                logger.info("ROSpecID: " + rospec.getROSpecID());
                List<SpecParameter> specList = rospec.getSpecParameterList();
                if (!specList.isEmpty()) {
                    for  (SpecParameter specParam : specList) {
                        logger.info(specParam.toString());
                    }
                }
            }
            else {
                logger.info(response.toXMLString());
                logger.info("ADD_ROSPEC failures");
                System.exit(1);
            }
        } catch (InvalidLLRPMessageException ex) {
            logger.error("Could not display response string");
        } catch (TimeoutException ex) {
            logger.error("Timeout waiting for ADD_ROSPEC response");
            System.exit(1);
        }
    }

    /**
     * 启用ROSpec
     */
    private void enable() {
        LLRPMessage response;
        try {
            // factory default the reader
            logger.info("ENABLE_ROSPEC ...");
            ENABLE_ROSPEC ena = new ENABLE_ROSPEC();
            ena.setMessageID(getUniqueMessageID());
            ena.setROSpecID(rospec.getROSpecID());

            response =  connection.transact(ena, 10000);

            // check whether ROSpec addition was successful
            StatusCode status = ((ENABLE_ROSPEC_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("ENABLE_ROSPEC was successful");
            }
            else {
                logger.error(response.toXMLString());
                logger.info("ENABLE_ROSPEC_RESPONSE failed ");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 启动ROSpec
     */
    public void start() {
        LLRPMessage response;
        try {
            logger.info("START_ROSPEC ...");
            START_ROSPEC start = new START_ROSPEC();
            start.setMessageID(getUniqueMessageID());
            start.setROSpecID(rospec.getROSpecID());

            response =  connection.transact(start, 10000);

            // check whether ROSpec addition was successful
            StatusCode status = ((START_ROSPEC_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("START_ROSPEC was successful");
            }
            else {
                logger.error(response.toXMLString());
                logger.info("START_ROSPEC_RESPONSE failed ");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 停止ROSpec
     */
    public void stop() {
        LLRPMessage response;
        try {
            logger.info("STOP_ROSPEC ...");
            STOP_ROSPEC stop = new STOP_ROSPEC();
            stop.setMessageID(getUniqueMessageID());
            stop.setROSpecID(rospec.getROSpecID());

            response =  connection.transact(stop, 10000);

            // check whether ROSpec addition was successful
            StatusCode status = ((STOP_ROSPEC_RESPONSE)response).getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success"))) {
                logger.info("STOP_ROSPEC was successful");
            }
            else {
                logger.error(response.toXMLString());
                logger.info("STOP_ROSPEC_RESPONSE failed ");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void errorOccured(String s) {
        logger.error(s);
    }

    @Override
    public void messageReceived(LLRPMessage message) {
        // convert all messages received to LTK-XML representation
        // and print them to the console

        // logger.debug("Received " + message.getName() + " message asychronously");

        if (message.getTypeNum() == RO_ACCESS_REPORT.TYPENUM) {
            RO_ACCESS_REPORT report = (RO_ACCESS_REPORT) message;

            List<TagReportData> tdlist = report.getTagReportDataList();

            for (TagReportData tr : tdlist) {
                logOneTagReport(tr);
            }

            List<Custom> clist = report.getCustomList();
            for (Custom cust : clist) {
                logOneCustom(cust);
            }
            

        } else if (message.getTypeNum() == READER_EVENT_NOTIFICATION.TYPENUM) {
            // TODO 
        }
    }

    /**
     * 处理Custom信息
     * @param cust
     */
    protected void logOneCustom(Custom cust) {

        if(!cust.getVendorIdentifier().equals(25882)) {
            logger.error("Non Impinj Extension Found in message");
            return;
        }
    }

    /**
     * 处理TagReportData信息
     * @param tr
     */
    protected void logOneTagReport(TagReportData tr) {
        // logger.info(tr.toString());
    }
}
