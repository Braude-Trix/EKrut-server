package models;

import java.io.Serializable;
import java.time.LocalDate;

public class PickupOrder extends Order implements Serializable {
	private String pickupCode;
	private String dateReceived;
	
	public PickupOrder(String orderId, String date, double price, String machineId, String status,
			PickUpMethod pickUpMethod, String pickupCode, Integer customerId ,String dateReceived) {
		super(orderId, date, price, machineId, status, pickUpMethod, customerId);  
		this.pickupCode = pickupCode;
		this.dateReceived = dateReceived;
	}

	public String getDateReceived() {
		return dateReceived;
	}

	public void setDateReceived(String dateReceived) {
		this.dateReceived = dateReceived;
	}

	public String getPickupCode() {
		return pickupCode;
	}

	public void setPickupCode(String pickupCode) {
		this.pickupCode = pickupCode;
	}

}
