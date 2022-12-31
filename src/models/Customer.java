package models;

import java.io.Serializable;

public class Customer extends User implements Serializable {

	private CustomerType type;
	private String subscriberNumber;
	private Integer monthlyBill;

	public Customer(String firstName, String lastName, Integer id, String email, String phoneNumber, String username,
			String password, boolean isLoggedIn, String creditCardNumber, CustomerType type, String subscriberNumber, Integer monthlyBill) {
		super(firstName, lastName, id, email, phoneNumber, username, password, isLoggedIn, creditCardNumber);
		this.type = type;
		this.subscriberNumber = subscriberNumber;
		this.monthlyBill = monthlyBill;
	}
	
	public Customer(User user, CustomerType type, String subscriberNumber, Integer monthlyBill) {
		super(user.getFirstName(), user.getLastName(), user.getId(), user.getEmail(), user.getPhoneNumber(), user.getUsername(), 
				user.getPassword(), user.isLoggedIn(), user.getCreditCardNumber());
		this.type = type;
		this.subscriberNumber = subscriberNumber;
		this.monthlyBill = monthlyBill;
		
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

	public Integer getMonthlyBill() {
		return monthlyBill;
	}

	public void setMonthlyBill(Integer monthlyBill) {
		this.monthlyBill = monthlyBill;
	}


	
	
}
