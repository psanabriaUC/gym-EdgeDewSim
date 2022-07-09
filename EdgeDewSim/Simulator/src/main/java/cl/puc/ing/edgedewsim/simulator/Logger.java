package cl.puc.ing.edgedewsim.simulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for logging messages into the console.
 */
public class Logger {
    public static final String NEW_LINE = "\n";
    private static final ConcurrentHashMap<UUID, Logger> loggers = new ConcurrentHashMap<>();
    private static final Object lock = new Object();
    /**
     * The Constant LINE_SEPARATOR.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /**
     * Set data.separator to choose another way of separating the info in logs
     */
    private static final String DATA_SEPARATOR = System.getProperty("data.separator") != null ? System.getProperty("data.separator") : ";";
    public String EXPERIMENT = "";
    public int FINISHED_JOB_INDEX = 0;
    public boolean VERBOSE = true;
    public boolean LOG_FILES = false;

    private OutputStream DEBUG_OUTPUT_STREAM = null;
    /**
     * The output.
     */
    private OutputStream OUTPUT;
    /**
     * The disable output flag.
     */
    public boolean ENABLE = true;

    private String SCHEDULER = "";
    private String DEVICES = "";
    private String N_JOBS = "";

    public long currentMIPS = 0;


    public static Logger getInstance(Simulation simulation) {
        if (loggers.get(simulation.getId()) == null) {
            synchronized (lock) {
                if (loggers.get(simulation.getId()) == null) {
                    loggers.put(simulation.getId(), new Logger());
                }
            }
        }

        return loggers.get(simulation.getId());
    }

    public static void removeInstance(Simulation simulation) {
        UUID uuid = simulation.getId();
        synchronized (lock) {
            if (loggers.get(uuid) != null) {
                loggers.remove(uuid);
            }
        }
    }

    public void enable() {
        ENABLE = true;
    }

    public void disable() {
        ENABLE = false;
    }

    public void setOutput(OutputStream out) {
        OUTPUT = out;
    }

    private OutputStream getOutputStream() {
        if (OUTPUT == null)
            return System.out;
        return OUTPUT;
    }

    public void print(String data) {
        if (ENABLE)
            try {
                getOutputStream().write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void println(String data) {
        if (ENABLE)
            try {
                getOutputStream().write(data.getBytes());
                getOutputStream().write(LINE_SEPARATOR.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void println() {
        if (ENABLE)
            try {
                getOutputStream().write(LINE_SEPARATOR.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void println(Object data) {
        if (ENABLE)
            println(data.toString());
    }

    public void print(Object data) {
        if (ENABLE)
            print(data.toString());
    }

    public void logEntity(Entity e, String log, Object... data) {
        if (!ENABLE) return;
        StringBuilder logAux = new StringBuilder();
        logAux.append(e.getSimulation().getTime());
        logAux.append(DATA_SEPARATOR);
        logAux.append(e.getName());
        logAux.append(DATA_SEPARATOR);
        logAux.append(log);
        for (Object o : data) {
            logAux.append(DATA_SEPARATOR);
            logAux.append(o);
        }
        println(logAux.toString());
    }

    public void logGAIndividual(Short[] individual, String p) {
        if (!VERBOSE)
            return;

        StringBuilder s = new StringBuilder();
        for (Short aShort : individual)
            s.append(aShort).append(",");

        File file;

        FileWriter writer;
        file = new File("Yisel" + p);
        try {
            writer = new FileWriter(file, true);
            writer.write(s + NEW_LINE);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*Test Yisel*/
    public void logEnergy(Entity entity, String log, Object... data) {
        if (!VERBOSE)
            return;

        StringBuilder logAux = new StringBuilder();
        logAux.append(entity.getSimulation().getTime());
        logAux.append(DATA_SEPARATOR);
        logAux.append(log);
        for (Object o : data) {
            logAux.append(DATA_SEPARATOR);
            logAux.append(o);
        }
        println(logAux.toString());
    }

    /*Test Yisel logJob*/
    public void logJob(int jobId, String device, int batteryLevel, int jobInputSize, int jobOutputSize) {
        if (!VERBOSE)
            return;

        String fileName = "YiselJobs.csv";
        FINISHED_JOB_INDEX++;

        String logAux = jobId +
                "," +
                FINISHED_JOB_INDEX +
                "," +
                device +
                "," +
                batteryLevel +
                "," +
                EXPERIMENT +
                "," +
                jobInputSize +
                "," +
                jobOutputSize;
        String header = "JobId," +
                "JobIndex," +
                "Device," +
                "BatteryLevel," +
                "Experiment," +
                "JobInputSize," +
                "JobOutputSize";
        WriteLog(fileName, header, logAux);

    }

    /*Test Yisel logJob*/
    public void logJobDetails(int jobId, boolean rejected, boolean success, boolean successTransferBack,
                              long startTime, long startExecutionTime, long finishTime, long queueTime, long totalResultsTransferTime,
                              long totalTransferTime) {
        if (!VERBOSE)
            return;
        String fileName = "YiselJobsDetails.csv";

        String logAux = jobId +
                "," +
                EXPERIMENT +
                "," +
                rejected +
                "," +
                success +
                "," +
                successTransferBack +
                "," +
                startTime +
                "," +
                startExecutionTime +
                "," +
                finishTime +
                "," +
                queueTime +
                "," +
                totalResultsTransferTime +
                "," +
                totalTransferTime;
        String header = "JobId," +
                "Experiment," +
                "Rejected," +
                "Success," +
                "SuccessTransferBack," +
                "StartTime," +
                "StartExecutionTime," +
                "FinishTime," +
                "QueueTime," +
                "TotalResultsTransferTime," +
                "TotalTransferTime";
        WriteLog(fileName, header, logAux);

    }

    /*Test Yisel logDevice*/
    public void logDevice(String device, int jobsScheduled, int jobsFinished, int pendingTransfers, int totalTransfers, short wifiRSSI, double energyPercentageWastedInNetworkActivity, double initialJoules, double accEnergyInTransferring) {
        if (!VERBOSE)
            return;

        String fileName = "YiselDevices.csv";

        String logAux = device +
                "," +
                jobsScheduled +
                "," +
                jobsFinished +
                "," +
                pendingTransfers +
                "," +
                totalTransfers +
                "," +
                wifiRSSI +
                "," +
                energyPercentageWastedInNetworkActivity +
                "," +
                initialJoules +
                "," +
                accEnergyInTransferring +
                "," +
                EXPERIMENT;
        String header = "Device," +
                "JobsScheduled," +
                "JobsFinished," +
                "PendingTranfs," +
                "TotalTranfs," +
                "WifiRSSI," +
                "EnergyPercentageWastedInNetworkActivity," +
                "InitialJoules," +
                "AccEnergyInTransferring," +
                "Experiment,";
        WriteLog(fileName, header, logAux);
    }

    /*Test Yisel logExperiment*/
    public void logExperiment(int jobsArrived, int jobsScheduled, int jobsFinished, int jobsCompleted, double sentDataGB, double receivedDataGB, double percentEnergySendingData, double percentEnergyReceivingData, double totalGips, double executedGips) {
        if (!VERBOSE)
            return;

        String fileName = "YiselExperiments.csv";

        String logAux = EXPERIMENT +
                "," +
                jobsArrived +
                "," +
                jobsScheduled +
                "," +
                jobsFinished +
                "," +
                jobsCompleted +
                "," +
                sentDataGB +
                "," +
                receivedDataGB +
                "," +
                percentEnergySendingData +
                "," +
                percentEnergyReceivingData +
                "," +
                executedGips +
                "," +
                totalGips;
        String header = "Experiment," +
                "JobsArrived," +
                "JobsScheduled," +
                "JobsFinished," +
                "JobsCompleted," +
                "SentDataGB," +
                "ReceivedDataGB," +
                "PercentEnergySendingData," +
                "PercentEnergyReceivingData," +
                "ExecutedGips," +
                "TotalGips,";
        WriteLog(fileName, header, logAux);
    }

    /*Test Yisel logExperiment*/
    public void logExperiment2(int arrived, int notScheduled, int inputTransferInterrupted, int notStarted, int startedButNotFinished, int outputTransferInterrupted, int completed, double sentDataGB, double receivedDataGB, double totalDataToTransferGB, double percentEnergySendingData, double percentEnergyReceivingData, double totalGips, double executedGips) {
        if (!VERBOSE)
            return;

        String fileName = "YiselExperiments.csv";

        String logAux = EXPERIMENT +
                "," +
                arrived +
                "," +
                notScheduled +
                "," +
                inputTransferInterrupted +
                "," +
                notStarted +
                "," +
                startedButNotFinished +
                "," +
                outputTransferInterrupted +
                "," +
                completed +
                "," +
                sentDataGB +
                "," +
                receivedDataGB +
                "," +
                totalDataToTransferGB +
                "," +
                percentEnergySendingData +
                "," +
                percentEnergyReceivingData +
                "," +
                executedGips +
                "," +
                totalGips;
        String header = "Experiment," +
                "JobsArrived," +
                "JobsNotScheduled," +
                "JobsInputTransferInterrupted," +
                "JobsNotStarted," +
                "JobsStartedButNotFinished," +
                "JobsOutputTransferInterrupted," +
                "JobsCompleted," +
                "SentDataGB," +
                "ReceivedDataGB," +
                "TotalDataToTransferGB," +
                "PercentEnergySendingData," +
                "PercentEnergyReceivingData," +
                "ExecutedGips," +
                "TotalGips,";
        WriteLog(fileName, header, logAux);
    }

    /*Test Yisel logEvent*/
    public void logEvent(int srcId, long time, int trgId, int eventType) {
        if (!VERBOSE)
            return;

        String header = "srcId, time, trgId, eventType";
        String fileName = "YiselEvents.csv";

        String logAux = srcId +
                "," +
                time +
                "," +
                trgId +
                "," +
                eventType;
        WriteLog(fileName, header, logAux);
    }


    private void WriteLog(String fileName, String header, String content) {
        File file;
        boolean exists;
        FileWriter writer;
        file = new File(fileName);
        exists = file.exists();
        try {
            writer = new FileWriter(file, true);
            if (!exists && header != null && !header.trim().isEmpty()) {//then write the header
                writer.write(header + NEW_LINE);
            }
            writer.write(content + NEW_LINE);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    public void writeLog(String fileDescriptor, String content) {
        if (LOG_FILES) {
            String fileName = "results/" + SCHEDULER + "-" + DEVICES + "-" + N_JOBS + "-" + fileDescriptor + ".csv";
            WriteLog(fileName, "", content);
        }
    }

    public void writeMIPSLog(String timestamp) {
        writeLog("MIPMS", timestamp + "," + currentMIPS);
    }

    public void setSCHEDULER(String SCHEDULER) {
        this.SCHEDULER = SCHEDULER;
    }

    public void setDEVICES(String DEVICES) {
        this.DEVICES = DEVICES;
    }

    public void setN_JOBS(String n_JOBS) {
        N_JOBS = n_JOBS;
    }

    public void setLOG_FILES(boolean LOG_FILES) {
        this.LOG_FILES = LOG_FILES;
    }

    public void appendDebugInfo(String line) {

        try {
            DEBUG_OUTPUT_STREAM.write(line.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flushDebugInfo() {
        try {
            DEBUG_OUTPUT_STREAM.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDebugOutputStream(OutputStream debugFile) {
        DEBUG_OUTPUT_STREAM = debugFile;
    }


}
