package models;

import java.io.Serializable;

public class Worker extends User implements Serializable {

	private WorkerType type;
	private Regions region;

	public Worker(String firstName, String lastName, Integer id, String email, String phoneNumber, String username,
			String password, boolean isLoggedIn, String creditCardNumber, WorkerType type, Regions region) {
		super(firstName, lastName, id, email, phoneNumber, username, password, isLoggedIn, creditCardNumber);
		this.region = region;
		this.type = type;
	}
	
	public Worker(User user, WorkerType type, Regions region) {
		super(user.getFirstName(), user.getLastName(), user.getId(), user.getEmail(), user.getPhoneNumber(), user.getUsername(), 
				user.getPassword(), user.isLoggedIn(), user.getCreditCardNumber());
		this.region = region;
		this.type = type;
	}

	public WorkerType getType() {
		return type;
	}

	public void setType(WorkerType type) {
		this.type = type;
	}

	public Regions getRegion() {
		return region;
	}

	public void setRegion(Regions region) {
		this.region = region;
	}
	

}
