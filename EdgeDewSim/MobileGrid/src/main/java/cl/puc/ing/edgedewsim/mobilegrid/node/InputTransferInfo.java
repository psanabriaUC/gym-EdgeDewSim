package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;

public class InputTransferInfo extends TransferInfo<Job> {
    public Device device;
    public int nextJobId;

    public InputTransferInfo(Device device, Job job, long messagesCount, int currentIndex, int lastMessageSize) {
        super(device, job, messagesCount, lastMessageSize);
        this.device = device;
    }
}
