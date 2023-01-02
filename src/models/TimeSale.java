package models;

import java.io.Serializable;

public enum TimeSale implements Serializable {
	AllDay, Morning, Noon, Afternoon, Evening, Night;

	@Override
	public String toString() {
		switch (this) {
		case AllDay:
			return "All Day";
		case Morning:
			return "Morning: 6:00-12:00";
		case Noon:
			return "Noon: 12:00-16:00";
		case Afternoon:
			return "Afternoon: 16:00-20:00";
		case Evening:
			return "Evening: 20:00-00:00";
		case Night:
			return "Night: 00:00-6:00";
		default:
			return null;
		}
	}
}
