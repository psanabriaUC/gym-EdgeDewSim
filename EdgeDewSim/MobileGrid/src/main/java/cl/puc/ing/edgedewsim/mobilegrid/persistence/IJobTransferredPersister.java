package cl.puc.ing.edgedewsim.mobilegrid.persistence;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobTransfer;

import java.sql.SQLException;

public interface IJobTransferredPersister extends SQLSessionFactory {

    void insertJobTransferred(SQLSession session, JobTransfer jobTransfer) throws SQLException;

}
