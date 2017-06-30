package ro.happyhyppo.sim.snmp;

import ro.happyhyppo.sim.snmp.agent.SnmpAgent;
import ro.happyhyppo.sim.snmp.net.NS3000;

public class MiblessAgent {

    public static void main(String[] args) {
        try {
            new Thread(new SnmpAgent(new NS3000("127.0.0.1"))).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
