package models;

import java.io.Serializable;

public class DeliveryOrder extends Order implements Serializable {
	
	private String firstNameCustomer;
	private String lastNameCustomer;
	private String phoneNumber;
	private String fullAddress;
	private String pincode;
	private Regions region;
	private String timeForDelivery;
	private String deliveryLoadingTime;
	private Double distance;
	private String regionalManagerId;
	private String dateReceived;

	
	public DeliveryOrder(String orderId, String date, double price, String machineId, String status, PickUpMethod pickUpMethod, Integer customerId,
			String timeForDelivery, String regionalManagerId, String firstNameCustomer, String lastNameCustomer, String phoneNumber, String fullAddress,
			Regions region, String dateReceived, String pincode, String deliveryLoadingTime, Double distance) {
		super(orderId, date, price, machineId, status, pickUpMethod, customerId); 
		this.firstNameCustomer = firstNameCustomer;
		this.lastNameCustomer = lastNameCustomer;
		this.phoneNumber = phoneNumber;
		this.fullAddress = fullAddress;
		this.pincode = pincode;
		this.region = region;
		this.timeForDelivery = timeForDelivery;
		this.regionalManagerId = regionalManagerId;
		this.dateReceived = dateReceived;
		this.distance = distance;
		this.deliveryLoadingTime = deliveryLoadingTime;
	}



	public String getDeliveryLoadingTime() {
		return deliveryLoadingTime;
	}


	public void setDeliveryLoadingTime(String deliveryLoadingTime) {
		this.deliveryLoadingTime = deliveryLoadingTime;
	}


	public double getDistance() {
		return distance;
	}


	public void setDistance(double distance) {
		this.distance = distance;
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


	public String getTimeForDelivery() {
		return timeForDelivery;
	}

	public void setTimeForDelivery(String timeForDelivery) {
		this.timeForDelivery = timeForDelivery;
	}


	public String getRegionalManagerId() {
		return regionalManagerId;
	}

	public void setRegionalManagerId(String regionalManagerId) {
		this.regionalManagerId = regionalManagerId;
	}

	
}
