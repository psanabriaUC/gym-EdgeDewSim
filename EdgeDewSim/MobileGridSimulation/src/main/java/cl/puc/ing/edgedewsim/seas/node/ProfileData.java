package cl.puc.ing.edgedewsim.seas.node;

public class ProfileData implements Comparable<ProfileData> {
    private int toCharge;
    private double slope;

    public ProfileData(int toCharge, double slope) {
        super();
        this.toCharge = toCharge;
        this.slope = slope;
    }

    public int getToCharge() {
        return toCharge;
    }

    public void setToCharge(int toCharge) {
        this.toCharge = toCharge;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    @Override
    public int compareTo(ProfileData arg0) {
        return this.toCharge - arg0.toCharge;
    }

}
