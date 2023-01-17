import robocode.*;

import java.io.*;
import java.util.*;

import static java.lang.Math.*;

import java.awt.Color;

public class QLearner extends AdvancedRobot {
    private static final double startAlpha = 0.9; // learning rate
    private static final double gamma = 0.85; // discount factor
    private static final double startEpsilon = 0.75; // exploration rate
    private static final int attempts = 1000;
    private static final int timeToExperiment = attempts * 10 / 10;
    private static String filename = "data_" + startAlpha + "_" + gamma + "_" + startEpsilon + ".csv";
//    private static final String QTABLE = "qTable.jo";

    private static int hits = 0;
    private static Map<String, Double> q = new HashMap<>();
    private HashMap<Bullet, Decision> futureBulletsResults = new HashMap<>();
    private HashMap<String, Opponent> opponents = new HashMap<>();
    double prevEnergy = 100;
    private static double alpha = startAlpha;
    private static double epsilon = startEpsilon;
    private int reward = 0;
    private static ArrayList<Integer> rewards = new ArrayList<>();
    private final int degrees = 7;

    public void run() {
        System.out.println(getDataDirectory());
//        load();
        setBodyColor(Color.BLACK);
        setAdjustRadarForRobotTurn(true);
        State prevEnvState = getEnvState();
        State envState = prevEnvState;
        System.out.println("Alpha: " + alpha);
        System.out.println("Gamma: " + gamma);
        System.out.println("Epsilon: " + epsilon);

        for (int i = 0; i < 8; i++) {
            turnRadarRight(45);
        }

        while (true) {
            this.execute();
            Action action = chooseAction(envState);
            takeAction(action, envState);

            envState = getEnvState();
            updateKnowledge(new Decision(prevEnvState, action), prevEnvState, envState, prevEnergy);
            prevEnvState = envState;
            prevEnergy = getEnergy();

//            turnRadarRight(-45); //maximum radar angle per ture
        }
    }

    public Action chooseAction(State envState) {
        double maxPossibleFutureValue = -Double.MAX_VALUE;

        Action bestAction = null;
        Decision bestDecision = null;

        for (Action a : Action.values()) {
            Decision decision = new Decision(envState, a);
            if (q.containsKey(decision.toString())) {
                double possibleFutureValue = q.get(decision.toString());
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
            //case NO_ACTION:
            //    break;
//            case TURN_GUN_LEFT:
//                turnGunLeft(10);
//                break;
//            case TURN_GUN_RIGHT:
//                turnGunRight(10);
//                break;
//            case SMALL_TURN_GUN_LEFT:
//                turnGunLeft(2);
//                break;
//            case SMALL_TURN_GUN_RIGHT:
//                turnGunRight(2);
//                break;
            case FIRE:
                Bullet bullet = fireBullet(1);
                futureBulletsResults.put(bullet, new Decision(state, action));
                break;
//            case FORWARD:
//                ahead(20);
//                break;
//            case BACKWARD:
//                back(20);
//                break;
            case TURN_RIGHT:
                turnRight(degrees);
                turnRadarRight(degrees);
                break;
            case TURN_LEFT:
                turnLeft(degrees);
                turnRadarLeft(degrees);
                break;
        }
    }


    private State getEnvState() {
        double gunAngle = getGunHeading();
        double opponentAngle = getNearestKnownOpponentHeading();
        double opponentDistance = getNearestKnownOpponentDistance();

        double gunDistanceToOpponent = ((opponentAngle - gunAngle + 180) + 360) % 360 - 180;
        return new State(gunDistanceToOpponent, ((opponentAngle - getHeading() + 180) + 360) % 360 - 180, opponentDistance);
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
            if (q.containsKey(decision.toString())) {
                double possibleFutureValue = q.get(decision.toString());
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
        double reward = bulletHitSuccessfully ? 500 : -50;
        this.reward += reward;
        if (q.containsKey(decision.toString())) {
            double oldValue = q.get(decision.toString());
            double newValue = (1 - alpha) * oldValue + alpha * (reward + gamma * getEstimateOfPossibleFutureValue(getEnvState()));
            q.put(decision.toString(), newValue);
        } else {
            q.put(decision.toString(), reward);
        }
    }

    private void updateKnowledge(Decision decision, State prevEnvState, State causedEnvState, double prevEnergy) {
        double reward = 0;

        if (abs(causedEnvState.getClosest_opponent_heading()) < abs(prevEnvState.getClosest_opponent_heading())) {
            reward = 10;
//            this.reward += reward;
        }

        if (abs(causedEnvState.getClosest_opponent_heading()) > abs(prevEnvState.getClosest_opponent_heading())) {
            reward = -50;
//            this.reward += reward;
        }

        if (abs(causedEnvState.getClosest_opponent_heading()) == 0) {
            reward = 20;
//            this.reward += reward;
        }
        //reward += getEnergy() - prevEnergy;
        this.reward += reward;
        if (q.containsKey(decision.toString())) {
            double oldValue = q.get(decision.toString());
            double newValue = (1 - alpha) * oldValue + alpha * (reward + gamma * getEstimateOfPossibleFutureValue(getEnvState()));
            q.put(decision.toString(), newValue);
        } else {
            q.put(decision.toString(), reward);
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
        System.out.println(String.valueOf(reward));
        reduceAlpha();
        reduceEpsilon();
        reward = 0;
        hits = 0;
    }


    public void reduceAlpha() {
        double minAlpha = 0.06;
        if (alpha >= minAlpha)
            alpha = max(alpha - ((startAlpha / (double) timeToExperiment) / 4.0), minAlpha);
    }

    public void reduceEpsilon() {
        double minEpsilon = 0.06;
        if (epsilon >= minEpsilon)
            epsilon = max(epsilon - ((startEpsilon / (double) timeToExperiment) / 3.0), minEpsilon);
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
//        save();
        saveToCsv();
    }

    private void saveToCsv() {
        try {
            File file = new File(getDataDirectory() + "/" + filename);
            if (file.exists()) file.delete();

            RobocodeFileOutputStream out = new RobocodeFileOutputStream(getDataDirectory() + "/" + filename, true);
            out.write("round,reward\n".getBytes());
            StringBuilder dataToWrite = new StringBuilder();
            int index = 0;
            for (int reward : rewards) {
                String s = index + "," + reward;
                dataToWrite.append(s).append("\n");
                index++;
            }
            out.write(dataToWrite.toString().getBytes());
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//
//    private void load() {
//        try {
//            File f = getDataFile(QTABLE);
//            FileInputStream fis = new FileInputStream(f);
//            ObjectInputStream ois = new ObjectInputStream(fis);
//            q = (HashMap) ois.readObject();
//            ois.close();
//            fis.close();
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        } catch (ClassNotFoundException c) {
//            System.out.println("Class not found");
//            c.printStackTrace();
//        }
//    }

//    private void save() {
//        try {
//            File f = getDataFile(QTABLE);
//            RobocodeFileOutputStream fos = new RobocodeFileOutputStream(f);
//            ObjectOutputStream oos = new ObjectOutputStream(fos);
//            oos.writeObject(q);
//            oos.close();
//            fos.close();
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//    }
}