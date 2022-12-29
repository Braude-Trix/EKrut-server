package models;

import java.io.Serializable;

public class DeliveryOrder extends Order implements Serializable {
	
	private String firstNameCustomer;
	private String lastNameCustomer;
	private String phoneNumber;
	private String fullAddress;
	private String pincode;
	private Regions region;
	private String dateReceived;

	
	public DeliveryOrder(String orderId, String date, double price, String machineId, OrderStatus status, PickUpMethod pickUpMethod, Integer customerId,
			 String firstNameCustomer, String lastNameCustomer, String phoneNumber, String fullAddress,
			Regions region, String dateReceived, String pincode) {
		super(orderId, date, price, machineId, status, pickUpMethod, customerId); 
		this.firstNameCustomer = firstNameCustomer;
		this.lastNameCustomer = lastNameCustomer;
		this.phoneNumber = phoneNumber;
		this.fullAddress = fullAddress;
		this.pincode = pincode;
		this.region = region;
		this.dateReceived = dateReceived;
	}
	
	public DeliveryOrder(Order order, String firstNameCustomer, String lastNameCustomer, String phoneNumber, String fullAddress,
			Regions region, String dateReceived, String pincode) {
		super(order.getOrderId(), order.getDate(), order.getPrice(), order.getMachineId(), order.getStatus(), order.getPickUpMethod(), order.getCustomerId());  
		this.firstNameCustomer = firstNameCustomer;
		this.lastNameCustomer = lastNameCustomer;
		this.phoneNumber = phoneNumber;
		this.fullAddress = fullAddress;
		this.pincode = pincode;
		this.region = region;
		this.dateReceived = dateReceived;
	}

	public String getFirstNameCustomer() {
		return firstNameCustomer;
	}


	public void setFirstNameCustomer(String firstNameCustomer) {
		this.firstNameCustomer = firstNameCustomer;
	}


	public String getLastNameCustomer() {
		return lastNameCustomer;
	}


	public void setLastNameCustomer(String lastNameCustomer) {
		this.lastNameCustomer = lastNameCustomer;
	}


	public String getPhoneNumber() {
		return phoneNumber;
	}


	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}


	public String getFullAddress() {
		return fullAddress;
	}


	public void setFullAddress(String fullAddress) {
		this.fullAddress = fullAddress;
	}


	public String getPincode() {
		return pincode;
	}


	public void setPincode(String pincode) {
		this.pincode = pincode;
	}


	public Regions getRegion() {
		return region;
	}


	public void setRegion(Regions region) {
		this.region = region;
	}


	public String getDateReceived() {
		return dateReceived;
	}


	public void setDateReceived(String dateReceived) {
		this.dateReceived = dateReceived;
	}


	
}
