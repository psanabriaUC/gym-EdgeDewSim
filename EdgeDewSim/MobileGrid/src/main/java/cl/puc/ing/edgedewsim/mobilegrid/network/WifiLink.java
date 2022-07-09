package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.HashMap;
import java.util.Set;

public class WifiLink extends Link {

    //Contains information about the bandWith (kb/ms) of receiving 1 kb through a wifi connection depending on the wifi signal
    //strength (RSSI) measured in dBm. These bandWith apply when the whole data to be send is 100 kb.
    private static final HashMap<Short, Double> wifiRSSI_bandWith_100kb = new HashMap<>() {

        {
            put((short) -50, 0.4); //250 ms
            put((short) -80, 0.25);//400 ms
            put((short) -85, 0.166666667);//600 ms
            put((short) -90, 0.025641026);//3900ms
        }
    };

    //Contains information about the bandWith (kb/ms) of receiving 1 kb through a wifi connection depending on the wifi signal
    //strength (RSSI) measured in dBm. These bandWith apply when the whole data to be send is 10 kb.
    private static final HashMap<Short, Double> wifiRSSI_bandWith_10kb = new HashMap<>() {

        {
            put((short) -50, 0.08); //125 ms
            put((short) -80, 0.08);//125 ms
            put((short) -85, 0.044444444);//225 ms
            put((short) -90, 0.011428571);//875ms
        }
    };


    final short rssi;

    public WifiLink(short rssi, Set<Node> sources,
                    Set<Node> destinations, Simulation simulation) {
        super(0, 0, sources, destinations, simulation);
        this.rssi = rssi;
    }

    @Override
    public boolean canSend(Node scr, Node dst) {
        return source.contains(scr) && destinations.contains(dst);
    }

    @Override
    public long getTransmissionTime(long totalTransferSize, int size) {
        double s = size / 1024.0; //byte->kb
        double bw;// kb/ms
        if (totalTransferSize / 1024.0 > 100)
            bw = WifiLink.wifiRSSI_bandWith_100kb.get(rssi);
        else
            bw = WifiLink.wifiRSSI_bandWith_10kb.get(rssi);

        //Proxy-->Device(S) || Device(R)-->Proxy
        double d = this.delay;
        //((long)(s*1000.0/bw+d));
        return ((long) (s / bw + d));// ms
    }
}
