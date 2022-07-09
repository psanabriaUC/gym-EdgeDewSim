package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.network.MessageHandler;
import cl.puc.ing.edgedewsim.mobilegrid.network.NetworkModel;
import cl.puc.ing.edgedewsim.mobilegrid.network.UpdateMessage;
import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A representation of a device with a limited power supply belonging to a grid. This object emulates the processing
 * of arbitrary tasks and the battery's energy degradation, both when idle and when subjected to extended workloads.<br/>
 * <br/>
 * When sending messages, the following methods are invoked in order:
 * <ul>
 * <li>Sender's {@link Node#startTransfer(Node, int, Object)}</li>
 * <li>Receiver's {@link Node#incomingData(Node, int)}</li>
 * <li>Receiver's {@link Node#onMessageReceived(Message)}</li>
 * <li>Sender's {@link Node#onMessageSentAck(Message)}</li>
 * </ul>
 * Jobs are received by this device, executed, and then their completion is reported back to the original sender.
 */
public class Device extends Entity implements Node, DeviceListener {

    public static final int EVENT_TYPE_BATTERY_UPDATE = 100;
    public static final int EVENT_TYPE_CPU_UPDATE = 101;
    public static final int EVENT_TYPE_FINISH_JOB = 102;
    public static final int EVENT_TYPE_DEVICE_START = 103;
    public static final int EVENT_TYPE_STATUS_NOTIFICATION = 104;
    public static final int EVENT_TYPE_SCREEN_ACTIVITY = 105;
    public static final int EVENT_NETWORK_ACTIVITY = 106;
    public static final int EVENT_TYPE_SHUTDOWN = 107;

    /* Size of message buffer for transfers in bytes */
    public static final int MESSAGES_BUFFER_SIZE = 1024 * 1024; // 1mb
    // //128*1024
    // //128k
    /**
     * This field defines the update frequency of device status messages. The time is given
     * in milliseconds. The default value is 1298138 which equates approximately to 20 minutes.
     * <p>
     * Providing the '0' value, enables the status notification whose frequency is
     * dynamically adapted based on the time in which the battery level drops 1%.
     */
    public static long STATUS_NOTIFICATION_TIME_FREQ = 0;
    /**
     * Queue of messages that need to be transferred through the communication channel.
     */
    protected final PriorityQueue<TransferInfo<?>> transfersPending = new PriorityQueue<>();
    protected final Map<Integer, TransferInfo<?>> transferInfoMap = new HashMap<>();
    /**
     * List of jobs that have been finished and have already been reported to the proxy.
     * This is used for logging purposes only by the simulator.
     */
    protected final List<Job> finishedJobsCompleted = new LinkedList<>();
    /**
     * Flag to indicate this device is currently receiving data from the network.
     */
    protected boolean isReceiving = false;
    /**
     * Flag to indicate this device is currently sending data through the network.
     */
    protected boolean isSending = false;
    /**
     * Map containing the incoming jobs currently being transferred to this device. Maps the IDs of the jobs to their
     * respective {@link JobTransfer} information.
     */
    protected final Map<Integer, JobTransfer> outgoingJobTransfers = new HashMap<>();
    /**
     * Helper for handling all logic related to battery depletion.
     */
    protected final BatteryManager batteryManager;
    /**
     * Helper for handling job execution simulation based on available CPU.
     */
    protected final ExecutionManager executionManager;
    /**
     * Helper for handling battery depletion to network-related activity.
     */
    protected final NetworkEnergyManager networkEnergyManager;
    private Event nextStatusNotificationEvent = null;
    /**
     * Message handler for processing messages containing {@link Job} payloads.
     */
    private final JobMessageHandler jobMessageHandler = new JobMessageHandler();
    /**
     * Message handler for processing messages containing {@link UpdateMessage} payloads.
     */
    private final UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler();
    /**
     * Default message handler for unspecified message subtypes. Does nothing.
     */
    private final MessageHandler<?> defaultMessageHandler = new MessageHandler<>();
    private final boolean useBattery;
    private OnFinishJobListener onFinishJobListener;

    /**
     * when this flag is true the device informs its State Of Charge every time
     * it decreases in at least one percentage w.r.t the last SOC informed
     */
    //private boolean informSOC = true;
    public Device(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem, Simulation simulation, boolean useBattery) {
        super(name, simulation);
        this.batteryManager = bt;
        this.executionManager = em;
        this.networkEnergyManager = nem;
        this.useBattery = useBattery;
        this.onFinishJobListener = null;
    }

    /**
     * when this flag is true the device informs its State Of Charge every time
     * it decreases in at least one percentage w.r.t the last SOC informed
     */
    public Device(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem, Simulation simulation) {
        this(name, bt, em, nem, simulation, true);
    }

    /**
     * Called when a message is received by this device.
     *
     * @param message The message that just arrived. Note that this message's payload may represent only a fraction
     *                of the size of a full message in case it is too big to be sent at once.
     */
    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {
        isReceiving = false;
        if (networkEnergyManager.onReceiveData(message.getSource(), message.getDestination(), message.getTotalDataSize(), message.getMessageSize())) {
            getMessageHandler(message.getData()).onMessageReceived(message);
            if (isMessageFullyReceived(message)) {
                getMessageHandler(message.getData()).onMessageFullyReceived(message);
            }
            if (transfersPending.size() > 0 && !isSending) {
                TransferInfo<?> tInfo = transfersPending.peek();
                continueTransferring(tInfo);
            }
        } else {
            getMessageHandler(message.getData()).onCouldNotReceiveMessage(message);
        }
    }

    /**
     * Gets the message handler associated with the given data class type. Subclasses of {@link Device} that define
     * additional message types should overwrite this method to return their own message handlers, and defer back
     * to this implementation for unknown data types.
     *
     * @param data The data to process.
     * @return The respective {@link MessageHandler} for the given data.
     */
    @SuppressWarnings("unchecked")
    protected <T> MessageHandler<T> getMessageHandler(T data) {
        if (data == null) {
            return (MessageHandler<T>) defaultMessageHandler;
        } else if (data instanceof Job) {
            return (MessageHandler<T>) jobMessageHandler;
        } else if (data instanceof UpdateMessage) {
            return (MessageHandler<T>) updateMessageHandler;
        }
        return (MessageHandler<T>) defaultMessageHandler;
    }

    // TODO: we are currently only checking that the last package of the message has been received, this is insufficient,
    // we should be checking that all messages with lower offsets have been received as well.
    private boolean isMessageFullyReceived(@NotNull Message<?> message) {
        return message.isLastMessage();
    }

    /**
     * Called when an ACK for a message sent by this device is received back.
     *
     * @param messageSent The message that was sent.
     */
    @Override
    public <T> void onMessageSentAck(@NotNull Message<T> messageSent) {
        isSending = false;

        int messageSize = (int) NetworkModel.getModel(simulation).getAckMessageSizeInBytes();

        if (networkEnergyManager.onReceiveData(messageSent.getDestination(), this, messageSent.getTotalDataSize(), messageSize)) { // if the ack could be processed then the node update
            transferInfoMap.get(messageSent.getId()).increaseIndex();

            getMessageHandler(messageSent.getData()).onMessageSentAck(messageSent);

            // IF FINISH CURRENT TRANSFER
            if (messageSent.isLastMessage()) {
                getMessageHandler(messageSent.getData()).onMessageFullySent(messageSent);
            }

            TransferInfo<?> nextTransfer = transfersPending.peek();
            while (nextTransfer != null && nextTransfer.allMessagesSent()) {
                transfersPending.remove();
                nextTransfer = transfersPending.peek();
            }

            if (!isReceiving && nextTransfer != null) {
                continueTransferring(nextTransfer);
            }
        }
    }

    private <T> void continueTransferring(@NotNull TransferInfo<T> transferInfo) {
        long messageSize = transferInfo.getMessageSize(MESSAGES_BUFFER_SIZE);

        if (this.networkEnergyManager.onSendData(this, SchedulerProxy.getProxyInstance(simulation), transferInfo.getTotalTransferSize(MESSAGES_BUFFER_SIZE), messageSize)) { // return true if energy is enough to send the message
            getMessageHandler(transferInfo.getData()).onWillSendMessage(transferInfo);

            NetworkModel.getModel(simulation).send(this, SchedulerProxy.getProxyInstance(simulation), transferInfo.getId(), transferInfo.getTotalTransferSize(MESSAGES_BUFFER_SIZE), (int) messageSize, transferInfo.getData(),
                    transferInfo.getCurrentIndex(), transferInfo.isLastMessage());
        } else {
            getMessageHandler(transferInfo.getData()).onCouldNotSendMessage(transferInfo);
        }
    }

    @Override
    public <T> void fail(@NotNull Message<T> message) {
        getMessageHandler(message.getData()).onMessageSentFailedToArrive(message);

        // long time=Simulation.getTime()-jobStartTransferTime;
        // JobStatsUtils.fail(j);
        // Logger.logEntity(this, "link failed when send job
        // result.","jobId="+j.getJobId());
        // JobStatsUtils.changeLastTransferTime(j,
        // time,jobStartTransferTime);

    }

    @Override
    public boolean isOnline() {
        return this.isActive();
    }

    @Override
    public void processEvent(@NotNull Event event) {
        switch (event.getEventType()) {
            case Device.EVENT_TYPE_BATTERY_UPDATE:
                int newBatteryLevel = Objects.requireNonNull(event.getData());
                this.batteryManager.onBatteryEvent(newBatteryLevel);

                if (STATUS_NOTIFICATION_TIME_FREQ == 0
                        && SchedulerProxy.getProxyInstance(simulation).getLastReportedSOC(this) - newBatteryLevel >= BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION
                        && newBatteryLevel > 0) {
                    UpdateMessage updateMessage = new UpdateMessage(this.getName(), newBatteryLevel, simulation.getTime());
                    queueMessageTransfer(SchedulerProxy.getProxyInstance(simulation), updateMessage, UpdateMessage.STATUS_MSG_SIZE_IN_BYTES);
                }
                break;
            case Device.EVENT_TYPE_CPU_UPDATE:
                this.executionManager.onCPUEvent(Objects.requireNonNull(event.getData()));
                break;
            case Device.EVENT_TYPE_FINISH_JOB:
                Job job = Objects.requireNonNull(event.getData());

                if (!runsOnBattery())
                    job.setFromEdge(true);
                this.executionManager.onFinishJob(job);
                queueMessageTransfer(SchedulerProxy.getProxyInstance(simulation), job, job.getOutputSize());
                if (onFinishJobListener != null)
                    onFinishJobListener.finished(this, job);
                break;
            case Device.EVENT_TYPE_DEVICE_START:
                onStartup();
                break;
            case Device.EVENT_TYPE_STATUS_NOTIFICATION:
                // notify the proxy about my status
                UpdateMessage updateMessage = new UpdateMessage(this.getName(), (int) batteryManager.getCurrentSOC(),
                        simulation.getTime());
                queueMessageTransfer(SchedulerProxy.getProxyInstance(simulation), updateMessage, UpdateMessage.STATUS_MSG_SIZE_IN_BYTES);

                // plan the next status notification event
                if (Device.STATUS_NOTIFICATION_TIME_FREQ > 0 && useBattery) {
                    long nextNotificationTime = simulation.getTime() + Device.STATUS_NOTIFICATION_TIME_FREQ;
                    this.nextStatusNotificationEvent = Event.createEvent(Event.NO_SOURCE, nextNotificationTime,
                            this.getId(), Device.EVENT_TYPE_STATUS_NOTIFICATION, null);
                    simulation.addEvent(this.nextStatusNotificationEvent);
                }
                break;
            case Device.EVENT_TYPE_SCREEN_ACTIVITY:
                Boolean flag = Objects.requireNonNull(event.getData());
                this.batteryManager.onUserActivityEvent(flag);
                break;
            case Device.EVENT_NETWORK_ACTIVITY:
                Event.NetworkActivityEventData eventData = Objects.requireNonNull(event.getData());
                queueMessageTransfer(CloudNode.getCloudNode(simulation), null, eventData.getMessageSize(), TransferInfo.PRIORITY_HIGH);
                // queueMessageTransfer();
                break;
            case Device.EVENT_TYPE_SHUTDOWN:
                batteryManager.onBatteryEvent(0);
                break;
            default:
                throw new IllegalArgumentException("Unexpected event in simulation with id: " + simulation.getId()
                        + " with event type: " + event.getEventType());
        }
    }

    protected <T> void queueMessageTransfer(Node destination, T data, long payloadSize) {
        queueMessageTransfer(destination, data, payloadSize, TransferInfo.PRIORITY_DEFAULT);
    }

    /**
     * Queues a message for transfer to the given recipient. If the communication channel is currently free,
     * the message is sent immediately.
     *
     * @param destination The receiver of this message.
     * @param data        The message's payload.
     * @param payloadSize The size of the payload in bytes.
     */
    protected <T> void queueMessageTransfer(Node destination, T data, long payloadSize, int priority) {
        long subMessagesCount = (long) Math.ceil(payloadSize / (double) MESSAGES_BUFFER_SIZE);
        int lastMessageSize = (int) (payloadSize - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE);
        TransferInfo<T> transferInfo = new TransferInfo<>(SchedulerProxy.getProxyInstance(simulation), data, subMessagesCount, lastMessageSize);
        transferInfoMap.put(transferInfo.getId(), transferInfo);
        transferInfo.setPriority(priority);
        transfersPending.add(transferInfo);

        if (transfersPending.size() == 1 && !isReceiving && !isSending) {
            int messageSize = transferInfo.getMessageSize(MESSAGES_BUFFER_SIZE);
            if (this.networkEnergyManager.onSendData(this, destination, transferInfo.getTotalTransferSize(MESSAGES_BUFFER_SIZE), messageSize)) {
                // if energy is enough to send the message

                getMessageHandler(data).onWillSendMessage(transferInfo);

                NetworkModel.getModel(simulation).send(this, destination, transferInfo.getId(), transferInfo.getTotalTransferSize(MESSAGES_BUFFER_SIZE), messageSize, data,
                        transferInfo.getCurrentIndex(), transferInfo.isLastMessage());
            } else {
                getMessageHandler(data).onCouldNotSendMessage(transferInfo);
            }
        }
    }

    /**
     * Called when the device boots up.
     */
    private void onStartup() {
        SchedulerProxy.getProxyInstance(simulation).addDevice(this);
        this.batteryManager.startWorking();
        JobStatsUtils.getInstance(simulation).deviceJoinTopology(this, this.batteryManager.getStartTime());
        SchedulerProxy.getProxyInstance(simulation).updateDeviceSOC(this, getBatteryLevel());
        if (Device.STATUS_NOTIFICATION_TIME_FREQ > 0) {
            long nextNotificationTime = simulation.getTime() + Device.STATUS_NOTIFICATION_TIME_FREQ;
            this.nextStatusNotificationEvent = Event.createEvent(Event.NO_SOURCE, nextNotificationTime, this.getId(),
                    Device.EVENT_TYPE_STATUS_NOTIFICATION, null);
            simulation.addEvent(this.nextStatusNotificationEvent);
        }
    }

    /**
     * Called when the device runs out of battery.
     */
    public void onBatteryDepletion() {
        JobStatsUtils.getInstance(simulation).deviceLeftTopology(this, simulation.getTime());
        if (nextStatusNotificationEvent != null) {
            simulation.removeEvent(nextStatusNotificationEvent);
        }
        SchedulerProxy.getProxyInstance(simulation).remove(this);
        this.executionManager.shutdown();
        this.batteryManager.shutdown();
        this.setActive(false);

        Logger.getInstance(simulation).logEntity(this, "Device leave the cluster with queued_jobs=" + this.executionManager.getQueuedJobs() + "; pending_outgoing_transfers=" + this.transfersPending.size());
        for (JobTransfer jobTransfer : outgoingJobTransfers.values()) {
            Node destination = jobTransfer.destination;
            if (destination instanceof DeviceListener) {
                ((DeviceListener) destination).onDeviceFail(this);
            }
        }
    }

    @Override
    public void incomingData(@NotNull Node scr, int id) {
        // TODO:provide an energy-aware treatment for an incoming data message.
        // For example, enable the wifi to be able onMessageReceived data.
        isReceiving = true;
    }

    @Override
    public void failReception(@NotNull Node scr, int id) {
        // TODO:provide an energy-aware treatment for a reception failure
        // message. For example, disable the wifi.
    }

    /**
     * Adds a job to the processing queue. This method should be invoked once a job has finished being
     * transferred to this device.
     *
     * @param job The job to process.
     */
    private void addJob(Job job) {
        this.executionManager.addJob(job);
    }

    public Job removeJob(int index) {
        return this.executionManager.removeJob(index);
    }

    @Override
    public void startTransfer(@NotNull Node dst, int id, @Nullable Object data) {
        if (data instanceof Job) {
            Job job = (Job) data;
            if (!this.outgoingJobTransfers.containsKey(job.getJobId())) {
                outgoingJobTransfers.put(job.getJobId(), new JobTransfer(job, simulation.getTime(), dst));
            }
        }

        isSending = true;
    }

    @Override
    public void onDeviceFail(Node e) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean runsOnBattery() {
        return useBattery;
    }

    public List<Job> getFinishedJobTransfersCompleted() {
        return finishedJobsCompleted;
    }

    public int getCurrentTransfersCount() {
        int enqueuedJobs = 0;
        for (TransferInfo<?> transferInfo : transfersPending) {
            if (transferInfo.getData() instanceof Job) enqueuedJobs++;
        }
        return enqueuedJobs;
    }

    public int getCurrentTotalTransferCount() {
        return getCurrentTransfersCount() + finishedJobsCompleted.size();
    }

    public double getAccEnergyInTransferring() {
        return networkEnergyManager.getAccEnergyInTransferring();
    }

    /**
     * Considering that devices buffer jobs that have been assigned by a scheduler,
     * this method returns the amount of jobs which are queued and unfinished in the device.
     *
     * @return The number of jobs assigned to the device in queued and unfinished status.
     */
    public int getUnfinishedJobs() {
        return this.executionManager.getCurrentlyExecutingJobs() + this.executionManager.getQueuedJobs();
    }

    /**
     * Returns all the jobs assigned to the device.
     *
     * @return The number of jobs assigned to the device.
     */
    public int getNumberOfJobs() {
        return this.executionManager.getCurrentlyExecutingJobs();
    }

    /**
     * Returns waiting jobs on this device.
     *
     * @return The amount of jobs currently waiting to be executed.
     */
    public int getWaitingJobs() {
        return this.executionManager.getQueuedJobs();
    }

    public long getMIPS() {
        return this.executionManager.getMIPS();
    }

    public double getCPUUsage() {
        return this.executionManager.getCPUUsage();
    }

    public int getBatteryLevel() {
        return this.batteryManager.getCurrentBattery();
    }

    public long getEstimatedUptime() {
        return this.batteryManager.getEstimatedUptime();
    }

    public long getTotalBatteryCapacityInJoules() {
        return this.batteryManager.getBatteryCapacityInJoules();
    }

    public double getJoulesBasedOnLastReceivedSOC(int newBatteryLevel)
    {
        return this.batteryManager.getJoulesBasedOnLastReceivedSOC(newBatteryLevel);
    }

    /**
     * This method returns the last Wifi Received Signal Strength reported by
     * the device
     */
    public short getWifiRSSI() {
        return networkEnergyManager.getWifiRSSI();
    }

    /**
     * this method returns the energy (in Joules) that the device is supposed to
     * waste when sending the amount of data indicated as argument.
     *
     * @param data The amount of data transferred in bytes.
     */
    public double getEnergyWasteInTransferringData(double data) {
        long subMessagesCount = (long) Math.ceil(data / (double) MESSAGES_BUFFER_SIZE);
        long lastMessageSize = (long) data - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE;
        double energy = (subMessagesCount - 1)
                * networkEnergyManager.getJoulesWastedWhenTransferData(MESSAGES_BUFFER_SIZE);
        energy += networkEnergyManager.getJoulesWastedWhenTransferData(lastMessageSize);
        return energy;
        // networkEnergyManager.getJoulesWastedWhenTransferData(data);
    }

    public double getEnergyPercentageWastedInNetworkActivity() {
        double initialJoules = (((double) getInitialSOC()
                / (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION)
                * (double) batteryManager.getBatteryCapacityInJoules()) / 100;

        return (networkEnergyManager.getAccEnergyInTransferring() * 100) / initialJoules;
    }

    /**
     * Returns the state of charge of the device when it joined the grid as a value between 0 and 10.000.000,
     * where the latter means 100%.
     *
     * @return The initial state of charge of the device.
     */
    public int getInitialSOC() {
        return batteryManager.getInitialSOC();
    }

    /**
     * Returns the Joules of the device when it joined the grid.
     *
     * @return The initial amount of energy in the device's battery, in Joules.
     */
    public double getInitialJoules() {
        return getInitialSOC() * getTotalBatteryCapacityInJoules()
                / ((double) 100 * (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION);
    }

    @Override
    public boolean isSending() {
        // return finishedJobTransfersPending.size()!= 0;
        return isSending;
    }

    @Override
    public boolean isReceiving() {
        return isReceiving;
    }

    public void setOnFinishJobListener(OnFinishJobListener listener) {
        this.onFinishJobListener = listener;
    }

    /**
     * Convert an energy spent value given in Joules to the battery level percentage it represents
     * according to device battery capacity
     *
     * @param energyValueInJoules is the value to convert
     * @return an integer within the range [0, BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION x 100]
     */
    public int convertIntoBatteryLevel(double energyValueInJoules) {
        return (int) (((energyValueInJoules * 100) / this.getTotalBatteryCapacityInJoules()) * BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION);
    }

    /**
     * Utility helper class to encapsulate the information of a job transfer.
     */
    protected static class JobTransfer {
        private final Job job;
        private final long transferStartTime;
        private final Node destination;

        JobTransfer(Job job, long transferStartTime, Node destination) {
            this.job = job;
            this.transferStartTime = transferStartTime;
            this.destination = destination;
        }

        public Job getJob() {
            return job;
        }

        public long getTransferStartTime() {
            return transferStartTime;
        }

        public Node getDestination() {
            return destination;
        }
    }

    /**
     * Handler of messages containing {@link Job}s.
     */
    private class JobMessageHandler extends MessageHandler<Job> {

        @Override
        public void onMessageFullyReceived(Message<Job> message) {
            addJob(message.getData());
        }

        @Override
        public void onWillSendMessage(TransferInfo<Job> transferInfo) {
            if (transferInfo.getCurrentIndex() == 0) {
                JobStatsUtils.getInstance(simulation).transferResults(transferInfo.getData(), transferInfo.getDestination(), simulation.getTime());
                Job job = transferInfo.getData();
                Logger.getInstance(simulation).logEntity(Device.this, "Start transferring result; jobId=", job.getJobId());
            }
        }

        @Override
        public void onMessageSentAck(Message<Job> message) {
            int index = message.getOffset() + 1;
            Logger.getInstance(simulation).logEntity(Device.this, "Success Transfer (" + index + ")", "jobId=" + message.getData().getJobId());
        }

        @Override
        public void onMessageFullySent(Message<Job> message) {
            Job job = message.getData();
            Logger.getInstance(simulation).logEntity(Device.this, "Result completely transferred; jobId=", job.getJobId());
            JobStatsUtils.getInstance(simulation).successTransferBack(job);
            Device.this.outgoingJobTransfers.remove(job.getJobId());

            finishedJobsCompleted.add(message.getData());
        }

        @Override
        public void onCouldNotReceiveMessage(Message<Job> message) {
            Logger.getInstance(simulation).logEntity(Device.this, "Failed to onMessageReceived job " + message.getData().getJobId());
        }

        @Override
        public void onCouldNotSendMessage(TransferInfo<Job> transferInfo) {
            Logger.getInstance(simulation).logEntity(Device.this, "failed to send job result.", "jobId=" + transferInfo.getData().getJobId(),
                    "pendingJobs=" + transfersPending.size());
        }

        @Override
        public void onMessageSentFailedToArrive(Message<Job> message) {
            outgoingJobTransfers.remove(message.getData().getJobId());
        }
    }

    /**
     * Handler of messages containing {@link UpdateMessage}s.
     */
    private class UpdateMessageHandler extends MessageHandler<UpdateMessage> {

        @Override
        public void onWillSendMessage(TransferInfo<UpdateMessage> transferInfo) {
            if (transferInfo.getCurrentIndex() == 0) {
                JobStatsUtils.getInstance(simulation).registerUpdateMessage(Device.this, transferInfo.getData());
            }
        }
    }
}
