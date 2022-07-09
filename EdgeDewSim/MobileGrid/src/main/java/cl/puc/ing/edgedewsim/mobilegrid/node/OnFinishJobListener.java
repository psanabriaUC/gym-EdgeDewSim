package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;

public interface OnFinishJobListener {
    void finished(Device device, Job job);
}
