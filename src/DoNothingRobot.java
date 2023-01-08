import robocode.Robot;

public class DoNothingRobot extends Robot {
    @Override
    public void run() {
        while (true) {
            turnGunRight(1);
        }
    }
}
