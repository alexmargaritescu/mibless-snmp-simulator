package ro.happyhyppo.sim.snmp.agent;

public class VarBind {

    private String instance;

    private int type;

    private String value;

    private boolean readOnly;

    public VarBind(String instance, int type, String value) {
        this.instance = instance;
        this.type = type;
        this.value = value;
    }

    private VarBind(String instance, int type, Object value, boolean readOnly) {
        this.instance = instance;
        this.type = type;
        switch (type) {
        case 4:
            this.value = new String((byte[]) value);
            break;
        case 6:
            this.value = new String((byte[]) value);
            break;
        default:
            this.value = value.toString();
        }
        this.readOnly = readOnly;
    }

    public VarBind(VarBind varBind) {
        this.instance = varBind.getInstance();
        this.type = varBind.getType();
        this.value = varBind.getValue();
    }

    public VarBind(String oid, String index, Object value, byte type, boolean readOnly) {
        this(oid + "." + index, type, value, readOnly);
    }

    public String getInstance() {
        return instance;
    }

    public int getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isEndOfMib() {
        return type == 130;
    }

    public void setEndOfMib() {
        type = 130;
    }

    public void setNoSuchInstance() {
        type = 129;
    }

    public boolean isNoSuchInstance() {
        return type == 129;
    }

    public String toString() {
        return instance + "=" + value + (type == 130 ? " EndOfMib" : "") + (type == 129 ? " NoSuchInstance" : "");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instance == null) ? 0 : instance.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VarBind other = (VarBind) obj;
        if (instance == null) {
            if (other.instance != null)
                return false;
        } else if (!instance.equals(other.instance))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
