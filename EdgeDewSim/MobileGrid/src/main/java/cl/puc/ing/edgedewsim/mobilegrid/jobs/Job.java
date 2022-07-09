package cl.puc.ing.edgedewsim.mobilegrid.jobs;

import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A representation of an arbitrary task to be performed/computed by an {@link Entity} during
 * a simulation.
 */
public class Job {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    private final int jobId;
    private final int userProvidedJobId;
    private long ops;
    private int src;
    private int inputSize;
    private int outputSize;
    private boolean fromEdge;
    private final UUID simulationId;

    public Job(int userProvidedJobId, long ops, int src, int inputSize, int outputSize, @NotNull Simulation simulation) {
        super();
        this.jobId = NEXT_ID.incrementAndGet();
        this.userProvidedJobId = userProvidedJobId;
        this.ops = ops;
        this.src = src;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.fromEdge = false;
        this.simulationId = simulation.getId();
    }

    public long getOps() {
        return ops;
    }

    public void setOps(long ops) {
        this.ops = ops;
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public int getInputSize() {
        return inputSize;
    }

    public void setInputSize(int inputSize) {
        this.inputSize = inputSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }

    public int getJobId() {
        return jobId;
    }

    public boolean isFromEdge() {
        return fromEdge;
    }

    public void setFromEdge(boolean fromEdge) {
        this.fromEdge = fromEdge;
    }

    public UUID getSimulationId() {
        return simulationId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Job) {
            return ((Job) obj).jobId == this.jobId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.jobId;
    }

    @Override
    public String toString() {
        return "Job [jobId=" + this.jobId + "]";
    }


}
