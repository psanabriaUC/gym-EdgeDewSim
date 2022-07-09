package cl.puc.ing.edgedewsim.persistence.mybatis;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.*;

public class MybatisPersisterFactory implements IPersisterFactory {

    IDevicePersister devicePersister = null;
    IJobStatsPersister jobStatsPersister = null;
    IJobTransferredPersister jobTransferredPersister = null;
    ISimulationPersister simPersister = null;

    @Override
    public IDevicePersister getDevicePersister() {
        if (devicePersister == null)
            devicePersister = new DevicePersister();
        return devicePersister;
    }

    @Override
    public IJobStatsPersister getJobStatsPersister() {
        if (jobStatsPersister == null)
            jobStatsPersister = new JobStatsPersister();
        return jobStatsPersister;
    }

    @Override
    public IJobTransferredPersister getJobTransferredPersister() {
        if (jobTransferredPersister == null)
            jobTransferredPersister = new JobTransferredPersister();
        return jobTransferredPersister;
    }

    @Override
    public ISimulationPersister getSimulationPersister() {
        if (simPersister == null)
            simPersister = new SimulationPersister();
        return simPersister;
    }

}
