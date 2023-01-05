package models;

import java.io.Serializable;

public class InventoryFillTask implements Serializable {
    private String creationDate;
    private Regions region;
    private Integer machineId;
    private String machineName;
    private TaskStatus status;
    private Integer assignedWorker;

    public InventoryFillTask(String creationDate, Integer machineId, TaskStatus status, Integer assignedWorker) {
        this.creationDate = creationDate;
        this.machineId = machineId;
        this.status = status;
        this.assignedWorker = assignedWorker;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public Integer getMachineId() {
        return machineId;
    }

    public void setMachineId(Integer machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getAssignedWorker() {
        return assignedWorker;
    }

    public void setAssignedWorker(Integer assignedWorker) {
        this.assignedWorker = assignedWorker;
    }
}
