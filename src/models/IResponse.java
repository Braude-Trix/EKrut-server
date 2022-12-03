package models;

import java.util.List;

public interface IResponse {
	public List<Object> getBody();
	public String getPath();
	public ResponseCode getCode();
	public String getDescription();
}