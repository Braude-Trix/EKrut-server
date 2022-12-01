package models;

import java.util.List;

public interface IRequest {
    public String getPath();
    public Method getMethod();
    public List<Object> getBody();
}
