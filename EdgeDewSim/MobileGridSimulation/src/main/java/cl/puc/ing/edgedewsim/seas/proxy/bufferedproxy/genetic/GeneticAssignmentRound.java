package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;

import java.util.ArrayList;

public class GeneticAssignmentRound {

    //data needed to start a scheduling genetic round
    private final ArrayList<Job> jobsToSchedule;
    private final double totalDataToBeSchedule;


    //data generated after the scheduling genetic round
    private ArrayList<DataAssignment> assignment;

    //information data of the current scheduling genetic round
    private GAExecInformation gaInfo;
    private long assignmentStartTime;
    private long assignmentFinishedTime;

    public GeneticAssignmentRound(ArrayList<Job> jobsToSchedule, double totalJobDataToBeSchedule) {
        this.jobsToSchedule = jobsToSchedule;
        this.totalDataToBeSchedule = totalJobDataToBeSchedule;
    }

    public ArrayList<DataAssignment> getAssignment() {
        return assignment;
    }

    public void setAssignment(ArrayList<DataAssignment> assignment) {
        this.assignment = assignment;
    }

    public GAExecInformation getGaInfo() {
        return gaInfo;
    }

    public void setGaInfo(GAExecInformation gaInfo) {
        this.gaInfo = gaInfo;
    }

    public long getAssignmentFinishedTime() {
        return assignmentFinishedTime;
    }

    public void setAssignmentFinishedTime(long assignmentFinishedTime) {
        this.assignmentFinishedTime = assignmentFinishedTime;
    }

    public ArrayList<Job> getJobsToSchedule() {
        return jobsToSchedule;
    }

    public double getTotalDataToBeSchedule() {
        return totalDataToBeSchedule;
    }

    public long getAssignmentStartTime() {
        return assignmentStartTime;
    }

    public void setAssignmentStartTime(long assignmentStartTime) {
        this.assignmentStartTime = assignmentStartTime;
    }

    public Job getJob(int job) {
        if (jobsToSchedule != null && job < jobsToSchedule.size())
            return jobsToSchedule.get(job);

        return null;
    }

    public double getGenesAmount() {
        return jobsToSchedule.size();
    }


}
