package ro.happyhyppo.sim.snmp.agent;

import com.sun.management.snmp.SnmpCounter;
import com.sun.management.snmp.SnmpCounter64;
import com.sun.management.snmp.SnmpDataTypeEnums;
import com.sun.management.snmp.SnmpGauge;
import com.sun.management.snmp.SnmpInt;
import com.sun.management.snmp.SnmpIpAddress;
import com.sun.management.snmp.SnmpOid;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.SnmpString;
import com.sun.management.snmp.SnmpTimeticks;
import com.sun.management.snmp.SnmpVarBind;

public class VarBindFactoryImpl extends AbstractVarBindFactory {

    public VarBind[] decodeVarBindList(Object data, int operation) {
        SnmpVarBind[] snmpVarBindList = (SnmpVarBind[]) data;
        VarBind[] varBindList = new VarBind[snmpVarBindList.length];
        for (int i = 0; i < snmpVarBindList.length; i++) {
            int type = getType(snmpVarBindList[i]);
            varBindList[i] = new VarBind(snmpVarBindList[i].getOid().toString(), type,
                    snmpVarBindList[i].getStringValue());
        }
        return varBindList;
    }

    public Object encodeVarBindList(VarBind[] varBindList) {
        SnmpVarBind[] snmpVarBindList = new SnmpVarBind[varBindList.length];
        for (int i = 0; i < snmpVarBindList.length; i++) {
            try {
                snmpVarBindList[i] = new SnmpVarBind(varBindList[i].getInstance());
            } catch (SnmpStatusException e) {
                // shall we do more here?
                e.printStackTrace();
            }
            setValue(snmpVarBindList[i], varBindList[i].getType(), varBindList[i].getValue());
        }
        return snmpVarBindList;
    }

    private int getType(SnmpVarBind snmpVarBind) {
        if (snmpVarBind.getSnmpValue() instanceof SnmpInt) {
            return SnmpDataTypeEnums.IntegerTag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpString) {
            return SnmpDataTypeEnums.OctetStringTag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpIpAddress) {
            return SnmpDataTypeEnums.IpAddressTag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpGauge) {
            return SnmpDataTypeEnums.GaugeTag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpCounter) {
            return SnmpDataTypeEnums.CounterTag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpCounter64) {
            return SnmpDataTypeEnums.Counter64Tag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpTimeticks) {
            return SnmpDataTypeEnums.TimeticksTag;
        } else if (snmpVarBind.getSnmpValue() instanceof SnmpOid) {
            return SnmpDataTypeEnums.ObjectIdentifierTag;
        } else
            return -1;
    }

    private void setValue(SnmpVarBind snmpVarBind, int type, String value) {
        switch (type) {
        case SnmpDataTypeEnums.IntegerTag:
            snmpVarBind.setSnmpIntValue(Long.parseLong(value));
            break;
        case SnmpDataTypeEnums.OctetStringTag:
            snmpVarBind.setSnmpStringValue(value);
            break;
        case SnmpDataTypeEnums.IpAddressTag:
            snmpVarBind.setSnmpIpAddressValue(value);
            break;
        case SnmpDataTypeEnums.GaugeTag:
            snmpVarBind.setSnmpGaugeValue(Long.parseLong(value));
            break;
        case SnmpDataTypeEnums.CounterTag:
            snmpVarBind.setSnmpCounterValue(Long.parseLong(value));
            break;
        case SnmpDataTypeEnums.Counter64Tag:
            snmpVarBind.setSnmpCounter64Value(Long.parseLong(value));
            break;
        case SnmpDataTypeEnums.TimeticksTag:
            snmpVarBind.setSnmpTimeticksValue(Long.parseLong(value));
            break;
        case SnmpDataTypeEnums.ObjectIdentifierTag:
            snmpVarBind.setSnmpOidValue(value);
            break;
        case SnmpVarBind.errNoSuchObjectTag:
            snmpVarBind.setSnmpValue(SnmpVarBind.noSuchObject);
            break;
        case SnmpVarBind.errNoSuchInstanceTag:
            snmpVarBind.setSnmpValue(SnmpVarBind.noSuchInstance);
            break;
        case SnmpVarBind.errEndOfMibViewTag:
            snmpVarBind.setSnmpValue(SnmpVarBind.endOfMibView);
            break;
        default:
            snmpVarBind.setSnmpValue(SnmpVarBind.noSuchObject);
        }
    }
}
