package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * This population generator extends MaxMinIncludedPopGenerator class by adding an additional
 * restriction to the random generated population. This is that every random generated individual
 * must have no more than the amount of assigned jobs than the MaxMin individual
 */
public class MaxMinAndJobsCountAssignedPopGenerator extends MaxMinIncludedPopGenerator {

    @Override
    public ArrayList<Short[]> generatePopulation(ArrayList<Job> jobs, int totalIndividuals, int individualChromosomes, int chromoMaxValue) {

        ArrayList<Short[]> pop = new ArrayList<Short[]>();

        Short[] maxminIndividual = getMaxMinIndividual(jobs);
        totalIndividuals--;
        pop.add(maxminIndividual);

        int assignedJobs = getAssignedJobCount(maxminIndividual);
        pop.addAll(generateRandomPopulation(totalIndividuals, individualChromosomes, chromoMaxValue, assignedJobs));
        return pop;
    }

    private Collection<? extends Short[]> generateRandomPopulation(int populationSize, int chromosomesAmount,
                                                                   int alleleMaxValue, int maxAssignedJobs) {

        ArrayList<Short[]> randPopulation = new ArrayList<Short[]>();
        Random rand = new Random();

        for (int individualIndex = 0; individualIndex < populationSize; individualIndex++) {
            Short[] individual = new Short[chromosomesAmount];
            ArrayList<Integer> notAssignedJobs = new ArrayList<Integer>();
            Arrays.fill(individual, (short) -1);
            int assignedJobs = 0;
            //
            for (int chromosome = 0; chromosome < chromosomesAmount && assignedJobs < maxAssignedJobs; chromosome++) {
                short value = (short) (rand.nextInt(alleleMaxValue + 1) - 1);

                if (value != -1) assignedJobs += 1;
                else notAssignedJobs.add(chromosome);

                individual[chromosome] = value;
            }

            while (assignedJobs < maxAssignedJobs) {
                individual[notAssignedJobs.remove(0)] = (short) (rand.nextInt(alleleMaxValue));
                assignedJobs++;
            }
            randPopulation.add(individual);
        }

        return randPopulation;
    }


    private int getAssignedJobCount(Short[] individual) {
        int assigned = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i] != -1) assigned++;
        }
        return assigned;
    }

}
