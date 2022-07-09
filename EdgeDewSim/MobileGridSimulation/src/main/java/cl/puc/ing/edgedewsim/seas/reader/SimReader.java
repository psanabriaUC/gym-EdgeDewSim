package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.gridgain.spi.loadbalacing.energyaware.GridEnergyAwareLoadBalancing;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.network.*;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.DeviceTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.SimulationTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IPersisterFactory;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.ISimulationPersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;
import cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic.GAConfiguration;
import cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic.SimpleGASchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealingPolicy;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealingStrategy;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition.StealingCondition;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SimReader {

    private static final String PROXY_NAME = "PROXY";
    private static IPersisterFactory persisterFactory = null;
    private static int sim_id = -1;
    private final ReentrantLock simLock = new ReentrantLock();
    private final SimulationTuple simulationTuple = new SimulationTuple();
    //private boolean jobStealer=false;
    private final Simulation simulation;
    private String line;
    private BufferedReader conf;
    private Map<String, DeviceLoader> devices;
    private boolean hasJobGenerator;
    private JobGenerator jobGenerator;

    /**
     * Flag to enable or disable energy consumption simulation due to network related tasks (e.g. data transfers).
     */
    private boolean networkEnergyManagementFlag = false;

    public SimReader(Simulation simulation) {
        this.simulation = simulation;
        hasJobGenerator = false;
        jobGenerator = null;
    }

    public static void setPersisterFactory(IPersisterFactory persisterFactory) {
        SimReader.persisterFactory = persisterFactory;
    }

    public static int getSim_id() {
        return SimReader.sim_id;
    }

    /**
     * File format
     * -scheduler
     * className
     * OptionalParameters;
     * -nodes
     * name;mips;startTime;batteryBase;batteryFull;cpu*
     * -jobs
     * file
     */
    public void read(String file, boolean storeInDB) {
        ISimulationPersister simPersister = persisterFactory.getSimulationPersister();
        SQLSession session = simPersister.openSQLSession();
        if (storeInDB) {
            generateSimulationId(session, simPersister);
        }
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(4);

            this.conf = this.getReader(file);
            simulationTuple.setName(file);
            simulationTuple.setStartTime(new Timestamp(System.currentTimeMillis()));
            this.nextLine();
            while (line != null) {
                if (line.startsWith(";loadBalancing:"))
                    this.loadScheduler();
                else if (line.startsWith(";GAConfiguration"))
                    this.loadGAConfiguration();
                else if (line.startsWith(";comparator"))
                    this.loadComparator();
                else if (line.startsWith(";policy"))
                    this.loadPolicy();
                else if (line.startsWith(";strategy"))
                    this.loadStrategy();
                else if (line.startsWith(";condition"))
                    this.loadCondition();
                else if (line.startsWith(";link"))
                    this.loadLink();
                else if (line.startsWith(";networkEnergyManagementEnable"))
                    this.loadNetworkEnergyManagementFlag();
                else if (line.startsWith(";devicesStatusNotification"))
                    this.loadDeviceStatusNotificationPolicy();
                else if (line.startsWith(";nodeFile"))
                    this.loadNodes();
                else if (line.startsWith(";batteryFile"))
                    this.loadBatteryFile();
                else if (line.startsWith(";batteryFullCpuUsageFile"))
                    this.loadCpuFullScreenOffBatteryFile();
                else if (line.startsWith(";batteryFullCpuScreenOnFile"))
                    this.loadCpuFullScreenOnBatteryFile();
                else if (line.startsWith(";cpuFile"))
                    this.loadCPUFile();
                else if (line.startsWith(";wifiSignalStrength"))
                    this.loadWifiSignalStrength();
                else if (line.startsWith(";userActivity"))
                    this.loadUserActivity();
                else if (line.startsWith(";networkActivity")) {
                    this.loadNetworkActivity();
                } else if (line.startsWith(";jobsEvent")) {
                    executorService.execute(this.loadJobs());
                } else if (line.startsWith(";dynamicJobsEvent")) {
                    this.loadDynamicJobs();
                }

                else throw new IllegalStateException(this.line + " is not a valid parameter");
            }

            for (DeviceLoader loader : this.devices.values()) {
                loader.setSimLock(this.simLock);

                executorService.execute(loader);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

            this.conf.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        if (storeInDB) {
            updateSimulationTuple(session, simPersister);
            session.commit();
            session.close();
        }
    }

    private void loadDynamicJobs() throws IOException {
        DataInputStream inputStream = simulation.getInput();

        if (!hasJobGenerator) {
            System.out.println("Reading jobs parameters from input");
            int minJobs = inputStream.readInt();
            int maxJobs = inputStream.readInt();
            long minOps = inputStream.readLong();
            long maxOps = inputStream.readLong();
            int minInput = inputStream.readInt();
            int maxInput = inputStream.readInt();
            int minOutput = inputStream.readInt();
            int maxOutput = inputStream.readInt();
            int mode = inputStream.readInt();
            jobGenerator = new JobGenerator(minJobs, maxJobs, minOps, maxOps, minInput, maxInput, minOutput, maxOutput, mode);
            hasJobGenerator = true;
            System.out.println("Finished reading job parameters from input");
        }

        System.out.println("Reading timeline parameters for execution");
        long startTime = inputStream.readLong();
        int minDelta = inputStream.readInt();
        int maxDelta = inputStream.readInt();

        jobGenerator.generateJobs(simulation, startTime, minDelta, maxDelta);
        System.out.println("Jobs added to the simulation");
        this.nextLine();
    }

    private void loadDeviceStatusNotificationPolicy() throws Exception {
        //parse status notification policy
        this.nextLine();
        String[] statusNotificationPolicyParams = this.line.split(" ");
        long time = Long.parseLong(statusNotificationPolicyParams[1]);
        Device.STATUS_NOTIFICATION_TIME_FREQ = time;

        //parse status messages size
        this.nextLine();
        UpdateMessage.STATUS_MSG_SIZE_IN_BYTES = Integer.parseInt((this.line.split(" "))[1]);

        System.out.println("Status message notification frequency (in millis): " + time);
        System.out.println("Status message size (in bytes): " + UpdateMessage.STATUS_MSG_SIZE_IN_BYTES);
        this.nextLine();

    }

    private void loadGAConfiguration() throws Exception {
        String[] galineparts = this.line.split(" ");
        GAConfiguration gaConf = new GAConfiguration(galineparts[1], simulation);
        ((SimpleGASchedulerProxy) SchedulerProxy.getProxyInstance(simulation)).setGenAlgConf(gaConf);
        this.nextLine();
    }

    private void loadWifiSignalStrength() throws IOException {
        this.nextLine();
        while ((this.line != null) && (!this.line.startsWith(";"))) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String wifiSignalStrength = st.nextToken();
            String nodeId = st.nextToken().trim();
            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null) {
                System.err.println("There is no such device " + nodeId);
            }
            Objects.requireNonNull(loader).setWifiSignalStrength(Short.parseShort(wifiSignalStrength));
            this.nextLine();
        }

    }

    private void loadNetworkEnergyManagementFlag() throws Exception {
        String[] lineParts = this.line.split(" ");
        if (lineParts.length > 1) {
            networkEnergyManagementFlag = Boolean.parseBoolean(lineParts[1]);
        }
        System.out.println("NetworkEnergyManagementFlag: " + networkEnergyManagementFlag);
        //System.out.println("ACK Message size (in bytes): "+ NetworkModel.getModel().getAckMessageSizeInBytes());
        if (simulationTuple.getPolicy().compareTo("") != 0) {
            System.out.println("StealRequest Message size (in bytes): " + Message.STEAL_MSG_SIZE);
        }
        this.nextLine();

    }

    @SuppressWarnings("unchecked")
    private void loadCondition() throws Exception {
        DeviceLoader.MANAGER_FACTORY = new JobStealingFactory();
        String clazzName = this.line.split(" ")[1].trim();
        simulationTuple.setCondition(clazzName);
        Class<StealingCondition> clazz = (Class<StealingCondition>) Class.forName(clazzName);
        StealingCondition policy = clazz.getDeclaredConstructor().newInstance();
        this.setProperties(policy, clazz, this.line.split(" "), 2);
        this.simLock.lock();
        ((StealerProxy) SchedulerProxy.getProxyInstance(simulation)).setCondition(policy);
        this.simLock.unlock();
        this.nextLine();
    }

    @SuppressWarnings("unchecked")
    private void loadLink() throws Exception {
        String clazzName = this.line.split(" ")[1].trim();
        simulationTuple.setLink(clazzName);
        Class<Link> clazz = (Class<Link>) Class.forName(clazzName);
        Link ss = clazz.getDeclaredConstructor().newInstance();
        this.setProperties(ss, clazz, this.line.split(" "), 2);
        this.simLock.lock();
        ((SimpleNetworkModel) NetworkModel.getModel(simulation)).setDefaultLink(ss);
        this.simLock.unlock();
        this.nextLine();
    }

    @SuppressWarnings("unchecked")
    private void loadStrategy() throws Exception {
        String clazzName = this.line.split(" ")[1].trim();
        simulationTuple.setStrategy(clazzName);
        Class<StealingStrategy> clazz = (Class<StealingStrategy>) Class.forName(clazzName);
        StealingStrategy ss = clazz.getDeclaredConstructor().newInstance();
        this.setProperties(ss, clazz, this.line.split(" "), 2);
        this.simLock.lock();
        ((StealerProxy) SchedulerProxy.getProxyInstance(simulation)).setStrategy(ss);
        this.simLock.unlock();
        this.nextLine();
    }

    private void setProperties(Object ss,
                               Class<?> clazz, String[] split, int i) throws Exception {
        for (int j = i; j < split.length; j++) {
            String prop = split[j].trim();
            String[] kv = prop.split("=");
            String name = "set" + kv[0];
            Method m = clazz.getMethod(name, String.class);
            m.invoke(ss, kv[1]);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPolicy() throws Exception {
        DeviceLoader.MANAGER_FACTORY = new JobStealingFactory();
        String clazzName = this.line.split(" ")[1].trim();
        simulationTuple.setPolicy(clazzName);
        Class<StealingPolicy> clazz = (Class<StealingPolicy>) Class.forName(clazzName);
        StealingPolicy pol = clazz.getDeclaredConstructor().newInstance();
        this.setProperties(pol, clazz, this.line.split(" "), 2);
        this.simLock.lock();
        ((StealerProxy) SchedulerProxy.getProxyInstance(simulation)).setPolicy(pol);
        this.simLock.unlock();
        this.nextLine();
    }

    @SuppressWarnings("unchecked")
    private void loadComparator() throws Exception {
        String clazzName = this.line.split(" ")[1].trim();
        simulationTuple.setComparator(clazzName);
        Class<DeviceComparator> clazz = (Class<DeviceComparator>) Class.forName(clazzName);
        DeviceComparator comp = clazz.getDeclaredConstructor().newInstance();
        this.setProperties(comp, clazz, this.line.split(" "), 2);
        this.simLock.lock();
        ((GridEnergyAwareLoadBalancing) SchedulerProxy.getProxyInstance(simulation)).setDevComp(comp);
        this.simLock.unlock();
        this.nextLine();
    }

    private void loadCPUFile() throws IOException {
        this.nextLine();
        while ((this.line != null) && (!this.line.startsWith(";"))) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String cpuFile = st.nextToken();
            String nodeId = st.nextToken().trim();
            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null)
                System.err.println("There is no such device " + nodeId);
            else
                loader.setCPUFile(cpuFile);
            this.nextLine();
        }
    }

    private void loadCpuFullScreenOffBatteryFile() throws IOException {
        this.nextLine();
        while (!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String batFile = st.nextToken();
            String nodeId = st.nextToken().trim();
            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null)
                System.err.println("There is no such device " + nodeId);
            else
                loader.setBatteryCpuFullScreenOffFile(batFile);
            this.nextLine();
        }
    }

    private void loadCpuFullScreenOnBatteryFile() throws IOException {
        this.nextLine();
        while (!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String batFile = st.nextToken();
            String nodeId = st.nextToken().trim();
            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null)
                System.err.println("There is no such device " + nodeId);
            else
                loader.setBatteryCpuFullScreenOnFile(batFile);
            this.nextLine();
        }
    }

    private void loadBatteryFile() throws IOException {
        this.nextLine();

        StringTokenizer profileSt = new StringTokenizer(line, ";");
        String batbaseProfile = profileSt.nextToken();
        String[] parts = batbaseProfile.split("/");
        batbaseProfile = parts[parts.length - 1];
        simulationTuple.setBaseProfile(batbaseProfile);

        while (!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String batFile = st.nextToken();
            String nodeId = st.nextToken().trim();
            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null)
                System.err.println("There is no such device " + nodeId);
            else
                loader.setBatteryFile(batFile);
            this.nextLine();
        }
    }

    private void loadUserActivity() throws IOException {
        this.nextLine();

        while (!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String userActivityFile = st.nextToken();
            String nodeId = st.nextToken().trim();

            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null)
                System.err.println("There is no such device " + nodeId);
            else
                loader.setScreenActivityFilePath(userActivityFile);

            this.nextLine();
        }
    }

    private void loadNetworkActivity() throws IOException {
        this.nextLine();

        while (!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String networkActivityFile = st.nextToken();
            String nodeId = st.nextToken().trim();

            DeviceLoader loader = this.devices.get(nodeId);
            if (loader == null)
                System.err.println("There is no such device " + nodeId);
            else
                loader.setNetworkActivityFilePath(networkActivityFile);

            this.nextLine();
        }
    }

    private Thread loadJobs() throws IOException {
        this.nextLine();
        simulationTuple.setJobsFile(this.line);
        String nJobs = this.line.substring(
                this.line.indexOf("/", this.line.indexOf("/") + 1) + 1,
                this.line.indexOf(".")
        );
        Logger.getInstance(simulation).setN_JOBS(nJobs);
        Thread jobReader = new JobReader(this.simLock, this.getReader(this.line), simulation, this.networkEnergyManagementFlag);
        this.nextLine();
        return jobReader;
    }

	/*
	 * TODO: Cargar estrategias de stealing
	@SuppressWarnings("unchecked")
	private void loadStrategy() throws Exception{
		Class<StealingStrategy> stClas=(Class<StealingStrategy>)Class.forName(this.line);
		StealingStrategy strategy=stClas.newInstance();
		this.nextLine();
		while(line.contains(";")){
			StringTokenizer st=new StringTokenizer(line,";");
			Method m=stClas.getMethod(st.nextToken(), String.class);
			m.invoke(strategy, st.nextToken());
			this.nextLine();
		}
		((StealerProxy)SchedulerProxy.getProxyInstance(simulation)).setStrategy(strategy);
	}

	@SuppressWarnings("unchecked")
	private void loadPolicy() throws Exception {
		Class<StealingPolicy> polClas=(Class<StealingPolicy>)Class.forName(this.line);
		StealingPolicy policy=polClas.newInstance();
		this.nextLine();
		while(line.contains(";")){
			StringTokenizer st=new StringTokenizer(line,";");
			Method m=polClas.getMethod(st.nextToken(), String.class);
			m.invoke(policy, st.nextToken());
			this.nextLine();
		}
		((StealerProxy)SchedulerProxy.getProxyInstance(simulation)).setPolicy(policy);
	}*/

    private void loadNodes() throws IOException {
        this.nextLine();
        simulationTuple.setTopologyFile(this.line);
        String devices = this.line.substring(
                this.line.indexOf("/", this.line.indexOf("/") + 1) + 1,
                this.line.indexOf(".")
        );
        Logger.getInstance(simulation).setDEVICES(devices);
        DeviceReader deviceReader = new DeviceReader(this.line, networkEnergyManagementFlag, simulation);
        this.devices = deviceReader.getDevices();
        this.nextLine();
    }

    /**
     * Scheduler
     * If it is StealerProxy
     * policy
     * method;value*
     * stategy
     * method;value*
     */
    private void loadScheduler() throws Exception {
        String[] schedulerConstructor = this.line.split(" ");
        String clazzName = schedulerConstructor[1].trim();
        String schedulerName = clazzName.substring(clazzName.lastIndexOf(".") + 1);
        Logger.getInstance(simulation).setSCHEDULER(schedulerName);
        boolean schedulerHasArguments = false;
        String arguments = "";

        if (schedulerConstructor.length > 2) {
            schedulerHasArguments = true;
            arguments = schedulerConstructor[2].trim();
        }
        simulationTuple.setScheduler(clazzName);
        // TODO: save arguments into the DB
        @SuppressWarnings("unchecked")
        Class<SchedulerProxy> clazz = (Class<SchedulerProxy>) Class.forName(clazzName);
        this.simLock.lock();

        if (schedulerHasArguments) {
            clazz.getConstructor(String.class, String.class, Simulation.class).newInstance(PROXY_NAME, arguments, simulation);
        } else {
            clazz.getConstructor(String.class, Simulation.class).newInstance(PROXY_NAME, simulation);
        }
        DeviceTuple proxyTuple = new DeviceTuple(PROXY_NAME, 0, 0, simulationTuple.getSimId());
        persisterFactory.getDevicePersister().saveDeviceIntoMemory(PROXY_NAME, proxyTuple);
        this.simLock.unlock();
        this.nextLine();
    }

    private BufferedReader getReader(String file) throws FileNotFoundException {
        return new BufferedReader(new FileReader(file));
    }

    private void nextLine() throws IOException {
        this.line = this.conf.readLine();
        if (line == null) return;
        this.line = this.line.trim();
        while (line.startsWith("#") ||
                line.equals("")) {
            this.line = this.conf.readLine();
            if (line == null) return;
            this.line = this.line.trim();
        }
    }

    public SimulationTuple getSimulationTuple() {
        return simulationTuple;
    }

    //this method performs an insert to the table Simulation in order to get the id of the simulation tuple corresponding to this run.
    private void generateSimulationId(SQLSession session, ISimulationPersister simPersister) {

        try {
            simPersister.insertSimulation(session, simulationTuple);
            SimReader.sim_id = simulationTuple.getSimId();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSimulationTuple(SQLSession session, ISimulationPersister simPersister) {
        try {
            simPersister.updateSimulation(session, simulationTuple);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
