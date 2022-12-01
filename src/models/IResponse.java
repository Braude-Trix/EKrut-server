package models;

import java.util.List;

public interface IResponse {
	public List<Object> getBody();
	public Integer getCode();
	public String getDescription();
}