package serverModels;

import models.StatusInMachine;

import java.io.Serializable;

public class ProductInMachineHistory implements Serializable {
    private String machineId;
    private String productId;
    private StatusInMachine statusInMachine;
    private Integer amount;
    private int month;
    private int day;

    public ProductInMachineHistory(String machineId, String productId, StatusInMachine statusInMachine,
                                   Integer amount, int month, int day) {
        this.machineId = machineId;
        this.productId = productId;
        this.statusInMachine = statusInMachine;
        this.amount = amount;
        this.month = month;
        this.day = day;
    }


    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public StatusInMachine getStatusInMachine() {
        return statusInMachine;
    }

    public void setStatusInMachine(StatusInMachine statusInMachine) {
        this.statusInMachine = statusInMachine;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }
}
