import java.io.Serializable;

public class Decision implements Serializable {

    private State state;
    private Action action;

    public Action getAction() {
        return action;
    }

    public State getState() {
        return state;
    }

    public Decision(State state, Action action){
        this.state = state;
        this.action = action;
    }

}
