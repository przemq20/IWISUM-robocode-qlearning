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

    public Decision(State state, Action action) {
        this.state = state;
        this.action = action;
    }

    @Override
    public String toString() {
        return "State: " + state.toString() + ", action: " + action.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Decision)) {
            return false;
        }
        Decision d = (Decision) o;
        return d.state.equals(this.state) && this.action.equals(d.action);
    }
}
