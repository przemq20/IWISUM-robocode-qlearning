import java.io.Serializable;


public class State implements Serializable {
    private int closest_opponent_gun_heading;
    private int closest_opponent_heading;
    private int closest_opponent_distance;
    private int closest_bullet_distance;


    public State(double closest_opponent_gun_heading,double closest_opponent_heading,double closest_opponent_distance){
        this.closest_opponent_gun_heading = discreticizeGunHeading(closest_opponent_gun_heading);
        this.closest_opponent_heading = discreticizeHeading(closest_opponent_heading);
        this.closest_opponent_distance = discreticizeOpponentDistance(closest_opponent_distance);

    }


    public static int discreticizeGunHeading(double heading){
        if(heading > 45.0) {
            return 16;
        } else if (heading < -45){
            return -16;
        } else {
            return (int)(heading/3);
        }
    }

    public static int discreticizeHeading(double heading){
        return (int)(heading/10);
    }

    public static int discreticizeOpponentDistance(double distance) {
        return (int)(distance / 50);
    }


    public int getClosest_bullet_distance() {
        return closest_bullet_distance;
    }

    public int getClosest_opponent_distance() {
        return closest_opponent_distance;
    }

    public int getClosest_opponent_gun_heading() {
        return closest_opponent_gun_heading;
    }

    public int getClosest_opponent_heading() {
        return closest_opponent_heading;
    }
}
