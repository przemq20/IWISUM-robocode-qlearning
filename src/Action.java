import java.io.Serializable;

public enum Action implements Serializable {
    NO_ACTION,
    TURN_GUN_LEFT,
    TURN_GUN_RIGHT,
    FIRE,
    FORWARD,
    BACKWARD,
    TURN_RIGHT,
    TURN_LEFT;
}
