package models;

import java.io.Serializable;
import java.util.List;

public class Request implements Serializable, IRequest {
    private static final long serialVersionUID = 1L;
    private String path;
    private Method method;
    private List<Object> body;

    @Override
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    @Override
    public Method getMethod() {
        return method;
    }
    public void setMethod(Method method) {
        this.method = method;
    }
    @Override
    public List<Object> getBody() {
        return body;
    }
    public void setBody(List<Object> body) {
        this.body = body;
    }
}
