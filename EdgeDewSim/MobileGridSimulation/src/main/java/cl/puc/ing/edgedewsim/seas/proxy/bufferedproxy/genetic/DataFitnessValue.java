package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

public class DataFitnessValue extends FitnessValue {

    private double fitnessValue = Double.NaN;

    private int completelyTransferedJobs = 0;
    private double gridRemainingEnergy = Double.NaN;

    public DataFitnessValue(double fitness, int compJobs, double gridRemEnergy) {
        this.fitnessValue = fitness;
        this.completelyTransferedJobs = compJobs;
        this.gridRemainingEnergy = gridRemEnergy;
    }


    @Override
    public String toString() {
        return "fitness=" + fitnessValue + " transfJobs=" + completelyTransferedJobs + " RemainingEnergy(%)=" + gridRemainingEnergy;
    }

    public int getCompletelyTransferedJobs() {
        return completelyTransferedJobs;
    }

    public void setCompletelyTransferedJobs(int completelyTransferedJobs) {
        this.completelyTransferedJobs = completelyTransferedJobs;
    }

    public double getGridRemainingEnergy() {
        return gridRemainingEnergy;
    }

    public void setGridRemainingEnergy(double gridRemainingEnergy) {
        this.gridRemainingEnergy = gridRemainingEnergy;
    }

    public void setFitnessValue(double fitnessValue) {
        this.fitnessValue = fitnessValue;
    }

}
