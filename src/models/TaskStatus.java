package models;

import java.io.Serializable;

public enum TaskStatus implements Serializable {
    OPENED, IN_PROGRESS, CLOSED;

    @Override
    public String toString() {
        switch (this) {
            case OPENED:
                return "Opened";
            case IN_PROGRESS:
                return "In progress";
            case CLOSED:
                return "Closed";
            default:
                return null;
        }
    }
}
