package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;

import java.util.Comparator;

public class AscendingAggregatedJobDataComparator implements Comparator<Job> {

    @Override
    public int compare(Job job1, Job job2) {
        long job1value = job1.getInputSize() + job1.getOutputSize();
        long job2value = job2.getInputSize() + job2.getOutputSize();

        return Long.compare(job1value, job2value);
    }

}
