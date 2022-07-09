package cl.puc.ing.edgedewsim.server.data;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class DeviceData implements Data {
    @NotNull
    private String name = "";
    private final long id;
    private long mips;
    private int uptime;
    private int nJobs;
    private int remainingBattery;
    private double cpuUsage;
    private boolean hasBattery;
    private long assignedJobs;

    public DeviceData(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public long getMips() {
        return mips;
    }

    public void setMips(long mips) {
        this.mips = mips;
    }

    public int getUptime() {
        return uptime;
    }

    public void setUptime(int uptime) {
        this.uptime = uptime;
    }

    public int getNJobs() {
        return nJobs;
    }

    public void setNJobs(int nJobs) {
        this.nJobs = nJobs;
    }

    public int getRemainingBattery() {
        return remainingBattery;
    }

    public void setRemainingBattery(int remainingBattery) {
        this.remainingBattery = remainingBattery;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public boolean isHasBattery() {
        return hasBattery;
    }

    public void setHasBattery(boolean hasBattery) {
        this.hasBattery = hasBattery;
    }

    public long getAssignedJobs() {
        return assignedJobs;
    }

    public void setAssignedJobs(long assignedJobs) {
        this.assignedJobs = assignedJobs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceData that = (DeviceData) o;
        return mips == that.mips &&
                uptime == that.uptime &&
                nJobs == that.nJobs &&
                remainingBattery == that.remainingBattery &&
                Double.compare(that.cpuUsage, cpuUsage) == 0 &&
                hasBattery == that.hasBattery &&
                assignedJobs == that.assignedJobs &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mips, uptime, nJobs, remainingBattery, cpuUsage, hasBattery, assignedJobs);
    }

    @Override
    public void printData(@NotNull DataOutputStream outputStream) throws IOException {
        //outputStream.writeInt(uptime);
        outputStream.writeInt(nJobs);
        outputStream.writeInt(remainingBattery);
        outputStream.writeDouble(cpuUsage);
        outputStream.writeLong(assignedJobs);
        outputStream.flush();
    }

    public void printFullData(@NotNull DataOutputStream outputStream) throws IOException {
        outputStream.writeLong(id);
        outputStream.writeLong(mips);
        //outputStream.writeInt(uptime);
        outputStream.writeInt(nJobs);
        outputStream.writeInt(remainingBattery);
        outputStream.writeDouble(cpuUsage);
        outputStream.writeBoolean(hasBattery);
        outputStream.writeLong(assignedJobs);
        outputStream.flush();
    }
}
