package models;

import java.io.Serializable;

public class Customer extends User implements Serializable {

	private CustomerType type;
	private String subscriberNumber;
	private Regions region;

	public Customer(String firstName, String lastName, Integer id, String email, String phoneNumber, String username,
			String password, boolean isLoggedIn, String creditCardNumber, CustomerType type, String subscriberNumber, Regions region) {
		super(firstName, lastName, id, email, phoneNumber, username, password, isLoggedIn, creditCardNumber);
		this.type = type;
		this.subscriberNumber = subscriberNumber;
		this.region = region;
	}

	public CustomerType getType() {
		return type;
	}

	public void setType(CustomerType type) {
		this.type = type;
	}

	public String getSubscriberNumber() {
		return subscriberNumber;
	}

	public void setSubscriberNumber(String subscriberNumber) {
		this.subscriberNumber = subscriberNumber;
	}

	public Regions getRegion() {
		return region;
	}

	public void setRegion(Regions region) {
		this.region = region;
	}
	
	
}
