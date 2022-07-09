package cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity;

public class JobTransfer {

    private final boolean lastHop;
    private Integer jobTransferredId;
    private Integer jobStatsId;
    private Integer fromDeviceId;
    private Integer toDeviceId;
    private Integer hop;
    private Long time;
    private Long startTime;

    public JobTransfer(Integer jobStatsId, Integer fromDeviceId, Integer toDeviceId, Long time, Integer hop, Long startTime, boolean lastHop) {
        this.jobStatsId = jobStatsId;
        this.setFromDeviceId(fromDeviceId);
        this.toDeviceId = toDeviceId;
        this.time = time;
        this.hop = hop;
        this.setStartTime(startTime);
        this.lastHop = lastHop;
    }


    public Integer getJobStatsId() {
        return jobStatsId;
    }

    public void setJobStatsId(Integer jobStatsId) {
        this.jobStatsId = jobStatsId;
    }

    public boolean isLastHop() {
        return lastHop;
    }

    public Integer getToDeviceId() {
        return toDeviceId;
    }


    /**
     * @param device_id the id of device where the job is transferred to
     */
    public void setToDeviceId(Integer device_id) {
        this.toDeviceId = device_id;
    }


    public Long getTime() {
        return time;
    }


    public void setTime(Long time) {
        this.time = time;
    }


    public Integer getHop() {
        return this.hop;
    }


    public void setHop(Integer hop) {
        this.hop = hop;
    }

    public Integer getJobTransferredId() {
        return jobTransferredId;
    }


    public void setJobTransferredId(Integer jobTransferredId) {
        this.jobTransferredId = jobTransferredId;
    }


    public Long getStartTime() {
        return startTime;
    }


    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }


    public Integer getFromDeviceId() {
        return fromDeviceId;
    }


    public void setFromDeviceId(Integer fromDeviceId) {
        this.fromDeviceId = fromDeviceId;
    }

}
