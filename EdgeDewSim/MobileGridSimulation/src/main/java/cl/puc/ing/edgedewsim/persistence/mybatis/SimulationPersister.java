package cl.puc.ing.edgedewsim.persistence.mybatis;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.SimulationTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.ISimulationPersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;

public class SimulationPersister extends IbatisSQLSessionFactory implements ISimulationPersister {

    public SimulationPersister() {
    }


    @Override
    public void insertSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException {
        SqlSession ibatisSession = ((IbatisSQLSession) session).unwrap();
        try {
            ibatisSession.insert("insertSimulation", simulationTuple);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void updateSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException {
        SqlSession ibatisSession = ((IbatisSQLSession) session).unwrap();
        try {
            ibatisSession.update("updateSimulation", simulationTuple);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }

    }

}
