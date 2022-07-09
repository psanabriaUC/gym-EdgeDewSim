package cl.puc.ing.edgedewsim.mobilegrid.persistence;

public interface IPersisterFactory {

    IDevicePersister getDevicePersister();

    IJobStatsPersister getJobStatsPersister();

    IJobTransferredPersister getJobTransferredPersister();

    ISimulationPersister getSimulationPersister();

}
