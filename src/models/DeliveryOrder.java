package models;

public class DeliveryOrder {
    String deliveryDate;
    String orderId;
    String deliveryAddress;
    String region;

    public DeliveryOrder(String orderId, String deliveryAddress, String region, String deliveryDate) {
        this.orderId = orderId;
        this.deliveryAddress = deliveryAddress;
        this.region = region;
        this.deliveryDate = deliveryDate;
    }


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }


}
