package ro.happyhyppo.sim.snmp.agent;

import ro.happyhyppo.sim.snmp.net.NetworkElement;

public class SnmpAgent implements Runnable {

    private EngineImpl engine;

    private NetworkElement networkElement;

    public SnmpAgent(NetworkElement networkElement) throws Exception {
        this.networkElement = networkElement;
        engine = new EngineImpl(networkElement);
    }

    public void stop() {
        engine.shutdownAgent();
    }

    public void run() {
        System.out.println("Starting SNMP agent for " + networkElement.getIpAddress());
        engine.startAgent();
    }
}
