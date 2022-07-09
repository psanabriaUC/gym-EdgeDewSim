package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class GAConfiguration {


    private static final int OPERATOR_CLASSNAME = 1;
    private static final int OPERATOR_PARAMS = 2;

    private static final int GENES_AMOUNT = 1;
    private static final int MAX_GENE_VALUE = 2;
    private final Simulation simulation;
    private BufferedReader conf;
    private String line;
    private int populationSize;
    private int genesAmount;
    private int geneMaxValue;
    private PopulationGenerator popGenerator;
    private TerminationCondition terminationCondition;
    private ParentSelector parentSelector;
    private FitnessFunction fitnessEvaluator;
    private CrossoverOperator crossoverOperator;
    private MutationOperator mutationOperator;
    private PopulationReplacementStrategy popReplacementStrategy;
    private LocalSearchStrategy localSearchStrategy;
    private String heuristic;

    public GAConfiguration(String gaConfigFile, Simulation simulation) {
        this.simulation = simulation;
        try {
            readGAConfigurationFile(gaConfigFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readGAConfigurationFile(String gaConfigFile) throws Exception {

        this.conf = this.getReader(gaConfigFile);
        this.nextLine();
        while (line != null) {
            if (line.startsWith(";fitness_function"))
                loadFitnessFunction();
            if (line.startsWith(";gene_data"))
                loadGeneData();
            else if (line.startsWith(";population_params"))
                loadPopulationParams();
            else if (line.startsWith(";termination_condition"))
                loadTerminationCondition();
            else if (line.startsWith(";parent_selection"))
                loadParentSelection();
            else if (line.startsWith(";crossover_operator"))
                loadCrossoverOperator();
            else if (line.startsWith(";mutation_operator"))
                loadMutationOperator();
            else if (line.startsWith(";population_replacement"))
                loadPopulationReplacementStrategy();
			/*else if(line.startsWith(";local_search"))
				loadLocalSearchStrategy();*/
            else if (line.startsWith(";heuristic"))
                loadHeuristic();
            else throw new IllegalStateException(this.line + " is not a valid parameter");
        }
    }
	
	/*private void loadLocalSearchStrategy() throws Exception {		
		localSearchStrategy = new LocalSearchStrategy(fitnessEvaluator);
		nextLine();
	}*/


    private void loadHeuristic() throws Exception {
        String[] lineParts = this.line.split(" ");
        heuristic = lineParts[1];
        nextLine();
    }

    private void loadFitnessFunction() throws Exception {
        String[] lineParts = this.line.split(" ");
        String className = lineParts[OPERATOR_CLASSNAME];

        if (className.endsWith("DataAwareFitnessFunction"))
            fitnessEvaluator = lineParts.length > 2 ? new DataAwareFitnessFunction(Integer.parseInt(lineParts[OPERATOR_PARAMS]), simulation) : new DataAwareFitnessFunction(simulation);
        else {
            if (className.endsWith("DataPlusCPUAwareFitnessFunction"))
                fitnessEvaluator = lineParts.length > 4 ? new DataPlusCPUAwareFitnessFunction(Integer.parseInt(lineParts[OPERATOR_PARAMS]), Float.parseFloat(lineParts[OPERATOR_PARAMS + 1]), Float.parseFloat(lineParts[OPERATOR_PARAMS + 2]), simulation) : new DataPlusCPUAwareFitnessFunction(Float.parseFloat(lineParts[OPERATOR_PARAMS]), Float.parseFloat(lineParts[OPERATOR_PARAMS + 1]), simulation);
            else {
                if (className.endsWith("DataCPUEnergyAwareFitnessFunction"))
                    fitnessEvaluator = lineParts.length > 4 ? new DataCPUEnergyAwareFitnessFunction(Integer.parseInt(lineParts[OPERATOR_PARAMS]), Float.parseFloat(lineParts[OPERATOR_PARAMS + 1]), Float.parseFloat(lineParts[OPERATOR_PARAMS + 2]), simulation) : new DataPlusCPUAwareFitnessFunction(Float.parseFloat(lineParts[OPERATOR_PARAMS]), Float.parseFloat(lineParts[OPERATOR_PARAMS + 1]), simulation);
            }
        }


        nextLine();
    }

    private void loadPopulationReplacementStrategy() throws Exception {
        String[] lineParts = this.line.split(" ");
        String className = lineParts[OPERATOR_CLASSNAME];

        if (className.endsWith("DeterministicCrowding")) {
            popReplacementStrategy = new DeterministicCrowding(fitnessEvaluator, 2);
        }
        nextLine();
    }

    private void loadMutationOperator() throws Exception {
        String[] lineParts = this.line.split(" ");
        String className = lineParts[OPERATOR_CLASSNAME];
        double mutationRate = Double.parseDouble(lineParts[OPERATOR_PARAMS]);

        if (className.endsWith("IncrementalMutationOperator"))
            mutationOperator = new IncrementalMutationOperator(mutationRate, this.geneMaxValue);
        else {
            if (className.endsWith("RandomMutationOperator")) {
                mutationOperator = new RandomMutationOperator(mutationRate, this.geneMaxValue);
            } else if (className.endsWith("LocalSearchMutationOperator")) {
                mutationOperator = new LocalSearchStrategy(this.fitnessEvaluator, mutationRate, this.geneMaxValue);
            } else if (className.endsWith("ConserveJobsQuantityRandomMutationOperator")) {
                mutationOperator = new ConserveJobsQuantityRandomMutationOperator(mutationRate, this.geneMaxValue);
            }
        }

        nextLine();
    }

    private void loadCrossoverOperator() throws Exception {

        String[] lineParts = this.line.split(" ");
        String className = lineParts[OPERATOR_CLASSNAME];
        String[] params = Arrays.copyOfRange(lineParts, OPERATOR_PARAMS, lineParts.length);

        if (className.endsWith("MPointCrossoverOperator")) {
            int pointsNmb = Integer.parseInt(params[0]);
            double crossoverRate = Double.parseDouble(params[1]);
            crossoverOperator = new MPointCrossoverOperator(this.genesAmount, pointsNmb, crossoverRate);
            crossoverOperator.setName(crossoverOperator.getClass().getSimpleName() + " points=" + pointsNmb + " crossRate=" + crossoverRate);
        } else {
            double crossoverRate = Double.parseDouble(params[0]);
            if (className.endsWith("UniformCrossoverOperator"))
                crossoverOperator = new UniformCrossoverOperator(this.genesAmount, crossoverRate);
            else {
                if (className.endsWith("HalfUniformCrossoverOperator"))
                    crossoverOperator = new HalfUniformCrossoverOperator(this.genesAmount, crossoverRate);
            }
            crossoverOperator.setName(crossoverOperator.getClass().getSimpleName() + " crossRate=" + crossoverRate);
        }

        nextLine();
    }

    private void loadGeneData() throws Exception {

        String[] lineParts = this.line.split(" ");
        this.genesAmount = Integer.parseInt(lineParts[GENES_AMOUNT]);
        this.geneMaxValue = Integer.parseInt(lineParts[MAX_GENE_VALUE]);
        nextLine();

    }

    private void loadParentSelection() throws Exception {

        String[] lineParts = this.line.split(" ");
        String className = lineParts[OPERATOR_CLASSNAME];
        String[] params = Arrays.copyOfRange(lineParts, OPERATOR_PARAMS, lineParts.length);
        if (className.endsWith("TournamentParentSelector"))
            loadTournamentSelector(new TournamentParentSelector(this), params);
        nextLine();

    }

    private void loadTournamentSelector(TournamentParentSelector ts, String[] params) {

        Integer tournamentSize = Integer.parseInt(params[0]);
        Boolean withRepositionFlag = Boolean.parseBoolean(params[1]);

        ts.setTournamentSize(tournamentSize);
        ts.setWithReposition(withRepositionFlag);
        this.parentSelector = ts;
        this.parentSelector.setName(parentSelector.getClass().getSimpleName() + " tourSize=" + tournamentSize + " withReposition=" + withRepositionFlag);
    }

    private void loadTerminationCondition() throws Exception {

        String[] lineParts = this.line.split(" ");
        String className = lineParts[OPERATOR_CLASSNAME];

        if (className.endsWith("MinThresholdFitnessCondition")) {
            Double threshold = Double.valueOf(lineParts[OPERATOR_PARAMS]);
            terminationCondition = new MinThresholdFitnessCondition(threshold, this.fitnessEvaluator);
        } else {
            String[] termConditionParams = Arrays.copyOfRange(lineParts, OPERATOR_PARAMS, lineParts.length);
            if (className.endsWith("IncreasedFitnessTimeCondition")) {
                Long maxTimeSameFitness = Long.parseLong(termConditionParams[0]);
                Long maxEvolutionTotalTime = Long.parseLong(termConditionParams[1]);
                terminationCondition = new IncreasedFitnessTimeCondition(maxTimeSameFitness, maxEvolutionTotalTime, this.fitnessEvaluator);

            }


        }
        nextLine();
    }

    private void loadPopulationParams() throws Exception {

        String[] lineParts = this.line.split(" ");
        this.populationSize = Integer.parseInt(lineParts[1]);

        Class<PopulationGenerator> clazz = (Class<PopulationGenerator>) Class.forName(lineParts[2]);
        this.popGenerator = clazz.newInstance();
        nextLine();
    }

    private BufferedReader getReader(String file) throws FileNotFoundException {
        return new BufferedReader(new FileReader(file));
    }

    private void nextLine() throws IOException {
        this.line = this.conf.readLine();
        if (line == null) return;
        this.line = this.line.trim();
        while (line.startsWith("#") ||
                line.equals("")) {
            this.line = this.conf.readLine();
            if (line == null) return;
            this.line = this.line.trim();
        }
    }

    public LocalSearchStrategy getLocalSearchStrategy() {
        return localSearchStrategy;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getGenesAmount() {
        return genesAmount;
    }

    public int getGeneMaxValue() {
        return geneMaxValue;
    }

    public TerminationCondition getTerminationCondition() {
        return terminationCondition;
    }

    public CrossoverOperator getCrossoverOperator() {
        return crossoverOperator;
    }

    public FitnessFunction getFitnessEvaluator() {
        return fitnessEvaluator;
    }

    public ParentSelector getParentSelectorOperator() {
        return parentSelector;
    }

    public MutationOperator getMutationOperator() {
        return mutationOperator;
    }

    public PopulationReplacementStrategy getPopReplacementStrategy() {
        return popReplacementStrategy;
    }

    public String getHeuristic() {
        return heuristic;
    }

    public GAExecInformation getGAExecInformation() {
        GAExecInformation gaExecInfo = new GAExecInformation(simulation);
        gaExecInfo.setPopulation_size(populationSize);
        gaExecInfo.setPopulation_generator(popGenerator.getClass().getSimpleName());
        gaExecInfo.setJobs_count(genesAmount);
        gaExecInfo.setNodes_count(geneMaxValue); //this parameter could be dynamically obtained from the current number of nodes participate in the grid
        gaExecInfo.setTermination_condition(terminationCondition.getName());
        gaExecInfo.setCrossover_operator(crossoverOperator.getName());
        gaExecInfo.setMutation_operator(mutationOperator.getClass().getSimpleName() + " mutRate=" + mutationOperator.getMutationRate());
        gaExecInfo.setParent_selection(parentSelector.getName());
        gaExecInfo.setPopulation_replacement(popReplacementStrategy.getClass().getSimpleName());
        return gaExecInfo;
    }

    public PopulationGenerator getPopulationGenerator() {
        return popGenerator;
    }


}
