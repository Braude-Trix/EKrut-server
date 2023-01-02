package serverModels;

public class ServerConf {
    private String ip;
    private int port;
    public static String dbScheme;
    public static String dbUserName;
    public static String dbPassword;

    public ServerConf(String ip, int port, String dbScheme,
                      String dbUserName, String dbPassword) {
        this.ip = ip;
        this.port = port;
        this.dbScheme = dbScheme;
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbScheme() {
        return dbScheme;
    }

    public void setDbScheme(String dbScheme) {
        this.dbScheme = dbScheme;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
}
