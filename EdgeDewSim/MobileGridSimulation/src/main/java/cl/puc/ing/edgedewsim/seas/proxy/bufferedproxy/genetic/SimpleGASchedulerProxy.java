package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.TransferInfo;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.BufferedSchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SimpleGASchedulerProxy extends BufferedSchedulerProxy {

    private static final int EVENT_GENETIC_ALGORITHM_ROUND_FINISHED = 2;

    private static final String H_MAXMIN = "MaxMin";
    private static final String H_MINMIN = "MinMin";
    private static final String H_RANDOM = "Random";
    private final ArrayList<GeneticAssignmentRound> geneticRounds;
    private GAConfiguration genAlgConf;
    private double bufferedDataToBeShedule;
    private long accRoundtime;
    private int currentRound; // this index is to know which of the genetic
    // rounds is next
    private GeneticAssignmentRound currentGAR;

    private String heuristic;

    public SimpleGASchedulerProxy(String name, String bufferValue, Simulation simulation) {
        super(name, bufferValue, simulation);
        accRoundtime = 0;
        bufferedDataToBeShedule = 0;
        geneticRounds = new ArrayList<GeneticAssignmentRound>();
        currentRound = 0;
    }

    @Override
    protected void initializeDeviceAssignments() {
        super.initializeDeviceAssignments();
        HashMap<Integer, Device> devicesId = new HashMap<Integer, Device>();
        HashMap<Device, Integer> devicesObjects = new HashMap<Device, Integer>();
        int devId = 0;
        for (Iterator<Device> iterator = devices.values().iterator(); iterator.hasNext(); ) {
            Device d = iterator.next();
            devicesId.put(devId, d);
            devicesObjects.put(d, devId);
            devId++;
        }

        /**FIXME: the map of devices objects to devices ids can be provided by the SimpleGASchedulerProxy class**/
        FitnessFunction.devicesId = devicesId;
        FitnessFunction.devicesObjects = devicesObjects;
        PopulationGenerator.devicesId = devicesId;
        PopulationGenerator.devicesObjects = devicesObjects;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processEvent(Event event) {
        if (event.getEventType() == SimpleGASchedulerProxy.EVENT_GENETIC_ALGORITHM_ROUND_FINISHED) {
            scheduleJobs((ArrayList<DataAssignment>) event.getData());
            if (currentRound < geneticRounds.size()) {// means that there are
                // more assignments
                // rounds to be
                // performed
                currentGAR = geneticRounds.get(currentRound);
                currentRound++;
                runGeneticAlgorithm(currentGAR);
            }
        } else {
            super.processEvent(event);
        }
    }

    @Override
    protected void queueJob(Job job) {
        heuristic = genAlgConf.getHeuristic();
        if (heuristic.equals(H_MINMIN) || heuristic.equals(H_MAXMIN)) {
            int dataTransferRequirement = job.getInputSize() + job.getOutputSize();
            boolean inserted = false;
            int queueIndex = 0;
            while (!inserted) {
                if (queueIndex < bufferedJobs.size()) {
                    Job currentJob = bufferedJobs.get(queueIndex);
                    int currentJobDataTransferRequirement = currentJob.getInputSize() + currentJob.getOutputSize();
                    if ((currentJobDataTransferRequirement > dataTransferRequirement && heuristic.equals(H_MINMIN))
                            || (currentJobDataTransferRequirement < dataTransferRequirement
                            && heuristic.equals(H_MAXMIN))) {
                        bufferedJobs.add(queueIndex, job);
                        inserted = true;
                    } else {
                        queueIndex++;
                    }
                } else {
                    bufferedJobs.add(job);
                    inserted = true;
                }
            }
            bufferedDataToBeShedule += ((double) dataTransferRequirement / (double) (1024 * 1024)); // expressed in Mb
        } else if (heuristic.equals(H_RANDOM)) {
            bufferedJobs.add(job);
            bufferedDataToBeShedule += (((double) job.getInputSize() + (double) job.getOutputSize())
                    / (double) (1024 * 1024)); // expressed in Mb
        }

    }

    @Override
    protected void assignBufferedJobs() {
        ArrayList<Job> currentBufferedJobs = new ArrayList<Job>();
        currentBufferedJobs.addAll(bufferedJobs);

        GeneticAssignmentRound gar = new GeneticAssignmentRound(currentBufferedJobs, bufferedDataToBeShedule);
        geneticRounds.add(gar);
        bufferedDataToBeShedule = 0;

        if (simulation.getTime() < accRoundtime) { // means that the previous
            // genetic algorithm
            // scheduling is pending
            return;
        } else {
            currentGAR = geneticRounds.get(currentRound);
            currentRound++;
            runGeneticAlgorithm(currentGAR);
        }
    }

    public ArrayList<DataAssignment> getDevicesDataAssignment() {
        return (ArrayList<DataAssignment>) totalDataPerDevice.clone();
    }

    private void runGeneticAlgorithm(GeneticAssignmentRound gar) {

        long startTime = System.currentTimeMillis();
        gar.setAssignmentStartTime(simulation.getTime());
        int populationSize = genAlgConf.getPopulationSize();

        FitnessFunction fitnessEvaluator = genAlgConf.getFitnessEvaluator();
        fitnessEvaluator.setGeneticAssignmentDataRound(gar);
        GAExecInformation gaInfo = genAlgConf.getGAExecInformation();
        gar.setGaInfo(gaInfo);

        ArrayList<Short[]> population = createFirstGeneration(gar.getJobsToSchedule());

        TerminationCondition tm = genAlgConf.getTerminationCondition();
        //LocalSearchStrategy localSearch = genAlgConf.getLocalSearchStrategy();
        //localSearch.setupLocalSearch();
        ParentSelector parentSelectorOperator = genAlgConf.getParentSelectorOperator();
        CrossoverOperator crossoverOperator = genAlgConf.getCrossoverOperator();
        MutationOperator mutationOperator = genAlgConf.getMutationOperator();

        //((LocalSearchStrategy)mutationOperator).setupLocalSearch();

        PopulationReplacementStrategy popReplacementStrategy = genAlgConf.getPopReplacementStrategy();
        popReplacementStrategy.setGAExecInformation(gaInfo);
        EnumeratedIntegerDistribution shouldRecombine = new EnumeratedIntegerDistribution(new int[]{0, 1},
                new double[]{1 - crossoverOperator.getCrossOverRate(), crossoverOperator.getCrossOverRate()});
        EnumeratedIntegerDistribution shouldMutate = new EnumeratedIntegerDistribution(new int[]{0, 1},
                new double[]{1 - mutationOperator.getMutationRate(), mutationOperator.getMutationRate()});

        try {
            int cycles = 0;

            int pairCount = populationSize / 2;
            boolean oddIndividuals = false;
            if ((populationSize % 2) == 1) {
                pairCount++;
                oddIndividuals = true;
            }
            ((IncreasedFitnessTimeCondition) tm).startEvolution();

            while (!tm.satisfiedCondition(population)) {
                cycles++;

                parentSelectorOperator.setPopulation(population);

                ArrayList<Short[]> nextPopGeneration = new ArrayList<Short[]>();

                for (int pair = 0; pair < pairCount; pair++) {
                    ArrayList<Short[]> offspring = new ArrayList<Short[]>();
                    Short[] parent1 = parentSelectorOperator.getParent();
                    offspring.add(parent1);
                    Short[] parent2 = null;
                    if (!oddIndividuals || pair < pairCount - 1) {
                        parent2 = parentSelectorOperator.getParent();
                        offspring.add(parent2);
                    }

                    if (parent2 != null && shouldRecombine.sample() == 1) {
                        gaInfo.addCrossoverOperation(parent1, parent2);
                        offspring = crossoverOperator.recombine(parent1, parent2);

                        ArrayList<Short[]> parentAndChildren = new ArrayList<Short[]>();
                        parentAndChildren.add(parent1);
                        parentAndChildren.add(parent2);
                        parentAndChildren.addAll(offspring);
                        offspring = popReplacementStrategy.filterBestIndividuals(parentAndChildren);
                    }

                    ArrayList<Short[]> mutatedOffspring = new ArrayList<Short[]>();
                    for (Iterator<Short[]> offspringIterator = offspring.iterator(); offspringIterator.hasNext(); ) {
                        Short[] individual = offspringIterator.next();
                        if (shouldMutate.sample() == 1) {
                            gaInfo.addMutationOperation(1);
                            mutatedOffspring.add(mutationOperator.mutate(individual));
                        } else
                            mutatedOffspring.add(individual);

                    }
                    offspring = mutatedOffspring;
                    nextPopGeneration.addAll(offspring);
                }
                //apply local search over the best individual
                /**Short[] bestIndidivual = fitnessEvaluator.getBestIndividual();
                 population.remove(bestIndidivual);
                 Short[] improvedIndividual = localSearch.performLocalSearch(bestIndidivual);
                 population.add(improvedIndividual);*/

                if (nextPopGeneration.size() == populationSize) {
                    population = nextPopGeneration;

                    fitnessEvaluator.refreshCachedAssignments(population);
                } else {
                    throw new Exception("population size has changed");
                }

            }
            gaInfo.setElapsed_time(System.currentTimeMillis() - startTime);
            gaInfo.setEvolution_cycles(cycles);
            Short[] bestIndividual = fitnessEvaluator.getBestIndividual();

            /**
             * Uncomment for testing purposes: load an already obtained solution
             */
            // SolutionFileReader sfr = new
            // SolutionFileReader("sim_input/assignment5000.exp", 5000);
            // Short[] bestIndividual = sfr.loadSolution();

            FitnessValue bestFitness = fitnessEvaluator.evaluate(bestIndividual);

            gaInfo.setBestIndividual(bestIndividual);
            gaInfo.setIndividualBestFitness(bestFitness);


            accRoundtime += simulation.getTime() + gaInfo.getElapsed_time();
            handleRejectedJobs(bestIndividual, gar, accRoundtime);

            ArrayList<DataAssignment> solution = fitnessEvaluator.mapIndividualToSolution(bestIndividual);
            currentGAR.setAssignmentFinishedTime(accRoundtime);
            currentGAR.setAssignment(solution);
            Event roundFinishedEvent = Event.createEvent(Event.NO_SOURCE, accRoundtime,
                    this.getId(),
                    SimpleGASchedulerProxy.EVENT_GENETIC_ALGORITHM_ROUND_FINISHED, solution);
            simulation.addEvent(roundFinishedEvent);
            // Uncomment the next line for test purposes: print best individual,
            // save solution in a text file to recreate these assignments in a new
            // launch
            // System.out.println(gaInfo.printIndividual(gaInfo.getBestIndividual()));
            System.out.println(gaInfo.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRejectedJobs(Short[] bestIndividual, GeneticAssignmentRound gar, long rejectTime) {
        for (int i = 0; i < bestIndividual.length; i++) {
            if (bestIndividual[i] == -1) {
                Job j = gar.getJob(i);
                JobStatsUtils.getInstance(simulation).rejectJob(j, rejectTime);
                Logger.getInstance(simulation).logEntity(this, "Job rejected = " + j.getJobId() + " at " + rejectTime + " simulation time");
            }
        }

    }


    private ArrayList<Short[]> createFirstGeneration(ArrayList<Job> jobsToSchedule) {

        PopulationGenerator popGen = genAlgConf.getPopulationGenerator();
        PopulationGenerator.gaProxy = this;

        ArrayList<Short[]> population = popGen.generatePopulation(jobsToSchedule, genAlgConf.getPopulationSize(),
                genAlgConf.getGenesAmount(), genAlgConf.getGeneMaxValue());

        genAlgConf.getFitnessEvaluator().clearCachedAssignments();

        //DataAssignment.evaluator = new OverflowPenaltyDataEvaluator(
        //		genAlgConf.getFitnessEvaluator().getMaxEnergyAllowedForDataTransfer());
        // evaluate each individual so that the best individual of the
        // population could be extracted when evaluate termination condition
        for (Iterator<Short[]> iterator = population.iterator(); iterator.hasNext(); ) {
            Short[] individual = iterator.next();
            genAlgConf.getFitnessEvaluator().evaluate(individual);
        }

        return population;
    }


    protected void scheduleJobs(ArrayList<DataAssignment> solution) {
        for (DataAssignment deviceAssignment : solution) {
            Device current = deviceAssignment.getDevice();
            TransferInfo prev = null;
            for (Job job : deviceAssignment.getAssignedJobs()) {
                Logger.getInstance(simulation).logEntity(this, "Job assigned to ", job.getJobId(), current);
                // current.incrementIncomingJobs();
                incrementIncomingJobs(current);

                queueJobTransferring(current, job);

				/*
				JobStatsUtils.setJobAssigned(job);
				
				long subMessagesCount = (long) Math.ceil(job.getInputSize() / (double) MESSAGE_SIZE);
				long lastMessageSize = job.getInputSize() - (subMessagesCount - 1) * MESSAGE_SIZE;
				TransferInfo transferInfo = new TransferInfo(current, job, subMessagesCount, 0,
						lastMessageSize);
				transfersPending.put(job.getJobId(), transferInfo);

				if (prev == null) {
					idSend++;
					long messageSize = transferInfo.messagesCount == 1 ? transferInfo.lastMessageSize : MESSAGE_SIZE;
					Logger.logEntity(this, "Initiating Job transferring to ", job.getJobId(), current);

					// temporal cast (int)messageSize, message size must be long

					long time = NetworkModel.getModel().send(this, current, job.getJobId(), (int) messageSize, job);
					long currentSimTime = Simulation.getTime();
					JobStatsUtils.transfer(job, current, time - currentSimTime, currentSimTime);

				} else
					prev.nextJobId = job.getJobId();
				prev = transferInfo;
				*/
            }
        }
    }


    public GAConfiguration getGenAlgConf() {
        return genAlgConf;
    }

    public void setGenAlgConf(GAConfiguration genAlgConf) {
        this.genAlgConf = genAlgConf;
    }

    public String printGeneticRoundsInfo() {
        // GAExecInformation gaInfo = getGenAlgConf().getGAExecInformation();
        String gRInfo = "";// gaInfo.printFixedParameters()+"\n";
        int i = 0;
        for (Iterator<GeneticAssignmentRound> garIt = geneticRounds.iterator(); garIt.hasNext(); ) {
            GeneticAssignmentRound gar = garIt.next();
            gRInfo += "Round:" + i + " StartTime:" + gar.getAssignmentStartTime() + " ScheduleTime:"
                    + gar.getAssignmentFinishedTime() + "\n";
            gRInfo += gar.getGaInfo().toString() + "\n";
            i++;
        }
        return gRInfo;
    }

}
