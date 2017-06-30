package ro.happyhyppo.sim.snmp.agent;

import java.util.List;
import java.util.Map;

public interface Mib
{
    public void addVarBind(VarBind varBind);

    public void addAction(VarBind varBind, List<VarBind> targets, String delay);

    public VarBind[] getVarBindList(VarBind[] inList);

    public VarBind[] getNextVarBindList(VarBind[] inList);

    public VarBind[] getBulkVarBindList(VarBind[] inList, int maxRepetitions);

    public VarBind[] setVarBindList(VarBind[] inList);

    public void addvarBinds(Map<String, ? extends VarBind> varBinds);
}
