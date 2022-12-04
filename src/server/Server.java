package server;

import gui.MainGUI;
import models.*;
import ocsf.server.*;
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
        MainGUI.serverGui.printToConsole("New client has been connected -> see table");
        MainGUI.serverGui.checkConnectedClients();
    }

    /**
     * Hook method - called when new client was disconnected
     *
     * @param client the connection with the client.
     */
    protected void clientDisconnected(ConnectionToClient client) {
        MainGUI.serverGui.printToConsole("Client has been disconnected -> see table");
        MainGUI.serverGui.checkConnectedClients();
    }

    /**
     * This method handles any messages received from the client.
     *
     * @param msg    The message received from the client.
     * @param client The connection from which the message originated.
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        MainGUI.serverGui.checkConnectedClients();
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
            case "/UpdateSubscriber":
                Subscriber subscriber = (Subscriber) requestBody.get(0);
                if (requestMethod == Method.PUT) {
                    mysqlController.updateSubscriberNumberAndCreditCard(
                            subscriber.getId(), subscriber.getSubscriberNumber(),
                            subscriber.getCreditCardNumber(), response);
                    response.setPath("/UpdateSubscriber");
                }
            case "/AllSubscribers":
                if (requestMethod == Method.GET) {
                    mysqlController.getAllSubscribersFromDB(response);
                    response.setPath("/AllSubscribers");
                }
        }
        return response;
    }


    /**
     * This method overrides the one in the superclass.
     * Called when the server starts listening for connections.
     */
    protected void serverStarted() {
        MainGUI.serverGui.printToConsole("Server listening for connections on port " + getPort());
        mysqlController = new mysqlController(currentConf);
        // todo: delete all below
        //mysqlController.updateSubscriberNumberAndSubscriberCreditCard("111", "0", "0");
        //models.Subscriber subscriber = new models.Subscriber("242", "55","2222","333","5","6",null);
        //s.saveSubscriberToDB(subscriber);
        //    s.updateSubscriberNumber("111", "123456789");
        //    s.updateSubscriberNumber("3", null);
        //    s.updateSubscriberNumber("6", "yuval");
        //    models.Subscriber subscriber = s.getSubscriberDetails("111");
        //    System.out.println(subscriber.getFirstName());
        //    System.out.println(subscriber.getLastName());
        //    System.out.println(subscriber.getId());
        //    System.out.println(subscriber.getPhoneNumber());
        //    System.out.println(subscriber.getEmailAddress());
        //    System.out.println(subscriber.getCreditCardNumber());
        //    System.out.println(subscriber.getSubscriberNumber());
        //    models.Subscriber subscriber2 = s.getSubscriberDetails("123");
        //    System.out.println(subscriber2.getFirstName());
        //    System.out.println(subscriber2.getLastName());
        //    System.out.println(subscriber2.getId());
        //    System.out.println(subscriber2.getPhoneNumber());
        //    System.out.println(subscriber2.getEmailAddress());
        //    System.out.println(subscriber2.getCreditCardNumber());
        //    System.out.println(subscriber2.getSubscriberNumber());
        //    models.Subscriber subscriber3 = s.getSubscriberDetails("notexistid");
        //    System.out.println(subscriber3);
        //    System.out.println(s.isSubscriberExistInDB("notexistid"));
        //    System.out.println(s.isSubscriberExistInDB("444"));
        //    System.out.println(s.isSubscriberExistInDB("0"));
    }

    /**
     * This method overrides the one in the superclass.  Called
     * when the server stops listening for connections.
     */
    protected void serverStopped() {
        MainGUI.serverGui.printToConsole("Server has stopped listening for connections.");
    }


    public boolean closeServer() {
        if (isListening()) {
            try {
                this.close();
            } catch (IOException e) {
                MainGUI.serverGui.printToConsole("Could not close server, because of an error", true);
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
            MainGUI.serverGui.printToConsole("Server stopped listening for clients!", true);
            MainGUI.serverGui.setConnected(false);
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
        String dbScheme = "world";
        String dbUserName = "root";
        String dbPassword = "1234";
        return new ServerConf(ip, port, dbScheme, dbUserName, dbPassword);
    }
}
