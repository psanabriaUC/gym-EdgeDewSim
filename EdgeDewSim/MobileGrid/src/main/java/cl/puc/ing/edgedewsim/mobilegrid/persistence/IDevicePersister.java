package cl.puc.ing.edgedewsim.mobilegrid.persistence;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.DeviceTuple;

import java.sql.SQLException;

public interface IDevicePersister extends SQLSessionFactory {

    void saveDeviceIntoMemory(String name, DeviceTuple deviceTuple);

    void insertDevice(SQLSession session, DeviceTuple device) throws SQLException;

    void insertInMemoryDeviceTuples(SQLSession session);

    DeviceTuple getDevice(String name);

}
