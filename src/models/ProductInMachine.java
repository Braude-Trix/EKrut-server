package models;

import java.io.Serializable;

public class ProductInMachine implements Serializable {
    String machineId;
    String productId;
    StatusInMachine statusInMachine;
    Integer amount;

    public ProductInMachine(String machineId, String productId, StatusInMachine statusInMachine, Integer amount) {
        this.machineId = machineId;
        this.productId = productId;
        this.statusInMachine = statusInMachine;
        this.amount = amount;
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
}
