package ro.happyhyppo.sim.snmp.agent;

public interface VarBindFactory {

    public abstract VarBind[] decodeVarBindList(Object data, int operation);

    public abstract Object encodeVarBindList(VarBind[] varBindList);

}