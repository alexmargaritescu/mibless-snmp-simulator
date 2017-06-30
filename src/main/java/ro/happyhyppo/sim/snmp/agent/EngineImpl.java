package ro.happyhyppo.sim.snmp.agent;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.sun.management.comm.SnmpAdaptorServer;
import com.sun.management.snmp.SnmpDefinitions;
import com.sun.management.snmp.SnmpIpAddress;
import com.sun.management.snmp.SnmpMsg;
import com.sun.management.snmp.SnmpOid;
import com.sun.management.snmp.SnmpPdu;
import com.sun.management.snmp.SnmpPduBulk;
import com.sun.management.snmp.SnmpPduFactory;
import com.sun.management.snmp.SnmpPduFactoryBER;
import com.sun.management.snmp.SnmpPduRequest;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.SnmpTimeticks;
import com.sun.management.snmp.SnmpTooBigException;
import com.sun.management.snmp.SnmpVarBind;
import com.sun.management.snmp.SnmpVarBindList;

import ro.happyhyppo.sim.snmp.net.NetworkElement;

public class EngineImpl implements SnmpPduFactory {

    private NetworkElement networkElement;

    private SnmpAdaptorServer snmpAdaptor = null;

    private SnmpPduFactory berFactory;

    private Map<Integer, SnmpPdu> reqMap = new HashMap<Integer, SnmpPdu>();

    private VarBindFactory varBindFactory;

    private Mib mib;

    public EngineImpl(NetworkElement networkElement) throws Exception {
        this.networkElement = networkElement;
        int port = 161;
        if (networkElement.getIpAddress().equals("127.0.0.1")) {
            snmpAdaptor = new SnmpAdaptorServer(false, port, InetAddress.getByName("127.0.0.1"));
        } else {
            snmpAdaptor = new SnmpAdaptorServer(false, port, InetAddress.getByName(networkElement.getIpAddress()));
        }
        berFactory = new SnmpPduFactoryBER();
        snmpAdaptor.setPduFactory(this);
        snmpAdaptor.setBufferSize(4096);
        // snmpAdaptor.setEnterpriseOid(".1.3.6.1.4.1.37576.2.2.0");
        varBindFactory = new VarBindFactoryImpl();
        mib = new MibImpl(networkElement);
    }

    public void shutdownAgent() {
        if (snmpAdaptor.isActive()) {
            while (snmpAdaptor.getActiveClientCount() > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // don't care
                }
            }
            snmpAdaptor.stop();
        }
    }

    public void startAgent() {
        if (!snmpAdaptor.isActive()) {
            snmpAdaptor.start();
        }
        sendV1Trap();
    }

    public SnmpPdu getCachedSnmpPdu(SnmpPdu pdu) {
        SnmpPdu myPdu = null;
        synchronized (reqMap) {
            myPdu = reqMap.remove(pdu.requestId);
        }
        if (myPdu != null) {
            return myPdu;
        } else {
            return pdu;
        }
    }

    /**
     * This method is called when a PDU is sent.
     */
    public SnmpMsg encodeSnmpPdu(SnmpPdu pdu, int maxPktSize) throws SnmpStatusException, SnmpTooBigException {
        return berFactory.encodeSnmpPdu(getCachedSnmpPdu(pdu), maxPktSize);
    }

    /**
     * This method is called when a PDU is received.
     */
    public SnmpPdu decodeSnmpPdu(SnmpMsg msg) throws SnmpStatusException {
        return this.decodeSnmpPdu(berFactory.decodeSnmpPdu(msg));
    }

    public SnmpPdu decodeSnmpPdu(SnmpPdu pdu) throws SnmpStatusException {
        VarBind[] varBindListIn = varBindFactory.decodeVarBindList(pdu.varBindList, pdu.type);
        VarBind[] varBindListOut = new VarBind[0];
        SnmpPduRequest pduRequest = (SnmpPduRequest) pdu;
        String comm = new String(pduRequest.community);
        if (pdu.type == 160 || pdu.type == 161 || pdu.type == 166) {
            if (!networkElement.getCommunity().canRead(comm)) {
                throw new SnmpStatusException(SnmpStatusException.noAccess);
            }
        } else if (pdu.type == 163) {
            if (!networkElement.getCommunity().canWrite(comm)) {
                throw new SnmpStatusException(SnmpStatusException.noAccess);
            }
        }
        SnmpPdu myPdu = null;
        if (pdu.type == 160) { // get
            myPdu = ((SnmpPduRequest) pdu).getResponsePdu();
            varBindListOut = mib.getVarBindList(varBindListIn);
            for (int i = 0; i < varBindListOut.length; i++) {
                if (varBindListOut[i].isNoSuchInstance() && pduRequest.version == 0) {
                    varBindListOut = varBindListIn;
                    break;
                }
            }
        } else if (pdu.type == 161) { // get-next
            myPdu = ((SnmpPduRequest) pdu).getResponsePdu();
            varBindListOut = mib.getNextVarBindList(varBindListIn);
        } else if (pdu.type == 165) { // bulk
            myPdu = ((SnmpPduBulk) pdu).getResponsePdu();
            varBindListOut = mib.getBulkVarBindList(varBindListIn, ((SnmpPduBulk) pdu).maxRepetitions);
            pdu = ((SnmpPduBulk) pdu).getResponsePdu();
            pdu.type = 160; // to fool the agent (as we do not implement any
                            // MIB), we will insert our own PDU anyway
            pdu.varBindList = new SnmpVarBind[0];
        } else if (pdu.type == 163) { // set
            varBindListOut = mib.getVarBindList(varBindListIn);
            for (int i = 0; i < varBindListOut.length; i++) {
                if (varBindListOut[i].isNoSuchInstance() || varBindListOut[i].isReadOnly()) {
                    if (pduRequest.version == 0) {
                        pduRequest.setErrorStatus(SnmpDefinitions.snmpRspNoSuchName);
                    } else {
                        pduRequest.setErrorStatus(SnmpDefinitions.snmpRspNotWritable);
                    }
                    pduRequest.setErrorIndex(i);
                    return pdu;
                } else if (varBindListIn[i].getType() != varBindListOut[i].getType()) {
                    pduRequest.setErrorStatus(SnmpDefinitions.snmpRspWrongType);
                    pduRequest.setErrorIndex(i);
                    return pdu;
                }
            }
            myPdu = ((SnmpPduRequest) pdu).getResponsePdu();
            varBindListOut = mib.setVarBindList(varBindListIn);
        }
        myPdu.varBindList = (SnmpVarBind[]) varBindFactory.encodeVarBindList(varBindListOut);
        synchronized (reqMap) {
            reqMap.put(pdu.requestId, myPdu);
        }
        return pdu;
    }

    public void sendV1Trap() {
        // SnmpVarBindList varBindList = new SnmpVarBindList();
        // varBindList.addVarBind(new SnmpVarBind(new
        // SnmpOid(".1.3.6.1.4.1.37576.2.2.2.1.1.7"), new SnmpInt(2)));
        try {
            InetAddress trapManager = InetAddress.getByName("127.0.0.1");
            snmpAdaptor.setTrapPort(10162);
            SnmpOid enterpriseOid = new SnmpOid(".1.3.6.1.4.1.37576.2.2.0");
            SnmpTimeticks timeticks = new SnmpTimeticks(System.currentTimeMillis());
            snmpAdaptor.snmpV1Trap(trapManager, new SnmpIpAddress(networkElement.getIpAddress()), "public",
                    enterpriseOid, 0, 0, new SnmpVarBindList(), timeticks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
