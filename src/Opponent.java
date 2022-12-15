public class Opponent {
    private double angle;
    private double distance;

    public double getAngle() {
        return angle;
    }

    public double getDistance() {
        return distance;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Opponent(double angle, double distance){
        this.angle=angle;
        this.distance=distance;
    }
}
