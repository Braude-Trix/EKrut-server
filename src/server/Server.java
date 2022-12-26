package server;

import gui.ServerGui;
import models.IRequest;
import models.Method;
import models.Request;
import models.Response;
import models.ResponseCode;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import serverModels.ServerConf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * This class overrides some methods in the abstract
 * superclass in order to give more functionality to the server.
 */
public class Server extends AbstractServer {
    public static Server server_instance;
    public mysqlController mysqlController;
    private ServerConf currentConf;

    /**
     * Constructs an instance of the server.
     *
     * @param port The port number to connect on.
     */
    public Server(int port) {
        super(port);
    }

    /**
     * Hook method - called when new client is connected
     *
     * @param client the connection connected to the client.
     */
    protected void clientConnected(ConnectionToClient client) {
        ServerGui.serverGui.printToConsole("New client has been connected -> see table");
        ServerGui.serverGui.checkConnectedClients();
    }

    /**
     * Hook method - called when new client was disconnected
     *
     * @param client the connection with the client.
     */
    protected void clientDisconnected(ConnectionToClient client) {
        ServerGui.serverGui.printToConsole("Client has been disconnected -> see table");
        ServerGui.serverGui.checkConnectedClients();
    }

    /**
     * This method handles any messages received from the client.
     *
     * @param msg    The message received from the client.
     * @param client The connection from which the message originated.
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        ServerGui.serverGui.checkConnectedClients();
        IRequest request = (Request) msg;
        Response response = parseClientRequest(request);
        try {
            client.sendToClient(response);
        } catch (IOException e) {
            //handle exception
            throw new RuntimeException(e);
        }
    }

    public Response parseClientRequest(IRequest request) {
        String requestPath = request.getPath();
        Method requestMethod = request.getMethod();
        List<Object> requestBody = request.getBody();
        Response response = new Response();

        switch (requestPath) {
//            case "/UpdateSubscriber":
//                Subscriber subscriber = (Subscriber) requestBody.get(0);
//                if (requestMethod == Method.PUT) {
//                    mysqlController.updateSubscriberNumberAndCreditCard(
//                            subscriber.getId(), subscriber.getSubscriberNumber(),
//                            subscriber.getCreditCardNumber(), response);
//                    response.setPath("/UpdateSubscriber");
//                }
//                break;
//            case "/AllSubscribers":
//                if (requestMethod == Method.GET) {
//                    mysqlController.getAllSubscribersFromDB(response);
//                    response.setPath("/AllSubscribers");
//                }
//                break;
                
            case "/login/getUser":
                if (requestMethod == Method.GET) {
                    mysqlController.getUserFromDB(response, (String)requestBody.get(0), (String)requestBody.get(1));
                    response.setPath("/login/getUser");
                }
                break;
            case "/user/myOrders":
                if (requestMethod == Method.GET) {
                    mysqlController.getMyOrdersFromDB(response, (Integer)requestBody.get(0));
                    response.setPath("/user/myOrders");
                }
                break;
            case "/order/RecivedDateDelivery":
                if (requestMethod == Method.GET) {
                    mysqlController.getRecivedDateDeliveryFromDB(response, (String)requestBody.get(0));
                    response.setPath("/order/RecivedDateDelivery");
                }
                break;
            case "/order/RecivedDatePickup":
                if (requestMethod == Method.GET) {
                    mysqlController.getRecivedDatePickupFromDB(response, (String)requestBody.get(0));
                    response.setPath("/order/RecivedDatePickup");
                }
                break;
            case "/order/pickupOrder/getPickupCode":
                if (requestMethod == Method.GET) {
                    mysqlController.getPickupCodeFromDB(response, (String)requestBody.get(0));
                    response.setPath("/order/pickupOrder/getPickupCode");
                }
                break;
            default:
                mysqlController.editResponse(response, ResponseCode.SERVER_ERROR,
                        "Operation doesn't exist", null);
        }
        return response;
    }
    
    /**
     * This method overrides the one in the superclass.
     * Called when the server starts listening for connections.
     */
    protected void serverStarted() {
        ServerGui.serverGui.printToConsole("Server listening for connections on port " + getPort());
        mysqlController = new mysqlController(currentConf);
    }

    /**
     * This method overrides the one in the superclass.  Called
     * when the server stops listening for connections.
     */
    protected void serverStopped() {
        ServerGui.serverGui.printToConsole("Server has stopped listening for connections.");
    }


    public boolean closeServer() {
        if (isListening()) {
            try {
                this.close();
            } catch (IOException e) {
                ServerGui.serverGui.printToConsole("Could not close server, because of an error", true);
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * This method is responsible for the creation of the server instance.
     *
     * @param serverConf The Server configuration.
     */
    public static void initServer(ServerConf serverConf) {
        if (Server.server_instance == null)
            Server.server_instance = new Server(serverConf.getPort());
        else
            Server.server_instance.setPort(serverConf.getPort());

        Server.server_instance.currentConf = serverConf;

        try {
            Server.server_instance.listen(); // Start listening for connections
        } catch (Exception ex) {
            ServerGui.serverGui.printToConsole("Server stopped listening for clients!", true);
            ServerGui.serverGui.setConnected(false);
        }
    }

    public static ServerConf getDefaultServerConf() {
        // default params
        String ip = "127.0.0.1";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }
        int port = 5555;
        String dbScheme = "ekrut";
        String dbUserName = "root";
        String dbPassword = "1234";
        return new ServerConf(ip, port, dbScheme, dbUserName, dbPassword);
    }
}
