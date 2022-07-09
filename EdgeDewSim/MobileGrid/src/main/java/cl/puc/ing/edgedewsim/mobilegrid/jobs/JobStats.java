package cl.puc.ing.edgedewsim.mobilegrid.jobs;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;

import java.util.ArrayList;
import java.util.List;

public class JobStats {

    // Statistics of all results stemming from processing jobs sent back to the original emitter.
    private final List<Node> resultsTransfers = new ArrayList<>();
    private final List<Long> resultsTransfersTimes = new ArrayList<>();
    protected boolean assigned = false;
    protected boolean success = false;
    protected boolean successTransferBack = false;
    protected boolean rejected = false;
    protected boolean fromEdge = false;

    /**
     * Statistics of all job-related data sent to entities meant to process them.
     */
    protected final List<TransferStatus> transfersInfo = new ArrayList<>();
    //this time is set when the job enters the mobile grid
    protected long startTime;

    //this value represent the executed mips of the job and is set at the same time the finishTime is set
    protected long executedMips = 0L;

    //this time is set when the job starts to be executed by a node
    protected long startExecutionTime = -1;

    //this time is set when the job finishes normally or abnormally. It remains in -1 value only when the job execution time was never set
    protected long finishTime = -1;


    public JobStats(long startTime, Node node) {
        super();
        this.startTime = startTime;

        this.transfersInfo.add(new TransferStatus(true, node, startTime, startTime));
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned() {
        assigned = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isCompleted() {
        return success && successTransferBack;
    }

    public List<Node> getTransfers() {
        List<Node> nodes = new ArrayList<>();
        for (TransferStatus transferStatus : this.transfersInfo) {
            nodes.add(transferStatus.node);
        }
        return nodes;
    }


    public void addTransfers(Node node, long time, long startTime) {
        this.transfersInfo.add(new TransferStatus(true, node, startTime, time));

	    /*
		this.transfersCompleted.add(false);
		this.transfers.add(node);
		this.transferTimes.add(time);
		this.startTransferTimes.add(startTime);
		*/
    }

    public void addResultsTransfers(Node node, long time) {
        this.resultsTransfers.add(node);
        this.resultsTransfersTimes.add(time);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setLastTransferTime(long time, long startTime) {
        TransferStatus lastTransferInfo = this.transfersInfo.get(this.transfersInfo.size() - 1);

        lastTransferInfo.startTransferTime = startTime;
        lastTransferInfo.transferTime = time;
    }

    public long getStartExecutionTime() {
        return startExecutionTime;
    }

    public void setStartExecutionTime(long startExecutionTime) {
        this.startExecutionTime = startExecutionTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public long getTotalTransferTime() {
        long total = 0;
        for (TransferStatus transferStatus : transfersInfo) {
            total += transferStatus.transferTime;
        }
        return total;
    }

    public long getTotalResultsTransferTime() {
        long t = 0;
        for (Long l : this.resultsTransfersTimes)
            t += l;
        return t;
    }

    public long getTotalTime() {
        return this.finishTime - this.startTime;
    }

    public long getTotalExecutionTime() {
        if (this.startExecutionTime == -1) return -1;
        return this.finishTime - this.startExecutionTime;//be careful with this value, very small jobs may have 0 milliseconds of execution time
    }

    public boolean statedToExecute() {
        return this.startExecutionTime != -1;
    }

    public long getQueueTime() {
        long result;
        if (this.isSuccess())
            result = this.startExecutionTime - this.startTime;
        else
            result = this.finishTime - this.startTime;
        return result;
    }

    public void successTransferredBack() {
        this.successTransferBack = true;
    }

    public void setLastResultTransferTime(long time) {
        TransferStatus lastTransferInfo = this.transfersInfo.get(this.transfersInfo.size() - 1);
        lastTransferInfo.transferTime = time;
    }

    public int getTotalTransfers() {
        return this.transfersInfo.size();
    }

    public int getTotalResultTransfers() {
        return this.resultsTransfers.size();
    }

    public boolean executedSuccessButNotTransferredBack() {
        return (this.success && !this.successTransferBack);
    }

    /**
     * public void transferredBackInitiated() {
     * this.transferBackInitiated =true;
     * }
     * <p>
     * public boolean isTransferBackInitiated() {
     * return transferBackInitiated;
     * }
     */

    public void setJobTransferCompleted(Node node) {
        /* TODO: in the case the job is sent several times to the same node, this method will set as complete the first transferring. Provide a fix
         *  to this behavior if this case becomes usual in simulations */

        TransferStatus transferStatus = null;
        for (TransferStatus item : this.transfersInfo) {
            if (item.node == node) {
                transferStatus = item;
                break;
            }
        }

        if (transferStatus == null) {
            throw new IllegalStateException("Marked a job as 'complete' without ever having it sent first.");
        }

        transferStatus.transferCompleted = true;
    }

    public boolean wasReceivedByAWorkerNode() {
        //first node of the transfer node list is ignored because it is considered that is the node from which de job was scheduled
        for (int i = 1; i < this.transfersInfo.size(); i++) {
            if (transfersInfo.get(i).transferCompleted) {
                return true;
            }
        }
        return false;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public long getExecutedMips() {
        return executedMips;
    }

    public void setExecutedMips(long executedMips) {
        this.executedMips = executedMips;
    }

    public boolean isFromEdge() {
        return fromEdge;
    }

    public void setFromEdge(boolean fromEdge) {
        this.fromEdge = fromEdge;
    }

    protected static class TransferStatus {
        private final Node node;
        private boolean transferCompleted;
        private long startTransferTime;
        private long transferTime;

        public TransferStatus(boolean transferCompleted, Node node, long startTransferTime, long transferTime) {
            this.transferCompleted = transferCompleted;
            this.node = node;
            this.startTransferTime = startTransferTime;
            this.transferTime = transferTime;
        }

        public boolean isTransferCompleted() {
            return transferCompleted;
        }

        public Node getNode() {
            return node;
        }

        public long getStartTransferTime() {
            return startTransferTime;
        }

        public long getTransferTime() {
            return transferTime;
        }
    }


}