package models;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Request implements Serializable, IRequest {

    private static final long serialVersionUID = 1L;

    private String path;
    private Method method;
    private List<Object> body;

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public Method getMethod() {
        return method;
    }
    public void setMethod(Method method) {
        this.method = method;
    }
    public List<Object> getBody() {
        return body;
    }
    public void setBody(List<Object> body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(path, request.path) && method == request.method && Objects.equals(body, request.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, method, body);
    }
}
