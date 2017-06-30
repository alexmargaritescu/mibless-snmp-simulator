package ro.happyhyppo.sim.snmp.net;

public class NS3000 extends NetworkElement {

    public NS3000(String ipAddress) {
        super(ipAddress);
    }

    @Override
    protected void init() {
        community.setReadOnly("public");
        community.setReadWrite("private");
        addOid("1.3.6.1.2.1.1.1", "NS3000".getBytes(), 4, true);
    }

}
