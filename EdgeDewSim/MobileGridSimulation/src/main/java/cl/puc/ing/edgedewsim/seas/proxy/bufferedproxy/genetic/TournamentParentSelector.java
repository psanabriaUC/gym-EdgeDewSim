package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class TournamentParentSelector extends ParentSelector {


    private final FitnessFunction fitnessFunction;
    private boolean withReposition = false;
    private int tournamentSize;
    private Random rand;

    public TournamentParentSelector(GAConfiguration ga) {
        this.selectableIndividuals = new ArrayList<Short[]>();
        this.fitnessFunction = ga.getFitnessEvaluator();
    }

    @Override
    public void setPopulation(ArrayList<Short[]> selectableIndividuals) {
        super.setPopulation(selectableIndividuals);
        rand = new Random();
    }

    @Override
    public Short[] getParent() throws Exception {

        if (selectableIndividuals.size() > 0) {
            ArrayList<Short> tournament;
            tournament = selectableIndividuals.size() >= this.tournamentSize ? buildTournament(this.tournamentSize, false) : buildTournament(selectableIndividuals.size(), true);

            int winnerIndex = getTournamentWinner(tournament);
            return !withReposition ? this.selectableIndividuals.remove(winnerIndex) : this.selectableIndividuals.get(winnerIndex);
        } else
            throw new Exception("Unable to play the tournament since no more selectable individuals remains");

    }

    private int getTournamentWinner(ArrayList<Short> tournament) {

        double bestFitness = Double.NEGATIVE_INFINITY;
        int candidate = 0;
        for (int curCandidate = 0; curCandidate < tournament.size(); curCandidate++) {
            double currentFitness = fitnessFunction.evaluate(selectableIndividuals.get(tournament.get(curCandidate))).getValue();
            if (currentFitness > bestFitness) {
                bestFitness = currentFitness;
                candidate = curCandidate;
            }
        }
        return ((int) (tournament.get(candidate)));
    }

    private ArrayList<Short> buildTournament(int tournamentSize, boolean includeAllSelectableIndividuals) {
        HashMap<Short, Short> tournament = new HashMap<Short, Short>();
        for (int selectionNmb = 0; selectionNmb < tournamentSize; selectionNmb++) {
            if (!includeAllSelectableIndividuals) {
                short individualIndex = (short) rand.nextInt(selectableIndividuals.size());
                while (tournament.get(individualIndex) != null)//the key mapped to a null value is not feasible
                    individualIndex = (short) rand.nextInt(selectableIndividuals.size());
                tournament.put(individualIndex, individualIndex);
            } else
                tournament.put((short) selectionNmb, (short) selectionNmb);
        }
        return new ArrayList<Short>(tournament.values());
    }


    public void setWithReposition(boolean withReposition) {
        this.withReposition = withReposition;
    }

    public void setTournamentSize(int tournamentSize) {
        this.tournamentSize = tournamentSize;
    }
}
