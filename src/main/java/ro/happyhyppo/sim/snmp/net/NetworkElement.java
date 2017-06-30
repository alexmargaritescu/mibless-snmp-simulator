package ro.happyhyppo.sim.snmp.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import ro.happyhyppo.sim.snmp.agent.Community;
import ro.happyhyppo.sim.snmp.agent.VarBind;

public abstract class NetworkElement {

    String ipAddress;

    byte[] macAddress;

    Community community;

    Map<String, VarBind> oidMap = new TreeMap<String, VarBind>();

    public NetworkElement(String ipAddress) {
        this.ipAddress = ipAddress;
        macAddress = getMacAddress();
        community = new Community();
        init();
    }

    abstract void init();

    void addOid(String oid, String value, boolean readOnly) {
        String index = value.length() + ".";
        for (int i = 0; i < value.length(); i++) {
            index += (byte) value.charAt(i);
            if (i < value.length() - 1) {
                index += ".";
            }
        }
        oidMap.put(oid + "." + index, new VarBind(oid, index, value, (byte) 4, readOnly));
    }

    void addOid(String oid, Object value, int type, boolean readOnly) {
        oidMap.put(oid + ".0", new VarBind(oid, "0", value, (byte) type, readOnly));
    }

    void addOid(String oid, String index, Object value, int type, boolean readOnly) {
        oidMap.put(oid + "." + index, new VarBind(oid, index, value, (byte) type, readOnly));
    }

    private byte[] getMacAddress() {
        StringTokenizer tokenizer = new StringTokenizer(ipAddress, ".");
        List<String> elements = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            elements.add(tokenizer.nextToken());
        }
        int ip1 = Integer.parseInt(elements.get(0));
        int ip2 = Integer.parseInt(elements.get(1));
        int ip3 = Integer.parseInt(elements.get(2));
        int ip4 = Integer.parseInt(elements.get(3));
        return new byte[] { 0x00, (byte) 0xe0, (byte) ip1, (byte) ip2, (byte) ip3, (byte) ip4 };
    }

    public Map<String, VarBind> getVarBinds() {
        Map<String, VarBind> varBinds = new TreeMap<String, VarBind>();
        varBinds.putAll(oidMap);
        return varBinds;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Community getCommunity() {
        return community;
    }

}
