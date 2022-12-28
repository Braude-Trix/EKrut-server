package models;

import java.io.Serializable;

public class Product implements Serializable {
    private String name;
    private String productId;
    private String information;
    private double price;

    public Product(String name, String productId, String information, double price) {
        this.name = name;
        this.productId = productId;
        this.information = information;
        this.price = price;
    }

    public Product(String name, String productId, float price) {
        this.name = name;
        this.productId = productId;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getProductId() {
        return productId;
    }

    public String getInformation() {
        return information;
    }

    public double getPrice() {
        return price;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
