package cl.puc.ing.edgedewsim.mobilegrid.persistence;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobStatsTuple;

import java.sql.SQLException;

public interface IJobStatsPersister extends SQLSessionFactory {

    void insertJobStats(SQLSession session, JobStatsTuple jobStats) throws SQLException;

    void updateJobStats(SQLSession session, JobStatsTuple jobStats) throws SQLException;

}
