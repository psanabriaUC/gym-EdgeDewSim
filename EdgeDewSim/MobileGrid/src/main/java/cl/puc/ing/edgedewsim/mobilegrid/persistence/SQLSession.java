package cl.puc.ing.edgedewsim.mobilegrid.persistence;

public interface SQLSession {

    void close();

    void commit();

}
