package models;

import java.util.List;

public interface IResponse {
	public List<Object> getBody();
	public ResponseCode getCode();
	public String getDescription();
}