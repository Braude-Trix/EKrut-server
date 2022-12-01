package models;

import java.util.List;

public interface IResponse<E> {
	public List<E> getBody();
	public Integer getCode();
	public String getDescription();
}