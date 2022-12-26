package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {


    private Integer customerId;
    private PickUpMethod pickUpMethod;
    private String orderId;
    private String date;
    private double price;
    private String machineId;
    private String status;
    private List<ProductInOrder> productsInOrder;

    public Order(String orderId, String date, double price, String machineId, String status, PickUpMethod pickUpMethod, Integer customerId) {
        this.orderId = orderId;
        this.date = date;
        this.price = price;
        this.machineId = machineId;
        this.status = status;
        this.productsInOrder = new ArrayList<>();
        this.pickUpMethod = pickUpMethod;
        this.customerId = customerId;
    }


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ProductInOrder> getProductsInOrder() {
        return productsInOrder;
    }

    public void setProductsInOrder(List<ProductInOrder> productsInOrder) {
        this.productsInOrder = productsInOrder;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public PickUpMethod getPickUpMethod() {
        return pickUpMethod;
    }

    public void setPickUpMethod(PickUpMethod pickUpMethod) {
        this.pickUpMethod = pickUpMethod;
    }


    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

}
