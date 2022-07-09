package cl.puc.ing.edgedewsim.persistence.mybatis;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobTransfer;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IJobTransferredPersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;

public class JobTransferredPersister extends IbatisSQLSessionFactory implements IJobTransferredPersister {

    @Override
    public void insertJobTransferred(SQLSession session, JobTransfer jobTransfer)
            throws SQLException {
        SqlSession ibatisSession = ((IbatisSQLSession) session).unwrap();
        try {
            ibatisSession.insert("insertJobTransfered", jobTransfer);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }

    }

}
