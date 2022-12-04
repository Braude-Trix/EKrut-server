package serverModels;

import javafx.beans.property.SimpleStringProperty;
import ocsf.server.ConnectionToClient;

import java.util.Objects;

public class ClientConnectionData {
    private String ip;
    private String host;
    public String status;

//    public ClientConnectionData() {}

    public ClientConnectionData(ConnectionToClient connectionToClient) {
        ip = Objects.requireNonNull(connectionToClient.getInetAddress()).getHostAddress();
        host = connectionToClient.getInetAddress().getHostName();
        status = connectionToClient.isAlive() ? "Alive" : "Down";
    }

    public ClientConnectionData(String ip, String host, String status) {
        this.ip = ip;
        this.host = host;
        this.status = status;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientConnectionData))
            return false;
        ClientConnectionData other = (ClientConnectionData) obj;
        return this.ip.equals(other.ip) &&
                this.host.equals(other.host) &&
                this.status.equals(other.status);
    }
}
