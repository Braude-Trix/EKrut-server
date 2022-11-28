import ocsf.server.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 */
public class EchoServer extends AbstractServer {  
  // The default port to listen on.
  final public static int DEFAULT_PORT = 5555;
  mysqlController s;
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port) {
    super(port);
  }
  
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient(Object msg, ConnectionToClient client) {
	  //System.out.println("Message received: " + msg + " from " + client);
	  //Map<String, String> userData = srialize((String) msg);
	  //s.saveUserToDB(userData);
	  //this.sendToAllClients(msg);
  }

    
  /**
   * This method overrides the one in the superclass.
   * Called when the server starts listening for connections.
   */
  protected void serverStarted() {
    System.out.println("Server listening for connections on port " + getPort());
    s = new mysqlController();
    s.connectToDB();
    //Subscriber subscriber = new Subscriber("242", "55","2222","333","5","6",null);
    //s.saveSubscriberToDB(subscriber);
	//    s.updateSubscriberNumber("111", "123456789");
	//    s.updateSubscriberNumber("3", null);
	//    s.updateSubscriberNumber("6", "yuval");
	//    Subscriber subscriber = s.getSubscriberDetails("111");
	//    System.out.println(subscriber.getFirstName());
	//    System.out.println(subscriber.getLastName());
	//    System.out.println(subscriber.getId());
	//    System.out.println(subscriber.getPhoneNumber());
	//    System.out.println(subscriber.getEmailAddress());
	//    System.out.println(subscriber.getCreditCardNumber());
	//    System.out.println(subscriber.getSubscriberNumber());
	//    Subscriber subscriber2 = s.getSubscriberDetails("123");
	//    System.out.println(subscriber2.getFirstName());
	//    System.out.println(subscriber2.getLastName());
	//    System.out.println(subscriber2.getId());
	//    System.out.println(subscriber2.getPhoneNumber());
	//    System.out.println(subscriber2.getEmailAddress());
	//    System.out.println(subscriber2.getCreditCardNumber());
	//    System.out.println(subscriber2.getSubscriberNumber());
	//    Subscriber subscriber3 = s.getSubscriberDetails("notexistid");
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
  
  /**
   * This method is responsible for the creation of the server instance.
   *
   * @param args[0] The port number to listen on.
   * 		Defaults to 5555 if no argument is entered.
   */ 
  public static void main(String[] args) {
    int port = DEFAULT_PORT; // Port to listen on (default 5555)

    try {
      port = Integer.parseInt(args[0]); //Get port from command line
    }
    catch(Throwable t){}
    EchoServer sv = new EchoServer(port);
    
    try {
      sv.listen(); //Start listening for connections
    } 
    catch (Exception ex) {
      System.out.println("ERROR - Could not listen for clients!");
    }
  }
}
