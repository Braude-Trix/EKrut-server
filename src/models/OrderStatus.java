package models;

import java.io.Serializable;

public enum OrderStatus implements Serializable {
	NotCollected, Collected, WaitingApproveDelivery, Done;
	
	@Override
	public String toString() {
		switch(this) {
		case NotCollected: return "Not Collected";
		case Collected: return "Collected";
		case WaitingApproveDelivery: return "Pending";
		case Done: return "Done";
		}
		return null;
	}
	
}
