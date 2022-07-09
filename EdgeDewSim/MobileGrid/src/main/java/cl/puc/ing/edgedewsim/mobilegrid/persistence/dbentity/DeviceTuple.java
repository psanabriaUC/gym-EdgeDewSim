package cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.IDevicePersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IPersisterFactory;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class DeviceTuple {

    private static IDevicePersister dp = null;

    private int device_id;
    private long mips;
    private long battery_capacity;
    private String name;
    private int sim_id;
    private long join_topology_time;
    private long left_topology_time;
    private boolean stored;

    public DeviceTuple() {
        stored = false;
    }

    public DeviceTuple(String name, long mips, long battery_capacity, int sim_id) {
        this.name = name;
        this.mips = mips;
        this.battery_capacity = battery_capacity;
        this.join_topology_time = -1;
        this.left_topology_time = -1;
        this.sim_id = sim_id;
        this.stored = false;
    }

    public static void setIPersisterFactory(@NotNull IPersisterFactory pf) {
        dp = pf.getDevicePersister();
    }

    public void persist(SQLSession session) {
        try {
            dp.insertDevice(session, this);
            this.stored = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getDevice_id() {
        return device_id;
    }

    public void setDevice_id(int device_id) {
        this.device_id = device_id;
    }

    public long getMips() {
        return mips;
    }

    public void setMips(long mips) {
        this.mips = mips;
    }

    public long getBattery_capacity() {
        return battery_capacity;
    }

    public void setBattery_capacity(long battery_capacity) {
        this.battery_capacity = battery_capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSim_id() {
        return sim_id;
    }

    public void setSim_id(int simulation_id) {
        this.sim_id = simulation_id;
    }

    public long getJoin_topology_time() {
        return join_topology_time;
    }

    public void setJoin_topology_time(long join_topology_time) {
        this.join_topology_time = join_topology_time;
    }

    public long getLeft_topology_time() {
        return left_topology_time;
    }

    public void setLeft_topology_time(long left_topology_time) {
        this.left_topology_time = left_topology_time;
    }

    public boolean isStored() {
        return stored;
    }
}
