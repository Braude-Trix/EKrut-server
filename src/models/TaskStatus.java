package models;

import java.io.Serializable;

public enum TaskStatus implements Serializable {
    OPENED, IN_PROGRESS, CLOSED;

    public String dbName() {
        switch (this) {
            case OPENED:
                return "OPENED";
            case IN_PROGRESS:
                return "IN_PROGRESS";
            case CLOSED:
                return "CLOSED";
            default:
                return null;
        }
    }

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
