import robocode.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

public class QLearner extends AdvancedRobot {
    private static final double startAlpha = 0.01; // learning rate
    private static double gamma = 0.98; // discount factor
    private static final double startEpsilon = 0.1; // exploration rate
    private static final int attempts = 10000;
    private static final int timeToExperiment = attempts * 8 / 10;
    private static String filename = "data.csv";
    private static final String QTABLE = "qTable.jo";

    private static int hits = 0;
    private static Map<Decision, Double> q = new HashMap<>();
    private HashMap<Bullet, Decision> futureBulletsResults = new HashMap<>();
    private HashMap<String, Opponent> opponents = new HashMap<>();
    double prevEnergy = 100;
    private static double alpha = startAlpha;
    private static double epsilon = startEpsilon;
    private int reward = 0;
    private static ArrayList<Integer> rewards = new ArrayList<>();

    public void run() {
        load();
        setAdjustRadarForRobotTurn(true);
        State prevEnvState = getEnvState();
        State envState = getEnvState();
        System.out.println("Alpha: " + alpha);
        System.out.println("Gamma: " + gamma);
        System.out.println("Epsilon: " + epsilon);

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
        double maxPossibleFutureValue = -Double.MAX_VALUE;

        Action bestAction = null;
        Decision bestDecision = null;

        for (Action a : Action.values()) {
            Decision decision = new Decision(envState, a);
            if (q.containsKey(decision)) {
                double possibleFutureValue = q.get(decision);
                if (possibleFutureValue > maxPossibleFutureValue) {
                    maxPossibleFutureValue = possibleFutureValue;
                    bestAction = a;
                    bestDecision = decision;
                }
            }
        }

        if (bestAction == null || random() < epsilon) {
            int length = Action.values().length;
            int index = (int) (floor(random() * length));
            return Action.values()[index];
        }
        return bestAction;
    }

    public void takeAction(Action action, State state) {
        switch (action) {
            case NO_ACTION:
                break;
            case TURN_GUN_LEFT:
                turnGunLeft(10);
                break;
            case TURN_GUN_RIGHT:
                turnGunRight(10);
                break;
            case SMALL_TURN_GUN_LEFT:
                turnGunLeft(2);
                break;
            case SMALL_TURN_GUN_RIGHT:
                turnGunRight(2);
                break;
            case FIRE:
                Bullet bullet = fireBullet(1);
                futureBulletsResults.put(bullet, new Decision(state, action));
                break;
            case FORWARD:
                ahead(40);
                break;
            case BACKWARD:
                back(20);
                break;
            case TURN_RIGHT:
                turnRight(35);
                break;
            case TURN_LEFT:
                turnLeft(35);
                break;
        }
    }


    private State getEnvState() {
        double gunAngle = getGunHeading();
        double opponentAngle = getNearestKnownOpponentHeading();
        double opponentDistance = getNearestKnownOpponentDistance();

        double gunDistanceToOpponent = ((opponentAngle - gunAngle + 180) + 360) % 360 - 180;

        State res = new State(gunDistanceToOpponent, ((opponentAngle - getHeading() + 180) + 360) % 360 - 180, opponentDistance);
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

            if (absoluteGunDistanceToOpponent < minAbsoluteGunDistanceToOpponent) {
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

            if (absoluteGunDistanceToOpponent < minAbsoluteGunDistanceToOpponent) {
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
                if (possibleFutureValue > maxPossibleFutureValue) {
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
        double reward = bulletHitSuccessfully ? 500 : 0;
        this.reward += reward;
        if (q.containsKey(decision)) {
            double oldValue = q.get(decision);
            double newValue = (1 - alpha) * oldValue + alpha * (reward + gamma * getEstimateOfPossibleFutureValue(getEnvState()));
            q.put(decision, newValue);
        } else {
            q.put(decision, reward);
        }
    }

    private void updateKnowledge(Decision decision, State prevEnvState, State causedEnvState, double prevEnergy) {
        double reward = 0;
        if (abs(causedEnvState.getClosest_opponent_gun_heading()) -
                abs(prevEnvState.getClosest_opponent_gun_heading()) <= 0) {
            reward = (float) (20 - abs(causedEnvState.getClosest_opponent_gun_heading())) / 2;
            this.reward += reward;
        }
        if (abs(causedEnvState.getClosest_opponent_gun_heading()) == 0) {
            reward = 20;
            this.reward += reward;
        }

        reward += getEnergy() - prevEnergy;

        if (q.containsKey(decision)) {
            double oldValue = q.get(decision);
            double newValue = (1 - alpha) * oldValue + alpha * (reward + gamma * getEstimateOfPossibleFutureValue(causedEnvState));
            q.put(decision, newValue);
        } else {
            q.put(decision, reward);
        }
    }


    public void onScannedRobot(ScannedRobotEvent e) {
        double gunAngle = getGunHeading();
        double opponentAngle = ((e.getBearing() + getHeading() + 360) % 360);

        double gunDistanceToOpponent = ((opponentAngle - gunAngle + 180) + 360) % 360 - 180;

        Opponent opponent = new Opponent(opponentAngle, e.getDistance());
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
        rewards.add(reward);

        reduceAlpha();
        reduceEpsilon();
        reward = 0;
        hits = 0;
    }


    public void reduceAlpha() {
        if (alpha <= 0) alpha = 0;
        else {
            alpha -= startAlpha / timeToExperiment;
        }
    }

    public void reduceEpsilon() {
        if (epsilon <= 0) epsilon = 0;
        else {
            epsilon -= startEpsilon / timeToExperiment;
        }
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        save();
        saveToCsv();
    }

    private void saveToCsv() {
        try {
            File file = new File(getDataDirectory() + "/" + filename);
            if (file.exists()) file.delete();

            RobocodeFileOutputStream out = new RobocodeFileOutputStream(getDataDirectory() + "/" + filename, true);
            StringBuilder dataToWrite = new StringBuilder();
            int index = 0;
            for (int reward : rewards) {
                String s = index + "," + reward;
                dataToWrite.append(s + "\n");
                index++;
            }
            out.write(dataToWrite.toString().getBytes());
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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