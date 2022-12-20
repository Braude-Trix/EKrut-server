package models;

import java.io.Serializable;

public class Machine implements Serializable {
    String id;
    String name;
    String region;
    String threshold;
    public Machine(String id, String name, String region, String threshold) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.threshold = threshold;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }
}
