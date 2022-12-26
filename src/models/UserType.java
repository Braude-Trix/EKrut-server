package models;

import java.io.Serializable;

public enum UserType implements Serializable {
	Unregistered, Client, Subscriber, MarketingWorker, ManagerWorker;
}
