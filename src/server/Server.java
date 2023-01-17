package server;

import gui.ServerGui;
import models.*;
import models.IRequest;
import models.Method;
import models.OrderStatus;
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
import java.util.stream.Collectors;

/**
 * This class overrides some methods in the abstract superclass in order to give
 * more functionality to the server.
 */
public class Server extends AbstractServer {
	public static Server server_instance;
	public mysqlController mysqlController;
	private ServerConf currentConf;

	public static String externalDBSchemeName = "ekrut_external_data_scheme";

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
		Response response = new Response();
		try {
			response = parseClientRequest(request);
		} catch (Exception e) {
			server.mysqlController.editResponse(
					response, ResponseCode.SERVER_ERROR, "Failed to parse client request", null);
			ServerGui.serverGui.printToConsole("Failed to parse client request", true);
		}
		try {
			client.sendToClient(response);
		} catch (IOException e) {
			// handle exception
			throw new RuntimeException(e);
		}
	}

	/**
	 * This method receives a request from the client and performs the desired action accordingly - this request is sent to the db,
	 * and returns a response according to the request
	 * @param request - A request brought from the client
	 * @return - response from mySqlController
	 */
	public Response parseClientRequest(IRequest request) {
		String requestPath = request.getPath();
		Method requestMethod = request.getMethod();
		List<Object> requestBody = request.getBody();
		Response response = new Response();
		switch (requestPath) {
		case "/login/getUser":
			if (requestMethod == Method.GET) {
				mysqlController.getUserFromDB(response, (String) requestBody.get(0), (String) requestBody.get(1));
			}
			break;
		case "/user/myOrders":
			if (requestMethod == Method.GET) {
				mysqlController.getMyOrdersFromDB(response, (Integer) requestBody.get(0));
			}
			break;
		case "/newOrder":
			Order order = (Order) requestBody.get(0);
			if (requestMethod == Method.POST) {
				mysqlController.saveOrderToDB(order, response);
			}
			break;
		case "/getMessages":
			String customerId = (String) requestBody.get(0);
			if (requestMethod == Method.GET) {
				mysqlController.getMyMessages(customerId, response);
			}
			break;

		case "/requestMachineProducts":
			String machineId = requestBody.get(0).toString();
			if (requestMethod == Method.GET) {
				mysqlController.getAllProductsInMachine(machineId, response);
			}
			break;

		case "/requestProducts":
			if (requestMethod == Method.GET) {
				mysqlController.getAllProducts(response);
			}
			break;
			
		case "/machines/requestMachineProductsData":
			String idMachine = requestBody.get(0).toString();
			if(requestMethod == Method.GET) {
				mysqlController.getProductsInMachineData(response, idMachine);
			}
			break;
			
		case "/machines/requestMachineProductsAmount":
			String idOfMachine = requestBody.get(0).toString();
			if(requestMethod == Method.GET) {
				mysqlController.getProductsInMachineAmount(response, idOfMachine);
			}
			break;
		case "/getMachineThreshold":
			Integer getMachineId = (Integer) requestBody.get(0);
			if (requestMethod == Method.GET) {
				mysqlController.getMachineThreshold(response, getMachineId);
			}
			break;
		case "/machines/setMachineThreshold":
			Integer MachineId = (Integer) requestBody.get(0);
			Integer getNewThreshold = (Integer) requestBody.get(1);
			if (requestMethod == Method.PUT) {
				mysqlController.setMachineThreshold(response, MachineId, getNewThreshold);
			}
			break;
		case "/workers/getWorkersByType":
			String wantedType = ((WorkerType) requestBody.get(0)).name();
			if(requestMethod == Method.GET) {
				mysqlController.getWorkersbyType(response, wantedType);
			}
			break;
			
		case "/workers/getRegionalManagerIdByRegion":
			String region = ((Regions) requestBody.get(0)).name();
			if(requestMethod == Method.GET) {
				mysqlController.getRegionalIdByRegion(response, region);
			}
			break;
			
		case "/workers/setOpenTask":
			Integer taskWorkerId = (Integer) requestBody.get(0);
			Integer taskMachineId = (Integer) requestBody.get(1);
			if(requestMethod == Method.POST) {
				mysqlController.setOpenTaskForOpWorker(response, taskWorkerId, taskMachineId);
			}
			break;
		case "/getCustomerIdByOrderId":
			String OrderIdFromCustomerId = requestBody.get(0).toString();
			if (requestMethod == Method.GET) {
				mysqlController.getCustomerIdByOrderIdFromDB(response, OrderIdFromCustomerId);
			}
			break;
		case "/getMachineName":
			Integer machineIdForName = Integer.parseInt((String) requestBody.get(0));
			if (requestMethod == Method.GET) {
				mysqlController.getMachineName(response, machineIdForName);
			}
			break;
		case "/getMonthlyBill":
			Integer userId = (Integer) requestBody.get(0);
			if (requestMethod == Method.GET) {
				mysqlController.getMonthlyBill(response, userId);
			}
			break;
		case "/UpdateMonthlyBill":
			Integer userIdForUpdateMonthlyBill = (Integer) requestBody.get(0);
			Double newMonthlyBill = Double.parseDouble(requestBody.get(1).toString());
			if (requestMethod == Method.PUT) {
				mysqlController.updateMonthlyBill(response, userIdForUpdateMonthlyBill, newMonthlyBill);
			}
			break;
		case "/requestCompletedOrders":
			Integer userIdCompleted = (Integer) requestBody.get(0);
			if (requestMethod == Method.GET) {
				mysqlController.getCompletedOrders(response, userIdCompleted);
			}
			break;
		case "/order/pickupOrder/getPickupCode":
			if (requestMethod == Method.GET) {
				mysqlController.getPickupCodeFromDB(response, (String) requestBody.get(0));
			}
			break;
		case "/order/deliveryOrder/changeStatusAndDateReceived":
			if (requestMethod == Method.PUT) {
				mysqlController.setStatusDeliveryOrderInDB(response, (String) requestBody.get(0),
						(OrderStatus) requestBody.get(1), (String) requestBody.get(2));
			}
			break;
		case "/machines/getMachine":
			if (requestMethod == Method.GET) {
				mysqlController.getMachinesOfRegions(response, (Regions) requestBody.get(0));
			}
			break;
		case "/user/myOrders/deliveryNotCollected":
			if (requestMethod == Method.GET) {
				mysqlController.getAmountNotificationDelivery(response, (Integer) requestBody.get(0));
			}
			break;
		case "/order/checkExistPickupOrderAndChangeStatus":
			if (requestMethod == Method.PUT) {
				mysqlController.putPickupCodeAndChangeStatus(response, (Integer) requestBody.get(0),
						(String) requestBody.get(1), (String) requestBody.get(2));
			}
			break;
		case "/login/getUserForEkConfiguration":
			if (requestMethod == Method.GET) {
				mysqlController.getCustomer(response, (User) requestBody.get(0));
			}
			break;
		case "/login/getUserForOLConfiguration":
			if (requestMethod == Method.GET) {
				mysqlController.getUserForOL(response, (User) requestBody.get(0));
			}
			break;
		case "/login/setLoggedIn":
			if (requestMethod == Method.PUT) {
				mysqlController.changeLoggedInUser(response, (Integer) requestBody.get(0),
						(Boolean) requestBody.get(1));
			}
			break;
		case "/getPendingDeliveriesOrdersByRegion":
			if (requestMethod == Method.GET) {
				mysqlController.getAllPendingDeliveriesOrdersByRegion(response, (requestBody.get(0).toString()));
			}
			break;
		case "/getCollectedDeliveryOrdersWithDate":
			if (requestMethod == Method.GET) {
				mysqlController.getCollectedDeliveryOrdersWithDate(response);
			}
			break;
		case "/getWaitingDeliveryOrdersWithDate":
			if (requestMethod == Method.GET) {
				mysqlController.getWaitingDeliveryOrdersWithDate(response);
			}
			break;
		case "/updateOrderStatus":
			if (requestMethod == Method.PUT) {
				mysqlController.updateOrderStatus(response, requestBody.get(0).toString(),
						(OrderStatus) requestBody.get(1));
			}
			break;
		case "/postMsg":
			if (requestMethod == Method.POST) {
				mysqlController.postMsg(response, requestBody.get(0).toString(),
						Integer.parseInt(requestBody.get(1).toString()),
						Integer.parseInt(requestBody.get(2).toString()));
			}
			break;
		case "/login/getAllSubscriberForFastLogin":
			if (requestMethod == Method.GET) {
				mysqlController.getSubscribersForFastLogin(response);
			}
			break;
		case "/login/getCustomerById":
			if (requestMethod == Method.GET) {
				mysqlController.getUserById(response, (Integer) requestBody.get(0));
			}
			break;
		case "/sales":
			if (requestMethod == Method.GET) {
				mysqlController.getSales(response, (String) requestBody.get(0), (String) requestBody.get(1));
			}
			if (requestMethod == Method.POST) {
				mysqlController.postSales(response, (Sale) requestBody.get(0));
			}
			if (requestMethod == Method.PUT) {
				mysqlController.changeSaleStatus(response, (String) requestBody.get(0), (String) requestBody.get(1));
			}
			break;
		case "/users/allPendingUsers":
			if (requestMethod == Method.GET) {
				Regions ofRegion = (Regions) requestBody.get(0);
				mysqlController.getAllPendingUsers(response, ofRegion);
			}
			break;
		case "/users/upgradeToCostumer":
			if (requestMethod == Method.POST) {
				mysqlController.upgradeUsersToCostumers(response, requestBody);
			}
			break;
		case "/requestUsers": // badihi
			if (requestMethod == Method.GET) {
				mysqlController.getUsersWithTheirStatus(response);
			}
			break;

		case "/requestUserStatus" : // badihi
			if (requestMethod == Method.GET) {
				mysqlController.getUsersStatus(response,(Integer)requestBody.get(0));
			}
			break;

		case "/upgradeClientToSubscriber": //badihi
			if (requestMethod == Method.PUT) {
				mysqlController.UpgradeClientToSubscriber(response, (Integer)requestBody.get(0));
			}
			break;
		case "/upgradeUserToClient": // badihi
			if (requestMethod == Method.POST) {
				mysqlController.UpgradeUserToClient(response, (Integer)requestBody.get(0),(String)requestBody.get(1));
			}
			break;
		case "/checkIfUserPending": // badihi
			if (requestMethod == Method.GET) {
				mysqlController.checkIfUserPending(response, (Integer)requestBody.get(0));
			}
			break;
		case "/operationalWorker/getOpenedTasks":
			if (requestMethod == Method.GET) {
				mysqlController.getOpenInventoryFillTasks(response, (Integer) requestBody.get(0));
			}
			break;
		case "/operationalWorker/fillInventory":
			if (requestMethod == Method.POST) {
				List<ProductInMachine> productsInMachine = requestBody.stream()
						.map(product -> (ProductInMachine) product)
						.collect(Collectors.toList());
				mysqlController.updateInventoryInDB(response, productsInMachine);
			}
			break;
		case "/operationalWorker/setInventoryTask":
			if (requestMethod == Method.PUT) {
				mysqlController.setInventoryTaskStatus(response, requestBody);
			}
			break;
		case "/reports":
			if (requestMethod == Method.GET) {
				mysqlController.getReport(response, (SavedReportRequest) requestBody.get(0));
			}
			break;
		default:
			mysqlController.editResponse(response, ResponseCode.SERVER_ERROR, "Operation doesn't exist", null);
			ServerGui.serverGui.printToConsole("Operation doesn't exist", true);
		}
		return response;
	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		ServerGui.serverGui.printToConsole("Server listening for connections on port " + getPort());
		mysqlController = new mysqlController(currentConf);
	}

	/**
	 * This method overrides the one in the superclass. Called when the server stops
	 * listening for connections.
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

	/**
	 * Default of the data that allows connection to the server
	 * @return - New configuration of the server
	 */
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
