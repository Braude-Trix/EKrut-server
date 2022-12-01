package models;

import java.io.Serializable;
import java.util.List;

public class Response<E> implements Serializable, IResponse<E>{
	private static final long serialVersionUID = 1L;
	private Integer code;
	private String description;
	private List<E> body;

	@Override
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public List<E> getBody() {
		return body;
	}

	public void setBody(List<E> body) {
		this.body = body;
	}
}
