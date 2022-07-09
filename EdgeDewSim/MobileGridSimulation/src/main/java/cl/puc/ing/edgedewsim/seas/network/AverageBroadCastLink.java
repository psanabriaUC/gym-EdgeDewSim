package cl.puc.ing.edgedewsim.seas.network;

import cl.puc.ing.edgedewsim.mobilegrid.network.BroadCastLink;
import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Random;
import java.util.Set;

public class AverageBroadCastLink extends BroadCastLink {

    protected double stdDelay = 0;
    protected double stdBandwidht = 0;
    protected Random delayRandom = new Random();
    protected Random bandwidhtRandom = new Random();

    public AverageBroadCastLink(Simulation simulation) {
        super(0, 0, null, null, simulation);
    }

    public AverageBroadCastLink(int delay, int bandwidth, Set<Node> source,
                                Set<Node> destinations, Simulation simulation) {
        super(delay, bandwidth, source, destinations, simulation);
    }

    @Override
    public long getTransmissionTime(long totalTransferSize, int size) {
        if (this.bandwidth <= 0) {
            throw new RuntimeException("Link speed is equal or lower than 0");
        }
        double diff;
        //Evita que la velocidad de transferencia sea 0 o menor
        do {
            diff = bandwidhtRandom.nextGaussian() * stdBandwidht;
        } while ((-diff) >= this.bandwidth);
        // se supone en bitspersecond
        double speed = (this.bandwidth + diff) / 8;
        double d = this.delay + bandwidhtRandom.nextGaussian() * stdDelay;
        return (long) (size / speed * 1000 + d);
    }

    public void setBandwidth(String bw) {
        this.bandwidth = Integer.parseInt(bw);
    }

    public void setDelay(String d) {
        this.delay = Integer.parseInt(d);
    }

    public void setStdBandwidth(String bw) {
        this.stdBandwidht = Double.parseDouble(bw);
    }

    public void setStdDelay(String d) {
        this.stdDelay = Double.parseDouble(d);
    }

    public void setBandwidthSeed(String bw) {
        this.bandwidhtRandom = new Random(Long.parseLong(bw));
    }

    public void setDelaySeed(String d) {
        this.delayRandom = new Random(Long.parseLong(d));
    }
}
