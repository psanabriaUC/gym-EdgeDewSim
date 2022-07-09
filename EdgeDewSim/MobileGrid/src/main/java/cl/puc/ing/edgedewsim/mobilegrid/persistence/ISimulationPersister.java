package cl.puc.ing.edgedewsim.mobilegrid.persistence;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.SimulationTuple;

import java.sql.SQLException;

public interface ISimulationPersister extends SQLSessionFactory {

    void insertSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException;

    void updateSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException;
}
