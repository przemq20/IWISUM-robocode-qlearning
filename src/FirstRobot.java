import robocode.Robot;

public class FirstRobot extends Robot {
    @Override
    public void run() {
        while (true) {
            ahead(100);
            turnRight(45);
            turnGunLeft(10);
            fire(1);
        }
    }
}
