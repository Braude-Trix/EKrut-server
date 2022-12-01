package models;

import java.io.Serializable;
import java.util.List;

public class Request<E> implements Serializable, IRequest<E> {
    private static final long serialVersionUID = 1L;
    private String path;
    private String method;
    private List<E> body;

    @Override
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    @Override
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    @Override
    public List<E> getBody() {
        return body;
    }
    public void setBody(List<E> body) {
        this.body = body;
    }
}
