import java.io.Serializable;


public class State implements Serializable {
//    private int closest_opponent_gun_heading;
    private int closest_opponent_heading;
//    private int closest_opponent_distance;
//    private int closest_bullet_distance;


    public State(double closest_opponent_gun_heading,double closest_opponent_heading,double closest_opponent_distance){
//        this.closest_opponent_gun_heading = discreticizeGunHeading(closest_opponent_gun_heading);
        this.closest_opponent_heading = discreticizeHeading(closest_opponent_heading);
//        this.closest_opponent_distance = discreticizeOpponentDistance(closest_opponent_distance);

    }


    public static int discreticizeGunHeading(double heading){
        if(heading > 75.0) {
            return 6;
        } else if (heading < -75){
            return -6;
        } else {
            return (int)(heading/15);
        }
    }

    public static int discreticizeHeading(double heading){
        if(heading > 120) {
            return 13;
        } else if (heading < -120){
            return -13;
        } else {
            return (int)(heading/10);
        }
    }

    public static int discreticizeOpponentDistance(double distance) {
        return (int)(distance / 50);
    }


//    public int getClosest_bullet_distance() {
//        return closest_bullet_distance;
//    }
//
//    public int getClosest_opponent_distance() {
//        return closest_opponent_distance;
//    }

//    public int getClosest_opponent_gun_heading() {
//        return closest_opponent_gun_heading;
//    }

    public int getClosest_opponent_heading() {
        return closest_opponent_heading;
    }

    @Override
    public String toString(){
        return "Opponent heading: " + closest_opponent_heading;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof State)) {
            return false;
        }
        State d = (State) o;
        return this.closest_opponent_heading == d.closest_opponent_heading;
    }
}
