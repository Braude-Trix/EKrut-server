package models;

import java.util.List;

public interface IRequest<E> {
    public String getPath();
    public String getMethod();
    public List<E> getBody();
}
