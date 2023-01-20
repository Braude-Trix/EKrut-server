package models;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Response implements Serializable, IResponse {
    private static final long serialVersionUID = 1L;
    private ResponseCode code;
    private String description;
    private List<Object> body;

    @Override
    public ResponseCode getCode() {
        return code;
    }

    public void setCode(ResponseCode code) {
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
        Response response = (Response) o;
        return code == response.code && Objects.equals(description, response.description) && Objects.equals(body, response.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, description, body);
    }
}
