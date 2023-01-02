package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class Sale implements Serializable {

	private String saleOrderId;
	private String saleName;
	private TypeSale saleType;
	private Regions saleRegion;
	private SaleStatus saleStatus;
	private String saleStartDate;
	private String saleEndDate;
	private TimeSale saleTime;
	private String saleDiscription;
	private String salePercentage;

	

	public Sale(String orderId, String startDate, String endDate, TimeSale time, String name,
			Regions region, SaleStatus status, String salePercentage, String description,TypeSale type) {
		super();
		this.saleOrderId = orderId;
		this.saleStartDate = startDate;
		this.saleEndDate = endDate;
		this.saleName = name;
		this.saleRegion = region;
		this.saleStatus = status;
		this.salePercentage = salePercentage;
		this.saleDiscription = description;
		this.saleTime = time;
		this.saleType=type;
		
	}

	public String getSalePercentage() {
		return salePercentage;
	}

	public void setSalePercentage(String salePercentage) {
		this.salePercentage = salePercentage;
	}

	public String getSaleOrderId() {
		return saleOrderId;
	}

	public void setSaleOrderId(String orderId) {
		this.saleOrderId = orderId;
	}

	public String getSaleName() {
		return saleName;
	}

	public void setSaleName(String name) {
		this.saleName = name;
	}


	public Regions getSaleRegion() {
		return saleRegion;
	}

	public void setSaleRegion(Regions region) {
		this.saleRegion = region;
	}

	public SaleStatus getSaleStatus() {
		return saleStatus;
	}

	public void setSaleStatus(SaleStatus status) {
		this.saleStatus = status;

	}

	public TypeSale getSaleType() {
		return saleType;
	}

	public void setSaleType(TypeSale saleType) {
		this.saleType = saleType;
	}

	public String getSaleDiscription() {
		return saleDiscription;
	}

	public void setSaleDiscription(String saleDiscription) {
		this.saleDiscription = saleDiscription;
	}

	public String getSaleStartDate() {
		return saleStartDate;
	}

	public void setSaleStartDate(String saleStartDate) {
		this.saleStartDate = saleStartDate;
	}

	public String getSaleEndDate() {
		return saleEndDate;
	}

	public void setSaleEndDate(String saleEndDate) {
		this.saleEndDate = saleEndDate;
	}

	public TimeSale getSaleTime() {
		return saleTime;
	}

	public void setSaleTime(TimeSale saleTime) {
		this.saleTime = saleTime;
	}
	

}
