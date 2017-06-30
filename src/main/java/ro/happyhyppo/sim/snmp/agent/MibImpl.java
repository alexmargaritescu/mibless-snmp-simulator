package ro.happyhyppo.sim.snmp.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ro.happyhyppo.sim.snmp.net.NetworkElement;

public class MibImpl implements Mib {

    private final Comparator<String> comparator = new OidComparator();

    private final Map<String, VarBind> varBinds = Collections.synchronizedMap(new TreeMap<String, VarBind>(comparator));

    private final Map<String, Action> actions = new HashMap<String, Action>();

    public MibImpl(NetworkElement networkElement) {
        addvarBinds(networkElement.getVarBinds());
    }

    public void addVarBind(final VarBind varBind) {
        varBinds.put(varBind.getInstance(), varBind);
    }

    public void addAction(final VarBind varBind, final List<VarBind> targets, final String delay) {
        actions.put(varBind.getInstance(), new Action(varBind, targets, delay));
    }

    public void addvarBinds(final Map<String, ? extends VarBind> varBinds) {
        this.varBinds.putAll(varBinds);
    }

    public VarBind[] getVarBindList(final VarBind[] inList) {
        final VarBind[] outList = new VarBind[inList.length];
        for (int i = 0; i < outList.length; i++) {
            if (varBinds.containsKey(inList[i].getInstance())) {
                outList[i] = varBinds.get(inList[i].getInstance());
            }
            else {
                outList[i] = new VarBind(inList[i]);
                outList[i].setNoSuchInstance();
                System.err.println("GET not found: " + inList[i].getInstance());
            }
        }
        return outList;
    }

    public VarBind[] getBulkVarBindList(final VarBind[] inList, final int maxRepetitions) {
        final List<VarBind> snap = new ArrayList<VarBind>();
        synchronized (varBinds) {
            snap.addAll(varBinds.values());
        }
        final List<VarBind> outList = new ArrayList<VarBind>();
        int count = 0;
        for (int i = 0; i < inList.length; i++) {
            final List<VarBind> subtree = new ArrayList<VarBind>();
            subtree.addAll(snap);
            if (count == maxRepetitions * inList.length) {
                break;
            }
            VarBind nextVarBind = getNextVarBind(inList[i], subtree);
            outList.add(nextVarBind);
            count++;
            while (!nextVarBind.isEndOfMib() && count != maxRepetitions * inList.length) {
                nextVarBind = getNextVarBindList(new VarBind[] { nextVarBind }, true)[0];
                outList.add(nextVarBind);
                count++;
            }
        }
        return outList.toArray(new VarBind[0]);
    }

    private VarBind getNextVarBind(final VarBind in, final List<VarBind> subtree) {
        VarBind out = null;
        for (final VarBind varBind : subtree) {
            if (comparator.compare(varBind.getInstance(), in.getInstance()) > 0) {
                out = varBind;
                subtree.remove(varBind);
                break;
            }
        }
        if (out == null) {
            out = new VarBind(in);
            out.setEndOfMib();
        }
        return out;
    }

    private VarBind[] getNextVarBindList(final VarBind[] inList, final boolean forBulk) {
        final List<VarBind> temp = new ArrayList<VarBind>();
        synchronized (varBinds) {
            temp.addAll(varBinds.values());
        }
        final VarBind[] outList = new VarBind[inList.length];
        for (int i = 0; i < inList.length; i++) {
            boolean found = false;
            for (final VarBind varBind : temp) {
                if (comparator.compare(varBind.getInstance(), inList[i].getInstance()) > 0) {
                    found = true;
                    outList[i] = varBind;
                    break;
                }
            }
            if (!found) {
                outList[i] = new VarBind(inList[i]);
                outList[i].setEndOfMib();
            }
        }
        return outList;
    }

    public VarBind[] getNextVarBindList(final VarBind[] inList) {
        return getNextVarBindList(inList, false);
    }

    public VarBind[] setVarBindList(final VarBind[] inList) {
        final VarBind[] outList = new VarBind[inList.length];
        for (int i = 0; i < outList.length; i++) {
            outList[i] = new VarBind(inList[i]);
            varBinds.put(inList[i].getInstance(), outList[i]);
            if (actions.containsKey(inList[i].getInstance())) {
                final Action action = actions.get(inList[i].getInstance());
                if (!inList[i].getValue().toString().equals(action.varBind.getValue())) {
                    continue;
                }
                new Thread() {
                    @Override
                    public void run() {
                        if (action.delay != null) {
                            try {
                                Thread.currentThread();
                                Thread.sleep(Integer.parseInt(action.delay) * 1000);
                            }
                            catch (final NumberFormatException e) {
                                e.printStackTrace();
                            }
                            catch (final InterruptedException e) {
                            }
                        }
                        for (int j = 0; j < action.targets.size(); j++) {
                            synchronized (varBinds) {
                                VarBind varBind = varBinds.get(action.targets.get(j).getInstance());
                                if (varBind == null) {
                                    varBind = new VarBind(action.targets.get(j));
                                    addVarBind(varBind);
                                }
                                varBind.setValue(action.targets.get(j).getValue());
                            }
                        }

                    }
                }.start();
            }
        }
        return outList;
    }

    class Action {
        VarBind varBind;

        List<VarBind> targets;

        String delay;

        Action(final VarBind varBind, final List<VarBind> targets, final String delay) {
            this.varBind = varBind;
            this.targets = targets;
            this.delay = delay;
        }

        @Override
        public String toString() {
            return varBind.toString();
        }

    }

    class OidComparator implements Comparator<String> {

        public int compare(final String o1, final String o2) {
            try {
                final String[] index1 = o1.replace('.', '_').split("_");
                final String[] index2 = o2.replace('.', '_').split("_");
                int i = 0;
                for (i = 0; i < index1.length - 1 && i < index2.length - 1; i++) {
                    if (Integer.parseInt(index1[i]) != Integer.parseInt(index2[i])) {
                        break;
                    }
                }
                if (Integer.parseInt(index1[i]) > Integer.parseInt(index2[i])) {
                    return 1;
                }
                else if (Integer.parseInt(index1[i]) < Integer.parseInt(index2[i])) {
                    return -1;
                }
                else {
                    if (o1.length() == o2.length()) {
                        return 0;
                    }
                    else if (o1.length() > o2.length()) {
                        return 1;
                    }
                    else {
                        return -1;
                    }
                }
            }
            catch (final NumberFormatException e) {
                e.printStackTrace();
                throw e;
            }
        }

    }

    public String print(final VarBind[] varBinds) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (final VarBind varBind : varBinds) {
            stringBuilder.append(varBind);
            stringBuilder.append(',');
        }
        if (varBinds.length > 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

}
