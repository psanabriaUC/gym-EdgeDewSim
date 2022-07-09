package cl.puc.ing.edgedewsim.persistence.mybatis;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSessionFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;


public class IbatisSQLSessionFactory implements SQLSessionFactory {

    private final Integer mux = 1;
    private SqlSessionFactory sqlMapper;

    public IbatisSQLSessionFactory() {
        String resource = "db/sqlMapConfig.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            sqlMapper = new SqlSessionFactoryBuilder().build(reader);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public SQLSession openSQLSession() {
        synchronized (mux) {
            return new IbatisSQLSession(sqlMapper.openSession());
        }
    }

}
