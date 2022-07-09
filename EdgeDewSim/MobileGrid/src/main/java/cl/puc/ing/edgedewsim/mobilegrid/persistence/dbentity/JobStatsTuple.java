package cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStats;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.*;
import cl.puc.ing.edgedewsim.simulator.Entity;

import java.sql.SQLException;

public class JobStatsTuple extends JobStats {

    private static IJobStatsPersister jsp = null;
    private static IJobTransferredPersister jtp = null;
    private static IDevicePersister dp = null;
    private int jobId;
    private int jobStatsId;
    private int simId;

    public JobStatsTuple(int jobId, int simId, long transferTime, Node node) {
        super(transferTime, node);
        this.jobId = jobId;
        this.simId = simId;
        this.jobStatsId = -1;
    }

    public static void setIPersisterFactory(IPersisterFactory pf) {
        jtp = pf.getJobTransferredPersister();
        jsp = pf.getJobStatsPersister();
        dp = pf.getDevicePersister();
    }

    private static void insertJobStats(SQLSession session, JobStatsTuple stat) {

        try {
            jsp.insertJobStats(session, stat);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertJobTransfer(SQLSession session, JobStatsTuple stat, Entity originNode, Entity destNode, long time, int hop, long startTime, boolean lastHop) {
        Integer from_deviceId = null;
        if (originNode != null)
            from_deviceId = dp.getDevice(originNode.getName()).getDevice_id();
        Integer to_deviceId = dp.getDevice(destNode.getName()).getDevice_id();
        JobTransfer jt = new JobTransfer(stat.getJobStatsId(), from_deviceId, to_deviceId, time, hop, startTime, lastHop);
        try {
            jtp.insertJobTransferred(session, jt);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void persist(SQLSession session) {

        insertJobStats(session, this);
        Node firstNode = transfersInfo.get(0).getNode();
        boolean lastHop = transfersInfo.size() == 1;//in other words if there is only one transfer for the job then this transfer is the last hop of the chain of transfers

        if (!(firstNode instanceof Device)) {
            insertJobTransfer(session, this, null, (Entity) firstNode, 0, 0, startTime, lastHop); //the transfer time between the an out of the grid device and the proxy does not matter so for that reason is zero.
        }

        for (int i = 1; i < transfersInfo.size() - 1; i++) {
            Entity originNode = (Entity) transfersInfo.get(i - 1).getNode();
            Entity destNode = (Entity) transfersInfo.get(i).getNode();
            insertJobTransfer(session, this, originNode, destNode, transfersInfo.get(i - 1).getTransferTime(),
                    i, transfersInfo.get(i - 1).getStartTransferTime(), lastHop);
        }

        if (!lastHop) {
            Entity originNode = (Entity) transfersInfo.get(transfersInfo.size() - 2).getNode();
            Entity destNode = (Entity) transfersInfo.get(transfersInfo.size() - 1).getNode();
            insertJobTransfer(session, this, originNode, destNode, transfersInfo.get(transfersInfo.size() - 1).getTransferTime(),
                    transfersInfo.size() - 1, transfersInfo.get(transfersInfo.size() - 1).getStartTransferTime(), true);
        }
    }

    public boolean isSuccessTransferBack() {
        return this.successTransferBack;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getSimId() {
        return simId;
    }

    public void setSimId(int simId) {
        this.simId = simId;
    }

    public int getJobStatsId() {
        return jobStatsId;
    }

    public void setJobStatsId(int jobStatsId) {
        this.jobStatsId = jobStatsId;
    }

    public int getLastTransferredNode() {
        Entity executorNode = (Entity) transfersInfo.get(transfersInfo.size() - 1).getNode();
        return dp.getDevice(executorNode.getName()).getDevice_id();
    }

}
