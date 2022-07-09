package cl.puc.ing.edgedewsim.persistence.mybatis;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import org.apache.ibatis.session.SqlSession;


public class IbatisSQLSession implements SQLSession {

    SqlSession sessionInstance;

    public IbatisSQLSession(SqlSession sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public void close() {
        sessionInstance.close();

    }

    public void commit() {
        sessionInstance.commit(true);
    }

    public SqlSession unwrap() {
        return sessionInstance;
    }

}
