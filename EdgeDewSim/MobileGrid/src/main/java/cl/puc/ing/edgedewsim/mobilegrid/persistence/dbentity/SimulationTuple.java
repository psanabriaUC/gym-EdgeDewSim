package cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity;

import java.sql.Timestamp;


public class SimulationTuple {

    private int simId = -1;
    private String name = "";
    private String scheduler = "";
    private String comparator = "";
    private String policy = "";
    private String strategy = "";
    private String condition = "";
    private String link = "";
    private String topologyFile = "";
    private String baseProfile = "";
    private String jobsFile = "";
    private Timestamp startTime = null;

    public SimulationTuple() {

    }

    public int getSimId() {
        return simId;
    }

    public void setSimId(int simId) {
        this.simId = simId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScheduler() {
        return scheduler;
    }

    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTopologyFile() {
        return topologyFile;
    }

    public void setTopologyFile(String topologyFile) {
        this.topologyFile = topologyFile;
    }

    public String getBaseProfile() {
        return baseProfile;
    }

    public void setBaseProfile(String baseProfile) {
        this.baseProfile = baseProfile;
    }

    public String getJobsFile() {
        return jobsFile;
    }

    public void setJobsFile(String jobsFile) {
        this.jobsFile = jobsFile;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }


}
