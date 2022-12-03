package models;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable, IResponse {
    private static final long serialVersionUID = 1L;
    private ResponseCode code;
    private String path;

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
