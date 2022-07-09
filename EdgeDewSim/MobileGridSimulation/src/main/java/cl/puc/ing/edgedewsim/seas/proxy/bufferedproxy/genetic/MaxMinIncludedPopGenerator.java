package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignmentEvaluatorIF;
import cl.puc.ing.edgedewsim.seas.proxy.DescendingDataAssignmentComparator;
import cl.puc.ing.edgedewsim.seas.proxy.RemainingDataTransferringEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * This population generator creates popSize-1 random individuals plus an extra individual
 * that represent the one of a MaxMin heuristic.
 */
public class MaxMinIncludedPopGenerator extends PopulationGenerator {

    private final RandomPopGenerator randomGen = new RandomPopGenerator();

    /**
     * jobs argument is the list of jobs that this instance of Population Generator uses to build
     * the MaxMin individual.
     * NOTE: The jobs are assumed to be ordered in descending order w.r.t the data input + data output.
     * FIXME: When individuals representation includes information about the order in which jobs needs
     * to be sent to devices, the assumption of the above note can be discarded
     */
    @Override
    public ArrayList<Short[]> generatePopulation(ArrayList<Job> jobs, int totalIndividuals, int individualChromosomes, int chromoMaxValue) {

        ArrayList<Short[]> pop = randomGen.generatePopulation(jobs, totalIndividuals, individualChromosomes, chromoMaxValue);

        Short[] maxminIndividual = getMaxMinIndividual(jobs);

        pop.add(maxminIndividual);

        return pop;
    }


    protected Short[] getMaxMinIndividual(ArrayList<Job> jobs) {

        ArrayList<DataAssignment> totalDataPerDevice = gaProxy.getDevicesDataAssignment();

        DataAssignmentEvaluatorIF previous = DataAssignment.evaluator;
        DataAssignment.evaluator = new RemainingDataTransferringEvaluator();

        Comparator<DataAssignment> comp = new DescendingDataAssignmentComparator(DataAssignment.evaluator);
        Collections.sort(totalDataPerDevice, comp);

        Short[] maxMinIndividual = new Short[jobs.size()];
        Arrays.fill(maxMinIndividual, (short) -1);

        for (int job = 0; job < jobs.size(); job++) {
            Job dataJob = jobs.get(job);
            DataAssignment assignment = null;
            double assignment_remaining_energy = -1;


            for (int index = 0; index < totalDataPerDevice.size(); index++) {
                DataAssignment da = totalDataPerDevice.get(index);
                double job_energy = da.getDevice().getEnergyWasteInTransferringData(dataJob.getInputSize());
                job_energy += da.getDevice().getEnergyWasteInTransferringData(dataJob.getOutputSize());
                double rem_energy = DataAssignment.evaluator.eval(da) - job_energy;
                if (rem_energy > 0 && rem_energy > assignment_remaining_energy) {
                    assignment = da;
                    assignment_remaining_energy = rem_energy;
                }
            }
            if (assignment != null) {
                assignment.scheduleJob(jobs.get(job));
                maxMinIndividual[job] = devicesObjects.get(assignment.getDevice()).shortValue();
                Collections.sort(totalDataPerDevice, comp);
            }
        }

        //set the original data assignment evaluator
        DataAssignment.evaluator = previous;

        return maxMinIndividual;

    }

}
