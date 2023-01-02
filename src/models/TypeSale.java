package models;

import java.io.Serializable;

public enum TypeSale implements Serializable {
	Sale2Plus1, Sale1Plus1, Sale2Plus2, PercentageDiscount, GetSecondOneAtDiscount;
	
	@Override
	public String toString() {
		switch (this) {
		case Sale1Plus1:
			return "1+1 Sale";
		case Sale2Plus1:
			return "2+1 Sale";
		case Sale2Plus2:
			return "2+2 Sale";
		case PercentageDiscount:
			return "Percentage";
		case GetSecondOneAtDiscount:
			return "Percentage on 2nd";
		default:
			return null;
		}
	}

}
