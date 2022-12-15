

import robocode.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

public class QLearner extends AdvancedRobot {
    private static final String QTABLE = "qTable.jo";
    private static int hits = 0;
    private static Map<Decision, Double> q = new HashMap<Decision, Double>();
    private HashMap<Bullet, Decision> futureBulletsResults = new HashMap<Bullet, Decision>();
    private HashMap<String, Opponent> opponents = new HashMap<String, Opponent>();
    double prevEnergy = 100;
    private static double alpha = 0.5;
    private static double beta = 0.3;
    private static double epsilon = 0.05;



    public void run(){
        load();
        setAdjustRadarForRobotTurn(true);
        State prevEnvState = getEnvState();
        State envState = getEnvState();

        while (true) {
            Action action = chooseAction(envState);
            takeAction(action, envState);

            envState = getEnvState();

            updateKnowledge(new Decision(prevEnvState, action), prevEnvState, envState, prevEnergy);
            prevEnvState = envState;
            prevEnergy = getEnergy();

            turnRadarRight(-45); //maximum radar angle per ture
        }
    }

    public Action chooseAction(State envState) {
        Action bestAction = null;
        double maxPossibleFutureValue = -Double.MAX_VALUE;

        Decision bestDecision = null;

        for (Action a : Action.values()) {
            Decision decision = new Decision(envState, a);
            if (q.containsKey(decision)) {
                double possibleFutureValue = q.get(decision);
                if (possibleFutureValue>maxPossibleFutureValue) {
                    maxPossibleFutureValue = possibleFutureValue;
                    bestAction = a;
                    bestDecision = decision;
                }
            }
        }

        if (bestAction == null || random()<epsilon) {
            int length = Action.values().length;
            int index = (int) (floor(random() * length));
            return Action.values()[index];
        }
        return bestAction;
    }

    public void takeAction(Action action, State state) {
        switch (action) {
            case NO_ACTION :
                break;
            case TURN_GUN_LEFT:
                turnGunLeft(5);
                break;
            case TURN_GUN_RIGHT:
                turnGunRight(5);
                break;
            case FIRE:
                Bullet bullet = fireBullet(3);
                futureBulletsResults.put(bullet, new Decision(state, action));
                break;
            case FORWARD:
                ahead(20);
                break;
            case BACKWARD:
                back(20);
                break;
            case TURN_RIGHT:
                turnRight(25);
                break;
            case TURN_LEFT:
                turnLeft(25);
                break;
        }
    }


    private State getEnvState() {
        double gunAngle = getGunHeading();
        double opponentAngle = getNearestKnownOpponentHeading();
        double opponentDistance = getNearestKnownOpponentDistance();

        double gunDistanceToOpponent = ((opponentAngle - gunAngle + 180) + 360) % 360 - 180;

        State res = new State(gunDistanceToOpponent,((opponentAngle - getHeading() + 180) + 360) % 360 - 180,opponentDistance);
        return res;
    }

    private double getNearestKnownOpponentHeading() {
        double absoluteGunDistanceToOpponent;
        double gunAngle = getGunHeading();

        double minAbsoluteGunDistanceToOpponent = 180;
        double minDistanceNeighbourHeading = 0;
        for (Opponent op : opponents.values()) {
            double d = op.getAngle();
            absoluteGunDistanceToOpponent = abs(((d - gunAngle + 180) + 360) % 360 - 180);

            if (absoluteGunDistanceToOpponent<minAbsoluteGunDistanceToOpponent) {
                minAbsoluteGunDistanceToOpponent = absoluteGunDistanceToOpponent;
                minDistanceNeighbourHeading = d;
            }
        }
        return minDistanceNeighbourHeading;
    }

    private double getNearestKnownOpponentDistance() {
        double absoluteGunDistanceToOpponent;
        double gunAngle = getGunHeading();

        double minAbsoluteGunDistanceToOpponent = 180;
        double minDistance = 0;
        for (Opponent op : opponents.values()) {
            double d = op.getAngle();
            absoluteGunDistanceToOpponent = abs(((d - gunAngle + 180) + 360) % 360 - 180);

            if (absoluteGunDistanceToOpponent<minAbsoluteGunDistanceToOpponent) {
                minAbsoluteGunDistanceToOpponent = absoluteGunDistanceToOpponent;
                minDistance = op.getDistance();
            }
        }
        return minDistance;
    }

    private double getEstimateOfPossibleFutureValue(State causedEnvState) {
        double maxPossibleFutureValue = -Double.MAX_VALUE;

        for (Action a : Action.values()) {
            Decision decision = new Decision(causedEnvState, a);
            if (q.containsKey(decision)) {
                double possibleFutureValue = q.get(decision);
                if (possibleFutureValue>maxPossibleFutureValue) {
                    maxPossibleFutureValue = possibleFutureValue;
                }
            }
        }

        if (maxPossibleFutureValue == -Double.MAX_VALUE) {
            return 0;
        }

        return maxPossibleFutureValue;
    }

    private void updateKnowledge(Decision decision, boolean bulletHitSuccessfully) {
        double reward = bulletHitSuccessfully ? 250 : 0;

        if (q.containsKey(decision)) {
            double oldValue = q.get(decision);
            double newValue = (1 - alpha) * oldValue + alpha * (reward + beta * getEstimateOfPossibleFutureValue(getEnvState()));
            q.put(decision, newValue);
        } else {
            q.put(decision, reward);
        }
    }

    private void updateKnowledge(Decision decision, State prevEnvState, State causedEnvState, double prevEnergy) {
        double reward = 0;
        if (abs(causedEnvState.getClosest_opponent_gun_heading()) -
                abs(prevEnvState.getClosest_opponent_gun_heading())<=0)
            reward = (float) (20 - abs(causedEnvState.getClosest_opponent_gun_heading())) / 2;
        if (abs(causedEnvState.getClosest_opponent_gun_heading()) == 0) {
            reward = 20;
        }

        reward += getEnergy() - prevEnergy;

        System.out.println("Reward " + reward);

        if (q.containsKey(decision)) {
            double oldValue = q.get(decision);
            double newValue = (1 - alpha) * oldValue + alpha * (reward + beta * getEstimateOfPossibleFutureValue(causedEnvState));
            q.put(decision, newValue);
        } else {
            q.put(decision, reward);
        }
    }



    public void onScannedRobot(ScannedRobotEvent e) {
        double gunAngle = getGunHeading();
        double opponentAngle = ((e.getBearing() + getHeading() + 360) % 360);

        double gunDistanceToOpponent = ((opponentAngle - gunAngle + 180) + 360) % 360 - 180;

        Opponent opponent = new Opponent( opponentAngle, e.getDistance());
        opponents.put(e.getName(), opponent);
    }

    public void onRobotDeath(RobotDeathEvent e) {
        opponents.remove(e.getName());
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        Decision decision = futureBulletsResults.get(event.getBullet());
        updateKnowledge(decision, true);
        hits++;
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        futureBulletsResults.remove(event.getBullet());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        Decision decision = futureBulletsResults.get(event.getBullet());
        updateKnowledge(decision, false);
    }


    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        hits = 0;
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        save();
    }

    private void load() {
        try {
            File f = getDataFile(QTABLE);
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            q = (HashMap) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }
    }

    private void save() {
        try {
            File f = getDataFile(QTABLE);
            RobocodeFileOutputStream fos = new RobocodeFileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(q);
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}