package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class PopulationGenerator {

    public static SimpleGASchedulerProxy gaProxy;
    public static HashMap<Integer, Device> devicesId;
    public static HashMap<Device, Integer> devicesObjects;

    public PopulationGenerator() {

    }

    public abstract ArrayList<Short[]> generatePopulation(ArrayList<Job> jobs, int totalIndividuals, int individualChromosomes, int chromoMaxValue);
}
