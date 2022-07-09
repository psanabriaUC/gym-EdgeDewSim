package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.network.NetworkModel;
import cl.puc.ing.edgedewsim.mobilegrid.network.UpdateMessage;
import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for the implementation of a centralized proxy that assigns jobs to arbitrary amounts for nodes
 * in a cluster.
 */
public abstract class SchedulerProxy extends Entity implements Node, DeviceListener {

    public static final int EVENT_JOB_ARRIVE = 1;
    public static final int EVENT_GENETIC_ALGORITHM_BATCH_FINISHED = 2;
    public static final int EVENT_END_JOB_ARRIVAL_BURST = 3;
    /* Size of message buffer for transfers in bytes */
    private static final int MESSAGE_SIZE = 1024 * 1024;// 1mb
    /**
     * A static reference to the proxy so it can be accessed from anywhere.
     */
    private static final ConcurrentHashMap<UUID, SchedulerProxy> proxyHashMap = new ConcurrentHashMap<>();
    private static final Object sLock = new Object();
    /**
     * Information currently known by the proxy about its different nodes. Maps the Node to the object containing
     * its data.
     */
    private final WeakHashMap<Node, DeviceData> deviceDataMap = new WeakHashMap<>();
    protected final HashMap<String, Device> devices = new HashMap<>();

    public SchedulerProxy(String name, Simulation simulation) {
        super(name, simulation);
        addProxy(simulation, this);
        this.simulation.addEntity(this);
        NetworkModel.getModel(this.simulation).addNewNode(this);
        Logger.getInstance(simulation).logEntity(this, "Proxy created", this.getClass().getName());
    }

    public static SchedulerProxy getProxyInstance(Simulation simulation) {
        return proxyHashMap.get(simulation.getId());
    }

    private static void addProxy(Simulation simulation, SchedulerProxy proxy) {
        synchronized (sLock) {
            proxyHashMap.put(simulation.getId(), proxy);
        }
    }

    public static void removeProxy(Simulation simulation) {
        synchronized (sLock) {
            if (proxyHashMap.get(simulation.getId()) != null)
                proxyHashMap.remove(simulation.getId());
        }
    }

    public static void resetProxy(Simulation simulation) {
        synchronized (sLock) {
            SchedulerProxy proxy = proxyHashMap.get(simulation.getId());
            if (proxy != null)
                proxy.reset();
        }
    }

    /**
     * Processes an event dispatched by the {@link Simulation}.
     *
     * @param event The event that will be processed.
     */
    public abstract void processEvent(Event event);

    /**
     * returns the remaining energy of the grid by aggregating the remaining energy of each node
     * that compose it. Remaining energy of a node is derived from the last state of charge update
     * message received by the proxy node.
     * The value is expressed in Joules
     **/
    public double getCurrentAggregatedNodesEnergy() {
        double currentAggregatedEnergy = 0;
        for (Device dev : devices.values()) {
            currentAggregatedEnergy += getJoulesBasedOnLastReportedSOC(dev); //dev.getJoulesBasedOnLastReportedSOC();
        }
        return currentAggregatedEnergy;
    }


    @Override
    public void startTransfer(@NotNull Node dst, int id, @Nullable Object data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void incomingData(@NotNull Node scr, int id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void failReception(@NotNull Node scr, int id) {
        // TODO Auto-generated method stub
    }

    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {
        Object data = message.getData();

        if (data instanceof UpdateMessage) {
            UpdateMessage msg = (UpdateMessage) data;
            Device device = devices.get(msg.getNodeId());
            Logger.getInstance(simulation).logEntity(this, "Battery update received from device " + msg.getNodeId() +
                    " value=" + msg.getPercentageOfRemainingBattery());
            updateDeviceSOC(device, msg.getPercentageOfRemainingBattery());
            // device.setLastBatteryLevelUpdate(msg.getPercentageOfRemainingBattery());
            JobStatsUtils.getInstance(simulation).registerUpdateMessage(this, (UpdateMessage) data);
        }


        // When we receive a message from a device, we check if we have any data to send to the device we got the
        // message from and start sending it to it.
        Device device = (Device) message.getSource();

        if (!deviceDataMap.get(device).pendingTransfers.isEmpty()) {
            TransferInfo<?> transferInfo = Objects.requireNonNull(deviceDataMap.get(device).pendingTransfers.peek());
            int messageSize = transferInfo.getMessageSize(MESSAGE_SIZE);

            if (transferInfo.getCurrentIndex() == 0) {
                setJobTotalTransferringTime(transferInfo);
            }
            continueTransferring(transferInfo);
        }

    }

    public void updateDeviceSOC(Device device, int newBatteryLevel) {
        this.deviceDataMap.get(device).lastReportedStateOfCharge = newBatteryLevel;
        this.deviceDataMap.get(device).joulesBasedOnLastReceivedSOC = device.getJoulesBasedOnLastReceivedSOC(newBatteryLevel);
    }

    /**
     * returns the available Joules of the device based on the value of the last
     * reported SOC
     */
    public double getJoulesBasedOnLastReportedSOC(Device device) {
        return this.deviceDataMap.get(device).joulesBasedOnLastReceivedSOC;
    }

    public int getLastReportedSOC(Device device) {
        return this.deviceDataMap.get(device).lastReportedStateOfCharge;
    }

    private void setJobTotalTransferringTime(@NotNull TransferInfo<?> transferInfo) {
        // FIXME: this code should be changed/fixed when variable cost of transferring for a node be implemented

        if (transferInfo.getData() instanceof Job) {
            Job job = (Job) transferInfo.getData();

            long currentSimTime = simulation.getTime();
            long totalTime = (transferInfo.getMessagesCount() - 1) * NetworkModel.getModel(simulation)
                    .getTransmissionTime(transferInfo.getDestination(), getProxyInstance(simulation), transferInfo.getTotalTransferSize(MESSAGE_SIZE), MESSAGE_SIZE);
            totalTime += NetworkModel.getModel(simulation).getTransmissionTime(getProxyInstance(simulation), transferInfo.getDestination(), transferInfo.getTotalTransferSize(MESSAGE_SIZE),
                     transferInfo.getLastMessageSize());
            JobStatsUtils.getInstance(simulation).transfer(job, transferInfo.getDestination(), totalTime, currentSimTime);
        }
    }

    /**
     * Called when a message originating from this entity was successfully received by a node in the network.
     *
     * @param messageSent The message sent.
     */
    @Override
    public <T> void onMessageSentAck(@NotNull Message<T> messageSent) {
        // This is the id of the node that received the message.
        // int destinationNodeId = messageSent.getDestination().getId();

        DeviceData deviceData = this.deviceDataMap.get(messageSent.getDestination());
        TransferInfo<?> transferInfo = deviceData.pendingTransfers.peek();
        // Should never be null, but we check just in case.
        if (transferInfo != null) {
            // If this is the last fragment of the message we are currently transmitting, we
            // remove the current
            // transfer info from the queue.
            if (transferInfo.isLastMessage()) {

                deviceData.pendingTransfers.remove();
                deviceData.completedTransfers.add(transferInfo);

                if (transferInfo.getData() instanceof Job) {
                    Job job = (Job) transferInfo.getData();
                    deviceData.incomingJobs--;

                    Logger.getInstance(simulation).logEntity(this, "Job transfer finished ", job.getJobId(), transferInfo.getDestination());
                    //JobStatsUtils.getInstance(simulation).setJobTransferCompleted(job, transferInfo.getDestination());
                }
            } else {
                if (transferInfo.getCurrentIndex() == 0) {
                    setJobTotalTransferringTime(transferInfo);
                }
                transferInfo.increaseIndex();
            }

            // If we have more data to send to the device (either additional fragments of
            // the previous message or the
            // first fragment of a new message), and the receiver device is currently not
            // busy, we send the data.
            if (!deviceData.pendingTransfers.isEmpty() && !transferInfo.getDestination().isSending() && !transferInfo.getDestination().isReceiving()) {
                TransferInfo<?> nextTransfer = Objects.requireNonNull(deviceData.pendingTransfers.peek());

                if (nextTransfer != transferInfo) {
                    setJobTotalTransferringTime(nextTransfer);
                }
                continueTransferring(nextTransfer);
            }
        } else {
            //TODO: Review for job stealing
            throw new IllegalStateException("An ack message of a transfer that yet not exist or is no longer active, has been received by the Proxy;" + messageSent.toString());
        }
    }

    @Override
    public <T> void fail(@NotNull Message<T> message) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isOnline() {
        return true;
    }

    private long sendMessage(Node destination, Object data, long totalTransferSize, int messageSize, int offset, boolean lastMessage) {
        return NetworkModel.getModel(simulation).send(this, destination, destination.getId(), totalTransferSize,
                messageSize, data, offset, lastMessage);
    }

    public void remove(Device device) {
        this.devices.remove(device.getName());
    }

    public void addDevice(Device device) {
        this.devices.put(device.getName(), device);
        this.deviceDataMap.put(device, new DeviceData(device.getInitialSOC(), device.getInitialJoules()));
    }

    public void reset() {
        deviceDataMap.clear();
        devices.clear();
    }

    @Override
    public void onDeviceFail(Node e) {
        // TODO Auto-generated method stub
    }

    /**
     * Queues a {@link Job} for transferring as soon as the channel between the proxy and the device becomes available.
     * Jobs are transferred in FIFO order.
     *
     * @param device The device to which assign the job.
     * @param job    The job to assign.
     */
    protected void queueJobTransferring(final Device device, final Job job) {
        Logger.getInstance(simulation).logEntity(this, "Job assigned to ", job.getJobId(), device);
        // device.incrementIncomingJobs();

        incrementIncomingJobs(device);
        JobStatsUtils.getInstance(simulation).setJobAssigned(job);

        queueMessageTransfer(device, job, job.getInputSize(), (destination, ETA) -> {
            long currentSimTime = simulation.getTime();
            Logger.getInstance(simulation).logEntity(this, "Initiating Job transferring to ", job.getJobId(), device);
            JobStatsUtils.getInstance(simulation).transfer(job, device, ETA - currentSimTime, currentSimTime);
        });
    }

    /**
     * Queues an arbitrary message for transfer to a given recipient.
     *
     * @param destination The receiver of the message.
     * @param data        The data to send.
     * @param messageSize The size of the data in bytes.
     */
    @SuppressWarnings("SameParameterValue")
    protected <T> void queueMessageTransfer(Node destination, T data, long messageSize) {
        queueMessageTransfer(destination, data, messageSize, null);
    }

    /**
     * Queues an arbitrary message for transfer to a given recipient. Callers may additionally specify a delegate to
     * invoke if we are able to immediately send the message (this only happens if the channel is empty and no previous
     * messages exist in the queue).
     *
     * @param destination The receiver of the message.
     * @param data        The data to send.
     * @param dataSize The size of the data in bytes.
     * @param delegate    A delegate to invoke in case the message is sent immediately.
     */
    protected <T> void queueMessageTransfer(Node destination, T data, long dataSize, OnInitiatingDataTransfer delegate) {

        long subMessagesCount = (long) Math.ceil(dataSize / (double) MESSAGE_SIZE);
        int lastMessageSize = (int) (dataSize - (subMessagesCount - 1) * MESSAGE_SIZE);

        TransferInfo<?> transferInfo = new TransferInfo<>(destination, data, subMessagesCount, lastMessageSize, delegate);

        DeviceData deviceData = deviceDataMap.get(destination);
        deviceData.pendingTransfers.add(transferInfo);

        // If the pending transfers queue for the given device is not empty, we say the transfer channel is busy.
        // boolean channelBusy = !deviceData.pendingTransfers.isEmpty(); //before change
        boolean channelBusy = destination.isSending() || destination.isReceiving();    //que tal si tambien se pregunta si	el destino esta recibiendo?
        if (!channelBusy) {// If the channel is not busy, we send the first message for the given job.
            TransferInfo<?> nextTransferInfo = Objects.requireNonNull(deviceData.pendingTransfers.peek());
            continueTransferring(nextTransferInfo);
        }
    }

    private void continueTransferring(TransferInfo<?> nextTransferInfo) {
        long time = sendMessage(nextTransferInfo.getDestination(), nextTransferInfo.getData(),
                nextTransferInfo.getTotalTransferSize(MESSAGE_SIZE),
                nextTransferInfo.getMessageSize(MESSAGE_SIZE),
                nextTransferInfo.getCurrentIndex(), nextTransferInfo.isLastMessage());

        OnInitiatingDataTransfer delegator = nextTransferInfo.getOnMessageSentHandler();
        if (delegator != null && nextTransferInfo.getCurrentIndex() == 0)
            delegator.onInitiatingDataTransfer(nextTransferInfo.getDestination(), time);
    }

    protected void incrementIncomingJobs(Node node) {
        this.deviceDataMap.get(node).incomingJobs++;
    }

    // Getters

    public Collection<Device> getDevices() {
        return this.devices.values();
    }

    @Override
    public boolean isSending() {
        return false;
    }

    @Override
    public boolean isReceiving() {
        return false;
    }


    public List<TransferInfo<?>> getTransfersPending() {
        List<TransferInfo<?>> transfers = new ArrayList<>();
        for (DeviceData deviceData : this.deviceDataMap.values()) {
            transfers.addAll(deviceData.pendingTransfers);
        }

        return transfers;
    }

    public List<TransferInfo<?>> getTransfersCompleted() {
        List<TransferInfo<?>> transfers = new ArrayList<>();
        for (DeviceData deviceData : this.deviceDataMap.values()) {
            transfers.addAll(deviceData.completedTransfers);
        }

        return transfers;
    }

    /**
     * Gets the number of queued jobs that are in a state of being transferred towards a given device (but haven't
     * been fully sent yet).
     *
     * @param node A node in the grid.
     * @return The amount of jobs queued for transfer against the given node.
     */
    public int getIncomingJobs(Node node) {
        return this.deviceDataMap.get(node).incomingJobs;
    }

    public interface OnInitiatingDataTransfer {
        void onInitiatingDataTransfer(Node destination, long ETA);
    }

    /**
     * Known information of the proxy for a given {@link Device}.
     */
    private static class DeviceData {
        /**
         * Estimated Joules computed based on the last battery state of charge received
         */
        private double joulesBasedOnLastReceivedSOC;

        /**
         * This amount of jobs currently enqueued to be transferred to a {@link Device}.
         */
        private int incomingJobs;
        /**
         * The last reported state of charge of a {@link Device} through an {@link UpdateMessage} event. Actual state of
         * charges should actually be slightly lower than what the proxy knows at any given time.
         */
        private int lastReportedStateOfCharge;

        /**
         * Hash map of message transfer queues for each device in the network. Maps the ID of the device
         * to its respective transfer queue.
         */
        private final Queue<TransferInfo<?>> pendingTransfers = new LinkedList<>();
        /**
         * Hash map of all messages that have been sent through a given channel with one
         * device. Maps the ID of the device with its respective transfer queue. Used
         * for logging purposes.
         */
        private final Queue<TransferInfo<?>> completedTransfers = new LinkedList<>();

        DeviceData(int lastReportedStateOfCharge, double joules) {
            incomingJobs = 0;
            this.lastReportedStateOfCharge = lastReportedStateOfCharge;
            this.joulesBasedOnLastReceivedSOC = joules;
        }
    }

}