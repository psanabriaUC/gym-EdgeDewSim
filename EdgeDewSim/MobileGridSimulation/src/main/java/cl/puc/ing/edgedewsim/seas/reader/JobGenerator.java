package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JobGenerator {
    private static final int UNIFORM_MODE = 100;
    private static final int ALL_MODE = 101;
    private static final int NONE_MODE = 102;
    private static final int FIXED_MODE = 103;
    private static final int NLOGN_MODE = 104;

    private int minJobs;
    private int maxJobs;
    private long minOps;
    private long maxOps;
    private int minInputSize;
    private int maxInputSize;
    private int minOutputSize;
    private int maxOutputSize;
    private int mode;

    public JobGenerator(int minJobs, int maxJobs, long minOps, long maxOps, int minInputSize, int maxInputSize, int minOutputSize, int maxOutputSize, int mode) {
        if (minJobs > maxJobs)
            throw new IllegalArgumentException("minJobs can't be greater than maxJobs");
        if (minOps > maxOps)
            throw new IllegalArgumentException("minOps can't be greater than maxOps");
        if (minInputSize > maxOutputSize)
            throw new IllegalArgumentException("minInputSize can't be greater than maxInputSize");
        if (minOutputSize > maxOutputSize)
            throw new IllegalArgumentException("minOutputSize can't be greater than maxOutputSize");

        this.minJobs = minJobs;
        this.maxJobs = maxJobs;
        this.minOps = minOps;
        this.maxOps = maxOps;
        this.minInputSize = minInputSize;
        this.maxInputSize = maxInputSize;
        this.minOutputSize = minOutputSize;
        this.maxOutputSize = maxOutputSize;
        this.mode = mode;
    }

    public int getMinJobs() {
        return minJobs;
    }

    public void setMinJobs(int minJobs) {
        if (minJobs > maxJobs)
            throw new IllegalArgumentException("minJobs can't be greater than maxJobs");
        this.minJobs = minJobs;
    }

    public int getMaxJobs() {
        return maxJobs;
    }

    public void setMaxJobs(int maxJobs) {
        if (minJobs > maxJobs)
            throw new IllegalArgumentException("minJobs can't be greater than maxJobs");
        this.maxJobs = maxJobs;
    }

    public long getMinOps() {
        return minOps;
    }

    public void setMinOps(long minOps) {
        if (minOps > maxOps)
            throw new IllegalArgumentException("minOps can't be greater than maxOps");
        this.minOps = minOps;
    }

    public long getMaxOps() {
        return maxOps;
    }

    public void setMaxOps(long maxOps) {
        if (minOps > maxOps)
            throw new IllegalArgumentException("minOps can't be greater than maxOps");
        this.maxOps = maxOps;
    }

    public int getMinInputSize() {
        return minInputSize;
    }

    public void setMinInputSize(int minInputSize) {
        if (minInputSize > maxOutputSize)
            throw new IllegalArgumentException("minInputSize can't be greater than maxInputSize");
        this.minInputSize = minInputSize;
    }

    public int getMaxInputSize() {
        return maxInputSize;
    }

    public void setMaxInputSize(int maxInputSize) {
        if (minInputSize > maxOutputSize)
            throw new IllegalArgumentException("minInputSize can't be greater than maxInputSize");
        this.maxInputSize = maxInputSize;
    }

    public int getMinOutputSize() {
        return minOutputSize;
    }

    public void setMinOutputSize(int minOutputSize) {
        if (minOutputSize > maxOutputSize)
            throw new IllegalArgumentException("minOutputSize can't be greater than maxOutputSize");
        this.minOutputSize = minOutputSize;
    }

    public int getMaxOutputSize() {
        return maxOutputSize;
    }

    public void setMaxOutputSize(int maxOutputSize) {
        if (minOutputSize > maxOutputSize)
            throw new IllegalArgumentException("minOutputSize can't be greater than maxOutputSize");
        this.maxOutputSize = maxOutputSize;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        if (mode != UNIFORM_MODE && mode != ALL_MODE) {
            throw new IllegalArgumentException("This mode doesn't exist");
        }
        this.mode = mode;
    }

    public void generateJobs(@NotNull Simulation simulation, long startTime, int minDeltaTime, int maxDeltaTime) {
        final Random random = new Random();
        final int length = random.nextInt(maxJobs - minJobs + 1) + minJobs;
        final int schedulerProxyId = simulation.getEntity("PROXY").getId();
        long currentTime = startTime;

        for (int i = 0; i < length; i++)  {
            currentTime += random.nextInt(maxDeltaTime - minDeltaTime + 1) + minDeltaTime;
            int inputSize = random.nextInt(maxInputSize - minInputSize + 1) + minInputSize;
            int outputSize = random.nextInt(maxOutputSize - minOutputSize + 1) + minOutputSize;

            long inputEntries = (long) (inputSize / 1024);
            long ops = getOps(inputEntries);

            while (ops >= maxOps) ops = getOps(inputEntries);

            Job job = new Job(i, ops, schedulerProxyId, inputSize, outputSize, simulation);

            Event event = Event.createEvent(Event.NO_SOURCE, currentTime, schedulerProxyId, SchedulerProxy.EVENT_JOB_ARRIVE, job);
            simulation.addEvent(event);
        }
    }

    private long getOps(long inputEntries) {

        if (mode == NONE_MODE)
            return 0;
        if (mode == FIXED_MODE)
            return 83629400;
        if (mode == NLOGN_MODE)
            return (long) ((long) Math.log(inputEntries) * inputEntries);
        if (mode == ALL_MODE) {
            int function = ((int) (Math.random() * 10000000)) % 3;
            switch (function) {
                case 0:
                    return (long) ((long) Math.log(inputEntries) * inputEntries);
                case 1:
                    return (long) ((long) Math.pow(inputEntries, 2));
                case 2:
                    return (long) ((long) Math.pow(inputEntries, 3));
            }
        }
        if (mode == UNIFORM_MODE) {
            final Random random = new Random();
            return (random.nextLong() & Long.MAX_VALUE) % (maxOps - minOps + 1) + minOps;
        }
        return -1;
    }
}
