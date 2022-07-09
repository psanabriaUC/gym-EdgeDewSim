package cl.puc.ing.edgedewsim.server.data;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class StatisticsData implements Data {
    int jobs;
    int completedJobs;
    int notCompletedJobs;
    long time;
    long completedOps;
    float averageOps;
    float totalSuccessfulOps;

    public int getJobs() {
        return jobs;
    }

    public void setJobs(int jobs) {
        this.jobs = jobs;
    }

    public int getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(int completedJobs) {
        this.completedJobs = completedJobs;
    }

    public int getNotCompletedJobs() {
        return notCompletedJobs;
    }

    public void setNotCompletedJobs(int notCompletedJobs) {
        this.notCompletedJobs = notCompletedJobs;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getCompletedOps() {
        return completedOps;
    }

    public void setCompletedOps(long completedOps) {
        this.completedOps = completedOps;
    }

    public float getAverageOps() {
        return averageOps;
    }

    public void setAverageOps(float averageOps) {
        this.averageOps = averageOps;
    }

    public float getTotalSuccessfulOps() {
        return totalSuccessfulOps;
    }

    public void setTotalSuccessfulOps(float totalSuccessfulOps) {
        this.totalSuccessfulOps = totalSuccessfulOps;
    }

    @Override
    public void printData(@NotNull DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(jobs);
        outputStream.writeInt(completedJobs);
        //outputStream.writeInt(notCompletedJobs);
        outputStream.writeLong(completedOps);
        outputStream.writeLong(time);
        //outputStream.writeFloat(averageOps);
        outputStream.writeFloat(totalSuccessfulOps);
        outputStream.flush();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatisticsData that = (StatisticsData) o;
        return jobs == that.jobs && completedJobs == that.completedJobs &&
                notCompletedJobs == that.notCompletedJobs &&
                time == that.time && completedOps == that.completedOps &&
                Float.compare(that.averageOps, averageOps) == 0 &&
                Float.compare(that.totalSuccessfulOps, totalSuccessfulOps) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobs, completedJobs, notCompletedJobs, time, completedOps, averageOps, totalSuccessfulOps);
    }
}
