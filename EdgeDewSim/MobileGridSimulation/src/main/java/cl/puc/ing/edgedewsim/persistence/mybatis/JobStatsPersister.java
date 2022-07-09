package cl.puc.ing.edgedewsim.persistence.mybatis;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobStatsTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IJobStatsPersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;

public class JobStatsPersister extends IbatisSQLSessionFactory implements IJobStatsPersister {


    @Override
    public void insertJobStats(SQLSession session, JobStatsTuple jobStats)
            throws SQLException {
        SqlSession ibatisSession = ((IbatisSQLSession) session).unwrap();
        try {
            ibatisSession.insert("insertJobStats", jobStats);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void updateJobStats(SQLSession session, JobStatsTuple jobStats)
            throws SQLException {
        // TODO Auto-generated method stub

    }

}
