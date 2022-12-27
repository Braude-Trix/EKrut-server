package models;

import java.io.Serializable;


public class MyOrders extends Order implements Serializable {

    private String receivedDate;
    
    public MyOrders(String orderId, String execDate, String receivedDate, double price, String machineId, OrderStatus status, PickUpMethod pickUpMethod, Integer customerId) {
    	super (orderId, execDate, price, machineId, status, pickUpMethod, customerId);
        this.receivedDate = receivedDate;
    }
    

	public String getReceivedDate() {
		return receivedDate;
	}


	public void setReceivedDate(String receivedDate) {
		this.receivedDate = receivedDate;
	}

}
