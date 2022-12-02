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
 * This class overrides some of the methods in the abstract
 * superclass in order to give more functionality to the server.
 */
public class Server extends AbstractServer {
    // The default port to listen on.
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
     * This method handles any messages received from the client.
     *
     * @param msg    The message received from the client.
     * @param client The connection from which the message originated.
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        IRequest request = (Request) msg;
        Response response = parseClientRequest(request);
        try {
            client.sendToClient(response);
        } catch (IOException e) {
            //handle exception
            throw new RuntimeException(e);
        }
        {

        }
//	  System.out.println(this.getClientConnections()); // all clients
//	  System.out.println("ip: " + client.getInetAddress().getHostAddress()); // this is ip
//	  System.out.println("host: " + client.getInetAddress().getHostAddress());
//	  System.out.println("status: " + client.isAlive()); // true if status is alive
//	  System.out.println(client);
        //System.out.println("Message received: " + msg + " from " + client);
        //Map<String, String> userData = srialize((String) msg);
        //s.saveUserToDB(userData);
        //this.sendToAllClients(msg);
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
                    mysqlController.updateSubscriberNumberAndCreditCard(subscriber.getId(), subscriber.getSubscriberNumber(), subscriber.getCreditCardNumber(), response);
                }
            case "/AllSubscribers":
                if (requestMethod == Method.GET) {
                    mysqlController.getAllSubscribersFromDB(response);
                }
        }
        return response;
    }


    /**
     * This method overrides the one in the superclass.
     * Called when the server starts listening for connections.
     */
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());
        MainGUI.serverGui.printToConsole("Server listening for connections on port " + getPort());
        mysqlController = new mysqlController(currentConf);
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
        System.out.println("Server has stopped listening for connections.");
    }

//  /**
//   * This method is responsible for the creation of the server instance.
//   *
//   * @param args[0] The port number to listen on.
//   * 		Defaults to 5555 if no argument is entered.
//   */

    public void closeServer() {
        try {
            this.close();
        } catch (IOException e) {
            System.out.println("Could not close server, because of:");
            e.printStackTrace();
        }
    }

    public static Server initServer(ServerConf serverConf) {
        Server sv = new Server(serverConf.getPort());
        sv.currentConf = serverConf;

        try {
            sv.listen(); // Start listening for connections
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients!");
        }
        return sv;
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
