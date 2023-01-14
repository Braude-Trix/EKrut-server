package server;

import gui.ServerGui;
import serverModels.ProductInMachineHistory;
import serverModels.ServerConf;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import models.*;

import static logic.EndOfMonthTask.NEW_REPORTS_CREATED;
import static logic.EndOfMonthTask.REPORTS_ALREADY_EXISTS;

public class mysqlController {
    public static Connection conn;
    public static Connection externalDBSchemeConn = null;
    private final static String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";

    public mysqlController(ServerConf serverConf) {
        String dbScheme = serverConf.getDbScheme();
        String dbUserName = serverConf.getDbUserName();
        String dbPassword = serverConf.getDbPassword();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            ServerGui.serverGui.printToConsole("Driver definition succeed");
        } catch (Exception ex) { // handle the error
            ServerGui.serverGui.printToConsole("Driver definition failed", true);
        }

        try {
            conn = DriverManager.getConnection(
                    String.format("jdbc:mysql://localhost/%s?serverTimezone=IST&useSSL=false", dbScheme),
                    dbUserName,
                    dbPassword);
            ServerGui.serverGui.setConnected(true);
        } catch (SQLException ex) { // handle any errors
            ServerGui.serverGui.printToConsole("SQLException: " + ex.getMessage(), true);
            ServerGui.serverGui.printToConsole("SQLState: " + ex.getSQLState(), true);
            ServerGui.serverGui.printToConsole("VendorError: " + ex.getErrorCode(), true);
            ServerGui.serverGui.setConnected(false);
        }
    }

    private static boolean connectExternalDB(String externalDBSchemeName, String dbUserName, String dbPassword){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            ServerGui.serverGui.printToConsole("Driver definition succeed");
        } catch (Exception ex) { // handle the error
            ServerGui.serverGui.printToConsole("Driver definition failed", true);
        }

        try {
            externalDBSchemeConn = DriverManager.getConnection(
                    String.format("jdbc:mysql://localhost/%s?serverTimezone=IST&useSSL=false", externalDBSchemeName),
                    dbUserName,
                    dbPassword);
            ServerGui.serverGui.printToConsole("SQL connection to external DB succeed");
            return true;
        } catch (SQLException ex) { // handle any errors
            ServerGui.serverGui.printToConsole("SQLException: " + ex.getMessage(), true);
            ServerGui.serverGui.printToConsole("SQLState: " + ex.getSQLState(), true);
            ServerGui.serverGui.printToConsole("VendorError: " + ex.getErrorCode(), true);
        }
        return false;


    }

    public boolean closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                try {
                    externalDBSchemeConn.close();
                    externalDBSchemeConn = null;
                }
                catch(Exception e){

                }
                ServerGui.serverGui.printToConsole("SQL connection was closed");
            } catch (SQLException e) {
                ServerGui.serverGui.printToConsole("Couldn't close SQL connection", true);
                return false;
            }
        }
        return true;
    }

    /**
     * function for 'import simulation', import all users, workers and customers tables from external scheme to our scheme
     * @param dbSchemeName - the name of our scheme
     * @param externalDBSchemeName - the name of the external scheme
     * @return true/false accordingly if everything succeed or not
     */
    public static boolean importUsersDataFromExternalDB(String dbSchemeName, String externalDBSchemeName, String DBPassword){
        String queryUsers = "INSERT INTO " + dbSchemeName + "." + "users" + " SELECT * FROM " + externalDBSchemeName + "." + "users";
        String queryWorkers = "INSERT INTO " + dbSchemeName + "." + "workers" + " SELECT * FROM " + externalDBSchemeName + "." + "workers";
        String queryCustomers = "INSERT INTO " + dbSchemeName + "." + "customers" + " SELECT * FROM " + externalDBSchemeName + "." + "customers";
        String deleteCustomersQuery = "DELETE FROM " + dbSchemeName + ".customers";
        String deleteWorkersQuery = "DELETE FROM " + dbSchemeName + ".workers";
        String deleteUsersQuery = "DELETE FROM " + dbSchemeName + ".users";
        try {
            if(externalDBSchemeConn == null) {
                if (!connectExternalDB(externalDBSchemeName, ServerConf.dbUserName, DBPassword))
                    return false;
            }
            Statement stmtDeleteCustomers = externalDBSchemeConn.createStatement();
            stmtDeleteCustomers.executeUpdate(deleteCustomersQuery);
            Statement stmtDeleteWorkers = externalDBSchemeConn.createStatement();
            stmtDeleteWorkers.executeUpdate(deleteWorkersQuery);
            Statement stmtDeleteUsers = externalDBSchemeConn.createStatement();
            stmtDeleteUsers.executeUpdate(deleteUsersQuery);

            Statement stmtUsers = externalDBSchemeConn.createStatement();
            stmtUsers.executeUpdate(queryUsers);
            Statement stmtWorkers = externalDBSchemeConn.createStatement();
            stmtWorkers.executeUpdate(queryWorkers);
            Statement stmtCustomers = externalDBSchemeConn.createStatement();
            stmtCustomers.executeUpdate(queryCustomers);
        } catch (SQLException e) {
            ServerGui.serverGui.printToConsole(e.getMessage(), true);
            if(externalDBSchemeConn != null) {
                try {
                    externalDBSchemeConn.close();
                    externalDBSchemeConn = null;
                } catch (SQLException ex) {
                    ServerGui.serverGui.printToConsole(ex.getMessage(), true);
                }
            }
            return false;
        }
        return true;
    }


	public void updateSubscriberNumberAndCreditCard(String id, String newSubscriberNumber, String newCreditCardNumber,
			Response response) {
		/*
		 * this method get id of subscriber and update his subscriberNumber to
		 * 'newSubscriberNumber' in DB.
		 */
		PreparedStatement stmt;
		String query = "UPDATE Subscriber SET creditCardNumber= ?, SubscriberNumber= ? WHERE id= ?";
		try {
			stmt = conn.prepareStatement(query);
			stmt.setString(1, newCreditCardNumber);
			stmt.setString(2, newSubscriberNumber);
			stmt.setString(3, id);
			stmt.executeUpdate();
			ServerGui.serverGui.printToConsole("Subscriber update done successfully");
			editResponse(response, ResponseCode.OK, "Successfully updated subscriber credentials", null);
		} catch (SQLException e) {
			editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
			e.printStackTrace();
			ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
		}
	}
    


    public boolean isSubscriberExistInDB(String id) {
        /**
         * this method get a subscriber id and return true/false if he is exists in DB or not
         */
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM Subscriber WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return false;
    }
    
    
    /**
	 * This method creates a query to the db, the query asks for a specific user, identified by his username and password.
	 * @param response - the method edits this response in order to show the user the result of the query
	 * @param username - user's username
	 * @param password - user's password
	 */

	public void getUserFromDB(Response response, String username, String password) {

    	List<Object> userDetails= new ArrayList<>();

        User user;
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM users WHERE username = ? AND userPassword = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            rs = stmt.executeQuery();
            if (rs.next()) {
                user = new User(rs.getString("firstName"), rs.getString("lastName"), rs.getInt("id"),
                		rs.getString("email"), rs.getString("phoneNumber"), rs.getString("username"), rs.getString("userPassword"),
                		rs.getBoolean("isLoggedIn"), rs.getString("creditCardNumber"));
                if (user.isLoggedIn()) {
                    editResponse(response, ResponseCode.INVALID_DATA, "The user is already logged in", null);
                }
                else {
                    userDetails.add(user);
                    editResponse(response, ResponseCode.OK, "Successfully got user details", userDetails);
                	changeLoggedInUser(response, user.getId(), true);
                }
            }
            else {
                editResponse(response, ResponseCode.INVALID_DATA, "The username or password are incorrect", null);
            }
            	
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	/**
	 * This method creates a query to the db, the query requests all the orders related to a specific customer.
	 * @param response - the method edits this response in order to show the user the result of the query
	 * @param customerId - users id.
	 */

	public void getMyOrdersFromDB(Response response, Integer customerId) {
    	List<Object> MyOrders= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT * FROM orders WHERE customerId = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setInt(1, customerId);
            rs = stmt.executeQuery();
            while (rs.next()) {
            	MyOrders order = new MyOrders(rs.getString("orderId"), rs.getString("orderDate"), rs.getString("orderDate"), rs.getInt("price"), rs.getString("machineId"),
            			OrderStatus.valueOf(rs.getString("orderStatus")), PickUpMethod.valueOf(rs.getString("pickUpMethod")), rs.getInt("customerId"));
            	MyOrders.add(order);
            }
            
            if (MyOrders.size() == 0) {
            	MyOrders = null;
            }
            editResponse(response, ResponseCode.OK, "Successfully sent all orders of specific user", MyOrders);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	/**
	 * This method creates a query to the db, the query asks the date of a specific received delivery order. 
	 * @param response - the method edits this response in order to show the user the result of the query
	 * @param orderId - the id of the order wanted to be checked.
	 */

	public void getReceivedDateDeliveryFromDB(Response response, String orderId) {
    	List<Object> RecivedDate= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT dateReceived FROM deliveryOrder WHERE orderId = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, orderId);
            rs = stmt.executeQuery();
            if (rs.next()) {
            	RecivedDate.add(rs.getString("dateReceived"));
            }
            editResponse(response, ResponseCode.OK, "Successfully sent the time of receiving the delivery order", RecivedDate);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	/**
	 * This method creates a query to the db, the query asks the date for a specific received Pickup order.
	 * @param response - the method edits this response in order to show the user the result of the query
	 * @param orderId - the id of the order wanted to be checked. 
	 */

	public void getReceivedDatePickupFromDB(Response response, String orderId) {
    	List<Object> RecivedDate= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT dateReceived FROM pickupOrder WHERE orderId = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, orderId);
            rs = stmt.executeQuery();
            if (rs.next()) {
            	RecivedDate.add(rs.getString("dateReceived"));
            }
            editResponse(response, ResponseCode.OK, "Successfully sent the time of receiving the pickup order", RecivedDate);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	/**
	 * This method creates a query to the db, the query asks for a pickUp code for a specific pickup order.
	 * @param response - the method edits this response in order to show the user the result of the query
	 * @param orderId - the id of the order wanted to be checked. 
	 */

	public void getPickupCodeFromDB(Response response, String orderId) {
    	List<Object> pickupCode= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT pickupCode FROM pickupOrder WHERE orderId = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, orderId);
            rs = stmt.executeQuery();
            if (rs.next()) {
            	pickupCode.add(rs.getString("pickupCode"));
            }
            editResponse(response, ResponseCode.OK, "Successfully sent the pickup code order", pickupCode);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error in data problem: please try again", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	public void setStatusDeliveryOrderInDB(Response response, String orderId, OrderStatus status, String dateReceived) {
	/*
	this method get id of subscriber and update his subscriberNumber to 'newSubscriberNumber' in DB.
	*/
		PreparedStatement stmt;
		String query = "UPDATE orders SET orderStatus= ? WHERE orderId= ?";
		try {
		stmt = conn.prepareStatement(query);
		String tempStatusString = getStringStatus(status);
		stmt.setString(1, tempStatusString);
		stmt.setString(2, orderId);
		stmt.executeUpdate();
		ServerGui.serverGui.printToConsole("Update order status - delivery successfully");
		
		query = "UPDATE deliveryOrder SET dateReceived= ? WHERE orderId= ?";
		stmt = conn.prepareStatement(query);
		stmt.setString(1, dateReceived);
		stmt.setString(2, orderId);
		stmt.executeUpdate();
		ServerGui.serverGui.printToConsole("Update order date received - delivery successfully");

		
		editResponse(response, ResponseCode.OK, "Successfully Update order status and date received - delivery", null);
		} catch (SQLException e) {
			editResponse(response, ResponseCode.DB_ERROR, "Communication problem, try again", null);
			e.printStackTrace();
			ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
		}
	}

    /**
     * edit reponse with given params
     * @param response - Response object
     * @param code - ResponseCode enum
     * @param description - description of the Response
     * @param body - the body for the client
     */
    static void editResponse(Response response, ResponseCode code, String description, List<Object> body) {
        response.setBody(body);
        response.setCode(code);
        response.setDescription(description);
    }
    
    public void getProductsInMachineData(Response response, String machineId) {
    	List<Object> machineIdList = new ArrayList<>();
    	List<Object> productsList = new ArrayList<>();
    	List<Object> newProductsList = new ArrayList<>();
    	
    	getAllProductsInMachine(machineId, response);
    	if(response.getBody() != null)
    		machineIdList = response.getBody();
    	
    	getAllProducts(response);
    	if(response.getBody() != null)
    		productsList = response.getBody();
    	
    	for (Object product : productsList) {
    		if (product instanceof Product) {
    			for (Object productInMachine : machineIdList) {
    				if(((ProductInMachine) productInMachine).getProductId().equals(((Product) product).getProductId())) {
    					newProductsList.add(product);
    				}
    			}
    		}
    	}
    		
    	editResponse(response, ResponseCode.OK, "Successfully import all products", newProductsList);
    }
    
    public void getProductsInMachineAmount(Response response, String machineId) {
    	List<Object> machineIdList = new ArrayList<>();
    	List<Object> productsList = new ArrayList<>();
    	List<Object> proudctsAmount = new ArrayList<>();
    	
    	getAllProductsInMachine(machineId, response);
    	if(response.getBody() != null)
    		machineIdList = response.getBody();
    	
    	getAllProducts(response);
    	if(response.getBody() != null)
    		productsList = response.getBody();
    	
    	for (Object product : productsList) {
    		if (product instanceof Product) {
    			for (Object productInMachine : machineIdList) {
    				if(((ProductInMachine) productInMachine).getProductId().equals(((Product) product).getProductId())) {
    					proudctsAmount.add(productInMachine);
    				}
    			}
    		}
    	}

    	editResponse(response, ResponseCode.OK, "Successfully import all products", proudctsAmount);
    }


    /**
     * function that get all the products from table product in DB. edit the response accordingly.
     * @param response - Response object for the user
     */
    public void getAllProducts(Response response) {
        Product product;
        List<Object> products = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM Products";
        try {
            //productName, productId, information, price
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                String productName = rs.getString("productName");
                InputStream input = null;
                try {

                    // input = new FileInputStream("/styles/" + productName +".png");
                    input = this.getClass().getResourceAsStream("/assets/" + productName +".png");
                } catch (Exception e) {
                    //input = new FileInputStream("/styles/defaultProductImage.png");
                    System.out.println(e);
                    input = this.getClass().getResourceAsStream("/assets/defaultProductImage.png");

                }
                if(input == null)
                {
                    input = this.getClass().getResourceAsStream("/assets/defaultProductImage.png");
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                }
                byte[] imageBytes = output.toByteArray();

                product = new Product(
                        productName, rs.getString("productId"),
                        rs.getString("information"),
                        rs.getDouble("price"),
                        imageBytes);
                products.add(product);


            }
            editResponse(response, ResponseCode.OK, "Successfully import all products", products);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * function that get all the products in specific machine with given machineId. edit the response accordingly.
     * @param machineId - the machine id
     * @param response - Response object for the user
     */
    public void getAllProductsInMachine(String machineId, Response response) {
        ProductInMachine productInMachine;
        List<Object> ProductsInMachine = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM ProductInMachine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(machineId));
            rs = stmt.executeQuery();
            while (rs.next()) {
                productInMachine = new ProductInMachine(
                        rs.getString("machineId"), rs.getString("productId"),
                        StatusInMachine.valueOf(rs.getString("statusInMachine")),
                        rs.getInt("amountInMachine"));
                ProductsInMachine.add(productInMachine);
            }
            editResponse(response, ResponseCode.OK, "Successfully import all products from machine", ProductsInMachine);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that save new order to orders table in DB. edit the response accordingly.
     * @param order - Order object
     * @param response - Response object for the user
     */
    public void saveOrderToDB(Order order, Response response) {
        PreparedStatement stmt;
        String query = "INSERT into orders (orderId, pickUpMethod, orderDate, price, machineId, orderStatus, customerId) VALUES (?,?, ?, ?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, order.getOrderId());
            stmt.setString(2, order.getPickUpMethod().toString());
            stmt.setString(3, order.getDate());
            stmt.setDouble(4, order.getPrice());
            stmt.setString(5, order.getMachineId());
            stmt.setString(6, getStringStatus(order.getStatus()));
            stmt.setInt(7, order.getCustomerId());

            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully save order", null);
            ServerGui.serverGui.printToConsole("Subscriber saved successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * get all the messages with given customerId  from the messages' table in DB. edit the response accordingly.
     * @param customerId - Customer Id
     * @param response - Response object for the user
     */
    public void getMyMessages(String customerId, Response response) {
        List<Integer> messages_ids = new ArrayList<>();
        List<Object> messages = new ArrayList<>();
        messages.add("Msg");
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM messages WHERE to_customerId = ? AND readed = 0";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(customerId));
            rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(rs.getString("message_content"));
                messages_ids.add(rs.getInt("messageId"));
            }
            editResponse(response, ResponseCode.OK, "Successfully query messages", messages);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        setMessageReaded(messages_ids);
    }

    /**
     * function that set message to readed (=1) in DB. edit the response accordingly.
     * @param messages_ids - list of messages that will set to 1
     */
    public void setMessageReaded(List<Integer> messages_ids){
        for(Integer msg_id: messages_ids) {
            PreparedStatement stmt;
            String query = "UPDATE messages SET readed = 1 WHERE messageId = ? ";
            try {
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, msg_id);
                stmt.executeUpdate();
                ServerGui.serverGui.printToConsole("message readed successfully");
            } catch (SQLException e) {
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            }
        }
    }

    /**
     * function that get all the pending deliveries from deliveryorder table, with given region. edit the response accordingly.
     * @param response - Response object for the user
     * @param region - Region object casting to String
     */
    public void getAllPendingDeliveriesOrdersByRegion(Response response, String region) {
        DeliveryOrder deliveryOrder;
        List<Object> deliveriesOrders = new ArrayList<>();
        List<String> resList;
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM deliveryorder WHERE region = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, region);
            rs = stmt.executeQuery();
            while (rs.next()) {
                deliveryOrder = new DeliveryOrder(rs.getString("orderId"),null, 0.0, null, null, null,null, null, null, null, rs.getString("fullAddress"), Regions.valueOf(region),rs.getString("dateReceived"), null);
                deliveriesOrders.add(deliveryOrder);
            }
            if(deliveriesOrders.size() == 0)
                deliveriesOrders = null;
            editResponse(response, ResponseCode.OK, "Successfully get all deliveries orders", deliveriesOrders);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    /**
     * function that get the delivery orders and their date with orderStatus WaitingApproveDelivery from DB. edit the response accordingly.
     * @param response - Response object for the user
     */
    public void getWaitingDeliveryOrdersWithDate(Response response) {
        List<Object> resList = new ArrayList<>();
        Map<String, String> resMap = new HashMap<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM orders WHERE orderStatus = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, "WaitingApproveDelivery");
            rs = stmt.executeQuery();
            while (rs.next()) {
                resMap.put(rs.getString("orderId"), rs.getString("orderDate"));
            }
            resList.add(resMap);
            editResponse(response, ResponseCode.OK, "Successfully get all waiting approve deliveries orders", resList);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that get the delivery Orders with date with orderStatus Collected. edit the response accordingly.
     * @param response - Response object for the user
     */
    public void getCollectedDeliveryOrdersWithDate(Response response) {
        List<Object> resList = new ArrayList<>();
        Map<String, String> resMap = new HashMap<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM orders WHERE orderStatus = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, "Collected");
            rs = stmt.executeQuery();
            while (rs.next()) {
                resMap.put(rs.getString("orderId"), rs.getString("orderDate"));
            }
            resList.add(resMap);
            editResponse(response, ResponseCode.OK, "Successfully get all waiting approve deliveries orders", resList);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    /**
     * function that save Delivery order to DB with given DeliveryOrder object. edit the response accordingly.
     * @param response - Response object for the user
     * @param deliveryOrder - DeliveryOrder object
     */
    public void saveDeliveryOrder(Response response, DeliveryOrder deliveryOrder) {
        PreparedStatement stmt;
        String query = "INSERT into deliveryorder (firstNameContact, lastNameContact, phoneNumberContact, fullAddress, pincode, region, orderId) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, deliveryOrder.getFirstNameCustomer());
            stmt.setString(2, deliveryOrder.getLastNameCustomer());
            stmt.setString(3, deliveryOrder.getPhoneNumber());
            stmt.setString(4, deliveryOrder.getFullAddress());
            stmt.setString(5, deliveryOrder.getPincode());
            stmt.setString(6, deliveryOrder.getRegion().toString());
            stmt.setString(7, deliveryOrder.getOrderId());
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully save delivery order", null);
            ServerGui.serverGui.printToConsole("Successfully save delivery order");
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that save in DB latePickUp order with given PickUpOrder object. edit the response accordingly.
     * @param response - Response object for the user
     * @param pickupOrder - PickupOrder object
     */
    public void saveLatePickUpOrder(Response response, PickupOrder pickupOrder) {
        PreparedStatement stmt;
        String query = "INSERT into pickuporder (pickupCode, orderId) VALUES (?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, pickupOrder.getPickupCode());
            stmt.setString(2, pickupOrder.getOrderId());
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully save delivery order", null);
            ServerGui.serverGui.printToConsole("Successfully save delivery order");
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that save products in orders in product_in_order table in DB with the given productList. edit the response accordingly.
     * @param response - Response object for the user
     * @param orderId - Order id
     * @param productsList - list of products in the order
     */
    public void saveProductsInOrder(Response response, String orderId, List<Object> productsList) {
        PreparedStatement stmt;
        for (Object product : productsList) {
            String query = "INSERT into product_in_order VALUES (?, ?, ?)";
            try {
                stmt = conn.prepareStatement(query);
                stmt.setString(1, orderId);
                stmt.setString(2, ((ProductInOrder) product).getProduct().getProductId());
                stmt.setInt(3, ((ProductInOrder) product).getAmount());
                stmt.executeUpdate();
                editResponse(response, ResponseCode.OK, "Successfully save products in order", null);
                ServerGui.serverGui.printToConsole("Successfully save products in order");
            } catch (SQLException e) {
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            }
        }
    }


    /**
     * function that write new message to the messages' table in DB. edit the response accordingly.
     * @param response - Response object for the user
     * @param msg - String message
     * @param fromId - sender message id
     * @param toId - receiver message id
     */
    public void postMsg(Response response, String msg, Integer fromId, Integer toId) {
        PreparedStatement stmt;
        String query = "INSERT into messages (to_customerId, from_Id, message_content, readed) VALUES (?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, toId);
            stmt.setInt(2, fromId);
            stmt.setString(3, msg);
            stmt.setInt(4, 0);
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully save message", null);
            ServerGui.serverGui.printToConsole("Successfully save message");
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    //postMsg(response, requestBody.get(0).toString(), requestBody.get(1).toString());

    /**
     * function that get the machine threshold with given machineId from DB. edit the response accordingly.
     * @param response - Response object for the user
     * @param machineId - machine Id
     */
    public void getMachineThreshold(Response response, Integer machineId) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Object> resList = new ArrayList<>();
        String query = "SELECT * FROM machine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, machineId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resList.add(rs.getInt("threshold"));
                editResponse(response, ResponseCode.OK, "Successfully get machine threshold", resList);

            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    
    public void getRegionalIdByRegion(Response response, String region) {
    	List<Object> managerIdByRegion = new ArrayList<>();
    	ResultSet rs;
    	PreparedStatement stmt;
    	String query = "SELECT * FROM workers WHERE workerType = ? AND region = ?";
    	try {
    		stmt = conn.prepareStatement(query);
    		stmt.setString(1, WorkerType.RegionalManager.name());
    		stmt.setString(2, region);
    		rs = stmt.executeQuery();
    		if (rs.next()) {
    			managerIdByRegion.add(rs.getInt("id"));
    			editResponse(response, ResponseCode.OK, "Successfully get manager ID", managerIdByRegion);
    		}
    		rs.close();
    	} catch (SQLException e) {
    		e.printStackTrace();
    		ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
    	}
    }
    
    
    /**
     * function that gets list of workers from DB accurding to a WorkerType.  edit the response accordingly.
     * @param response - Response object for the user
     * @param wantedType - the name of WorkerType to look for
     */
    public void getWorkersbyType(Response response, String wantedType) {
    	List<Object> workersByType = new ArrayList<>();
    	User currentUser;
    	Integer currentId;
     	Worker currentWorker;
        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT * FROM workers WHERE workerType = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, wantedType);
            rs = stmt.executeQuery();
            while (rs.next()) {
            	Regions tempRegion = (rs.getString("region") == null)? null: Regions.valueOf(rs.getString("region"));
            	currentId = rs.getInt("id");
            	currentUser = getUserDatabyId(response, currentId);
            	currentWorker = new Worker(currentUser, WorkerType.valueOf(wantedType), tempRegion);
            	workersByType.add(currentWorker);
            }
            rs.close();
    		ServerGui.serverGui.printToConsole("Successfully got user details");
            editResponse(response, ResponseCode.OK, "Successfully got user details", workersByType);
        }catch(SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    
    private User getUserDatabyId(Response response, Integer id) {
        User user = null;
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM users WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                user = new User(rs.getString("firstName"), rs.getString("lastName"), rs.getInt("id"),
                		rs.getString("email"), rs.getString("phoneNumber"), rs.getString("username"), rs.getString("userPassword"),
                		rs.getBoolean("isLoggedIn"), rs.getString("creditCardNumber"));
            }
            rs.close();
            return user;
        }catch (SQLException e) {
                editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return null;
    }
    
    /**
     * function that sets a new task for operationalWorker to a machine in DB. edit the response accordingly.
     * @param response - Response object for the user
     * @param workerId - Integer of workerId
     * @param machineId - Integer of machineId
     */
    
    public void setOpenTaskForOpWorker(Response response, Integer workerId, Integer machineId) {
    	PreparedStatement stmt;
    	ResultSet rs;
    	String query = "SELECT * FROM inventory_fill_tasks WHERE machineId = ? AND assignedWorker = ? AND status != ?";
    	try {
    		stmt = conn.prepareStatement(query);
    		stmt.setInt(1, machineId);
    		stmt.setInt(2, workerId);
    		stmt.setString(3, models.TaskStatus.CLOSED.dbName());
    		rs = stmt.executeQuery();
    		if(rs.next()) { // only if we have in DB a task in status OPEN or IN_PROGRESS
    			ServerGui.serverGui.printToConsole("task for worker and machine already open/in progress");
        		editResponse(response, ResponseCode.DB_ERROR, "task for worker and machine already open/in progress", null);
        		return; 
    		}
    	}catch(SQLException e) {
    		editResponse(response, ResponseCode.DB_ERROR, "Error reading from DB", null);
    		e.printStackTrace();
    		ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
    		return;
    	}
    	String date = LocalDate.now().format(DateTimeFormatter.ofPattern(models.StyleConstants.DATE_FORMAT));
    	query = "SELECT * FROM inventory_fill_tasks WHERE creationDate = ? AND machineId = ? AND assignedWorker = ? AND status = ?";
    	try {
    		stmt = conn.prepareStatement(query);
    		stmt.setString(1, date);
    		stmt.setInt(2, machineId);
    		stmt.setInt(3, workerId);
    		stmt.setString(4, models.TaskStatus.CLOSED.dbName());
    		rs = stmt.executeQuery();
    		if(rs.next()) { // only if we have in DB a task in status CLOSED on the same day
    			InventoryFillTask myTask = new InventoryFillTask(date, machineId, models.TaskStatus.OPENED, workerId);
    			List<Object> taskList = new ArrayList<>();
    			taskList.add(myTask);
    			setInventoryTaskStatus(response, taskList);
    			return;
    		}
    	}catch(SQLException e) {
    		editResponse(response, ResponseCode.DB_ERROR, "Error setting closed task to open", null);
    		e.printStackTrace();
    		ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
    	}
    	
    	// we have a task for this machine & worker that is CLOSE (not on this day) or not existing at all
    	query = "INSERT INTO inventory_fill_tasks (creationDate, machineId, status, assignedWorker)"
    			+ " VALUES (?, ?, ?, ?)";
    	try {
    		stmt = conn.prepareStatement(query);
    		stmt.setString(1, date);
    		stmt.setInt(2, machineId);
    		stmt.setString(3, models.TaskStatus.OPENED.dbName());
    		stmt.setInt(4, workerId);
    		stmt.executeUpdate();
    		ServerGui.serverGui.printToConsole("setting open task successfully");
    		editResponse(response, ResponseCode.OK, "setting open task successfully", null);
    	}catch(SQLException e) {
    		editResponse(response, ResponseCode.DB_ERROR, "Error setting open task", null);
    		e.printStackTrace();
    		ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
    	}
    }
    
    /**
     * function that update threshold of machine in DB.  edit the response accordingly.
     * @param response - Response object for the user
     * @param machineId - machine ID
     * @param newthreshold - the threshold to change
     */
    public void setMachineThreshold(Response response, Integer machineId, Integer newthreshold) {
    	PreparedStatement stmt;
    	String query = "UPDATE machine SET threshold= ? WHERE machineId= ?";
    	try {
    		stmt = conn.prepareStatement(query);
    		stmt.setInt(1, newthreshold);
    		stmt.setInt(2, machineId);
    		stmt.executeUpdate();
    		ServerGui.serverGui.printToConsole("update threshold successfully");
    		editResponse(response, ResponseCode.OK, "Successfully updated threshold", null);
    	}catch(SQLException e) {
    		editResponse(response, ResponseCode.DB_ERROR, "Error updating threshold", null);
    		e.printStackTrace();
    		ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
    	}
    }
    

    /**
     * function that update in inventory in DB after a new order.  edit the response accordingly.
     * @param response - Response object for the user
     * @param updatedInventory - list of ProductInMachine objects
     */
    public void updateInventoryInDB(Response response, List<Object> updatedInventory) {
        boolean isUpdateFailed = false;
        Response responseForHistory = new Response();
        ProductInMachine productInMachineCasted;
        for(Object productInMachine : updatedInventory) {
            productInMachineCasted = (ProductInMachine) productInMachine;
            PreparedStatement stmt;
            String query = "UPDATE ProductInMachine SET amountInMachine= ?, statusInMachine= ? WHERE productId = ? AND machineId = ?";
            try {
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, productInMachineCasted.getAmount());
                stmt.setString(2, productInMachineCasted.getStatusInMachine().toString());
                stmt.setString(3, productInMachineCasted.getProductId());
                stmt.setString(4, productInMachineCasted.getMachineId());
                stmt.executeUpdate();

                updateInventoryHistoryInDB(responseForHistory, productInMachineCasted);
                if (responseForHistory.getCode() != ResponseCode.OK) { // an error occurred in updating history
                    isUpdateFailed = true;
                }
            } catch (SQLException e) {
                isUpdateFailed = true;
                editResponse(response, ResponseCode.DB_ERROR,
                        "There was an error while updating products in machine", null);
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            }
        }
        String msg;
        if (isUpdateFailed) {
            msg = "Inventory in machine has been done, at least one product was failed to update";
            ServerGui.serverGui.printToConsole(msg, true);
            editResponse(response, ResponseCode.DB_ERROR, msg, null);
        } else {
            msg = "Inventory in machine has been updated successfully";
            ServerGui.serverGui.printToConsole(msg);
            editResponse(response, ResponseCode.OK, msg, null);
        }
    }

    /**
     * function that update in inventory history in DB after a new order. edit the response accordingly.
     * @param response - Response object for the user
     * @param productInMachine - productInMachine object
     */
    private void updateInventoryHistoryInDB(Response response, ProductInMachine productInMachine) {
        boolean isExists = checkIfInventoryHistoryHasCurrentDay(response, productInMachine);
        if (!isExists && response.getCode() == ResponseCode.DB_ERROR) { // an error occurred
            return;
        }
        if (isExists) {
            PreparedStatement stmt;
            String query = "UPDATE product_in_machine_history " +
                    "SET amountInMachine = ?, statusInMachine = ?, updated_month = ?, updated_day = ? " +
                    "WHERE productId = ? AND machineId = ?";
            try {
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, productInMachine.getAmount());
                stmt.setString(2, productInMachine.getStatusInMachine().toString());
                stmt.setInt(3, getCurrentMonth());
                stmt.setInt(4, getCurrentDay());
                stmt.setInt(5, Integer.parseInt(productInMachine.getProductId()));
                stmt.setInt(6, Integer.parseInt(productInMachine.getMachineId()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                editResponse(response, ResponseCode.DB_ERROR,
                        "There was an error while updating history products in machine", null);
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            }
        } else {
            insertProductInMachineToHistory(response, productInMachine);
        }
    }

    private boolean checkIfInventoryHistoryHasCurrentDay(Response response, ProductInMachine productInMachine) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Object> resList = new ArrayList<>();
        String query = "SELECT * FROM product_in_machine_history " +
                "WHERE productId = ? AND machineId = ? AND updated_month = ? AND updated_day = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(productInMachine.getProductId()));
            stmt.setInt(2, Integer.parseInt(productInMachine.getMachineId()));
            stmt.setInt(3, getCurrentMonth());
            stmt.setInt(4, getCurrentDay());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole("There was an error while checkIfInventoryHistoryHasCurrentDay", true);
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while checkIfInventoryHistoryHasCurrentDay", null);
        }
        return false;
    }

        /**
         * function that get the customerId by the given orderId from DB.  edit the response accordingly.
         * @param response - Response object for the user
         * @param orderId - Order Id
         */
    public void getCustomerIdByOrderIdFromDB(Response response, String orderId) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Object> resList = new ArrayList<>();
        String query = "SELECT * FROM orders WHERE orderId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, orderId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resList.add(rs.getInt("customerId"));
                editResponse(response, ResponseCode.OK, "Successfully get customer id", resList);

            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that get machineName with given machineId according to the DB. edit the response accordingly.
     * @param response - Response object for the user
     * @param machineId - machineId
     */
    public void getMachineName(Response response, Integer machineId) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Object> resList = new ArrayList<>();
        String query = "SELECT * FROM machine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, machineId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resList.add(rs.getString("machineName"));
                editResponse(response, ResponseCode.OK, "Successfully get machine threshold", resList);

            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that get the monthly bill from db with given userId.  edit the response accordingly.
     * @param response - Response object for the user
     * @param userId - User id
     */
    public void getMonthlyBill(Response response, Integer userId) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Object> resList = new ArrayList<>();
        String query = "SELECT * FROM customers WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resList.add(rs.getDouble("monthlyBill"));
                editResponse(response, ResponseCode.OK, "Successfully get monthly bill", resList);

            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getCompletedOrders(Response response, Integer customerId) {
        List<Object> OrderedIds = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        // String query = "SELECT * FROM orders WHERE customerId = ?";
        String query = "SELECT * FROM orders JOIN customers ON orders.customerId = customers.id WHERE orders.customerId = ?";
        Boolean idExist = false;
        Boolean beenFlag = false;


        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                beenFlag = true;
                Integer id = rs.getInt("customerId");
                String orderId = rs.getString("orderId");
                String customerType = rs.getString("customerType");

                char firstDigit = orderId.charAt(0);
                if((id.equals(customerId) && firstDigit == '1')|| customerType.equals("Client"))
                {
                    idExist = true;
                    break;
                }

            }
            if(!beenFlag)
            {
                idExist = true;
                String query2 = "SELECT * FROM customers WHERE customers.id = ?";
                stmt = conn.prepareStatement(query2);
                stmt.setInt(1, customerId);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    String customerType = rs.getString("customerType");
                    if(customerType.equals("Subscriber"))
                    {
                        idExist = false;
                        break;
                    }

                }
            }
            OrderedIds.add(idExist);
            editResponse(response, ResponseCode.OK, "Successfully import all products from machine",OrderedIds);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that update the monthly bill of subscriber in DB given userId and new monthly bill sum. edit the response accordingly.
     * @param response - Response object for the user
     * @param userId - user Id
     * @param newMonthlyBill - new sum of monthly bill
     */
    public void updateMonthlyBill(Response response, Integer userId, Double newMonthlyBill) {

        PreparedStatement stmt;
        String query = "UPDATE customers SET monthlyBill= ? WHERE id = ?";
        try {
                stmt = conn.prepareStatement(query);
                stmt.setDouble(1, newMonthlyBill);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            ServerGui.serverGui.printToConsole("update Monthly Bill successfully");
            editResponse(response, ResponseCode.OK, "Successfully update Monthly Bill successfully", null);
            } catch (SQLException e) {
                editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * function that update in DB the order status accroding specific orderId and new orderStatus. edit the response accordingly.
     * @param response - Response object for the user
     * @param orderId - Order Id
     * @param orderStatus - OrderStatus Enum
     */
    public void updateOrderStatus(Response response, String orderId, OrderStatus orderStatus) {

        PreparedStatement stmt;
        String query = "UPDATE orders SET orderStatus = ? WHERE orderId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, getStringStatus(orderStatus));
            stmt.setString(2, orderId);
            stmt.executeUpdate();
            ServerGui.serverGui.printToConsole("update order status successfully");
            editResponse(response, ResponseCode.OK, "Successfully update order status", null);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    private String getStringStatus(Regions region) {
        if (region == null)
            return "null";
        switch (region) {
            case All:
                return "All";
            case North:
                return "North";
            case South:
                return "South";
            case UAE:
                return "UAE";
        }
        return "null";
    }

    private String getStringStatus(SaleStatus status) {
        if (status == null)
            return "null";
        switch (status) {
            case Template:
                return "Template";
            case Ready:
                return "Ready";
            case Running:
                return "Running";
            case Outdated:
                return "Outdated";
        }
        return "null";
    }

    private String getStringStatus(TimeSale time) {
        if (time == null)
            return "null";
        switch (time) {
            case AllDay:
                return "AllDay";
            case Morning:
                return "Morning";
            case Noon:
                return "Noon";
            case Afternoon:
                return "Afternoon";
            case Evening:
                return "Evening";
            case Night:
                return "Night";
        }
        return "null";
    }

    private String getStringStatus(OrderStatus status) {
        if (status == null)
            return "null";
		switch(status) {
		case NotCollected: return "NotCollected";
		case Collected: return "Collected";
		case WaitingApproveDelivery: return "WaitingApproveDelivery";
		case Done: return "Done";
	    }
		return "null";
    }

	/**
	 * This method creates a query to the db, requests all the machines inside a given region.
	 * @param response - the response object to build and send back to the client.
	 * @param region - the wanted region. the machines returned must exist in this region.
	 */
	public void getMachinesOfRegions(Response response, Regions region) {
    	List<Object> machines= new ArrayList<>();
    	Machine machine;
        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT * FROM machine WHERE region = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, region.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
            	machine = new Machine(rs.getString("machineId"), rs.getString("machineName"), rs.getString("region"), rs.getString("threshold"));
            	machines.add(machine);
            }
            if (machines.size() == 0) {
            	machines = null;
            }
            editResponse(response, ResponseCode.OK, "Successfully sent the machines of that region", machines);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading region machines, please try again", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	/**
	 * This method creates a query to the db, requests all the not-collected deliveries of a specific user.
	 * @param response - the response object to build and send back to the client.
	 * @param userId - the id of the specific user. this id connects between the orders and the user.
	 */
	public void getAmountNotificationDelivery(Response response, Integer userId) {
    	List<Object> amountDeliveryNotCollected = new ArrayList<>();
    	int count = 0;
        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT orderId FROM orders WHERE orderStatus = ? and customerId = ? and pickUpMethod = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, getStringStatus(OrderStatus.NotCollected));
        	stmt.setInt(2, userId);
        	stmt.setString(3, PickUpMethod.delivery.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
            	count++;
            }
            amountDeliveryNotCollected.add(count);
            editResponse(response, ResponseCode.OK, "Successfully sent the amount of notifications this user has", amountDeliveryNotCollected);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	/**
	 * This method creates a query to the db and calls two more methods inside it.
	 * The query created by this method requests the id of not collected order set to be picked up from the given machine with the given pickup code.
	 * Then, after making sure that the order exists and valid, it calls another method to update the status of the order to be collected(by changing the collect time-> time=now).
	 * @param response - the response object to build and send back to the client.
	 * @param userId - the id of the user with the pickup order
	 * @param pickupCode - the code generated after pickup order was created
	 * @param machineId - the id of the machine the picked up order set to be picked from
	 */
	public void putPickupCodeAndChangeStatus(Response response, Integer userId, String pickupCode, String machineId) {
    	List<String> ordersId = new ArrayList<>();

    	PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT orderId FROM orders WHERE orderStatus = ? and customerId = ? and pickUpMethod = ? and machineId = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, getStringStatus(OrderStatus.NotCollected));
        	stmt.setInt(2, userId);
        	stmt.setString(3, PickUpMethod.latePickUp.toString());
        	stmt.setInt(4, Integer.parseInt(machineId));

            rs = stmt.executeQuery();
            while (rs.next()) {
            	ordersId.add(rs.getString("orderId"));
            }
            
            if (ordersId.size() == 0) {
                editResponse(response, ResponseCode.INVALID_DATA, "Entered code is incorrect, please try again", null);
                rs.close();
                return;
            }
            rs.close();
            
            for (String order: ordersId) {
                if (isExistPickupOrder(response, order, pickupCode)) {
                	updateTimeReceived(response, order, pickupCode);
                    if (response.getCode() == ResponseCode.DB_ERROR || response.getCode() == ResponseCode.SERVER_ERROR) {
                    	break;
                    }
                	changeStatusPickupOrderAfterEnterCode(response, order);
                	break;
                }
                if (response.getCode() == ResponseCode.DB_ERROR || response.getCode() == ResponseCode.SERVER_ERROR) {
                	break;
                }

            }
            if (response.getCode() == null) {
                editResponse(response, ResponseCode.INVALID_DATA, "Entered code is incorrect, please try again", null);
            }
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
	private boolean isExistPickupOrder(Response response, String orderId, String pickupCode) {
		PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT orderId FROM pickupOrder WHERE orderId = ? and pickupCode = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, orderId);
        	stmt.setString(2, pickupCode);
            rs = stmt.executeQuery();
            if (rs.next()) {
                rs.close();
            	return true;
            }
            else {
                rs.close();
            	return false;
            }
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return false;
	}
	
	private void changeStatusPickupOrderAfterEnterCode(Response response, String orderId) {
        PreparedStatement stmt;
		String query = "UPDATE orders SET orderStatus= ? WHERE orderId= ?";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, getStringStatus(OrderStatus.Collected));
            stmt.setString(2, orderId);
    		stmt.executeUpdate();

            editResponse(response, ResponseCode.OK, "A valid code has been entered for a pickup order",null);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
	}
	
	private void updateTimeReceived(Response response, String orderId, String pickupCode) {
		PreparedStatement stmt;
        String query = "UPDATE pickupOrder SET dateReceived= ? WHERE orderId= ? and pickupCode = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setString(1, LocalDate.now().format(DateTimeFormatter.ofPattern(models.StyleConstants.DATE_FORMAT)));
        	stmt.setString(2, orderId);
        	stmt.setString(3, pickupCode);
    		stmt.executeUpdate();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
	}
	
	/**
	 * This method creates a query to the db, the query asks for a specific customer with a given id.
	 * @param response - the response object to build and send back to the client.
	 * @param user - the specific user the query asks for inside customers table. (contains id).
	 */
	public void getCustomer(Response response, User user) {
		List<Object> customerDetails = new ArrayList<>();
		Customer customer;
		PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT * FROM customers WHERE id = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setInt(1, user.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {   	
            	customer = new Customer(user,CustomerType.valueOf(rs.getString("customerType")),rs.getString("subscriberNumber"),rs.getInt("monthlyBill"));
            	customerDetails.add(customer);
            	editResponse(response, ResponseCode.OK, "Registered customer successfully accepted",customerDetails);
            }
            else {
                editResponse(response, ResponseCode.INVALID_DATA, "Unregistered user",null);
            }
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
	}
	
	/**
	 * This method creates a query to the db, sets the isLoggedIn variable in the table to be set with the given boolean variable for a specific user with given id.
	 * This method used for both ways, showing that a user is logged in and logged out.
	 * @param response - the response object to build and send back to the client.
	 * @param userId - the id of the specific user the method wants to change loggedin status of.
	 * @param isLoggedIn - a boolean variable, contains the value to be inserted into the isLoggedIn col in the table for a specific user
	 */
	public void changeLoggedInUser(Response response, Integer userId, boolean isLoggedIn) {
		PreparedStatement stmt;
        String query = "UPDATE users SET isLoggedIn = ? WHERE id = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setBoolean(1, isLoggedIn);
        	stmt.setInt(2, userId);
    		stmt.executeUpdate();
        	editResponse(response, ResponseCode.OK, response.getDescription(), response.getBody());
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        
	}
	
	/**
	 * This method uses getCustomer method, which requests a specific user with an id, who is also a customer, the method then
	 * checks if the user is using OL (allowed to enter OL configuration) by checking if he is a worker only or a worker/customer
	 * @param response - the response object to build and send back to the client.
	 * @param user - the specific user the method checks.
	 */
	public void getUserForOL(Response response, User user) {

		getCustomer(response, user);
		Worker worker = null;
		List<Object> userDetails = new ArrayList<>();
		worker = getWorker(response, user);
		if (response.getCode() == ResponseCode.OK) {
			userDetails.add(response.getBody().get(0));
			if (response.getCode() == ResponseCode.OK && worker != null) {
				userDetails.add(worker);
		    	editResponse(response, ResponseCode.OK, "The user is both a customer and an employee",userDetails);
			}
			return;
		}
		worker = getWorker(response, user);
		if (worker == null) {
		    editResponse(response, ResponseCode.INVALID_DATA, "Unregistered user",null);
		}
		else {
			userDetails.add(worker);
			editResponse(response, ResponseCode.OK, "The employee has successfully logged in",userDetails);
		}
	}
	
	
	private Worker getWorker(Response response, User user) {
		Worker worker = null;
		PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT * FROM workers WHERE id = ?";
        try {
        	stmt = conn.prepareStatement(query);
        	stmt.setInt(1, user.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
            	Regions tempRegion = (rs.getString("region") == null)? null: Regions.valueOf(rs.getString("region"));
            	worker = new Worker(user, WorkerType.valueOf(rs.getString("workerType")), tempRegion);
            }
            rs.close();
            return worker;
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return null;
	}
	
	
	/**
	 * This method creates a query to the db, the query asks for all the ids of subscribers.
	 * @param response - the response object to build and send back to the client.
	 */
	public void getSubscribersForFastLogin(Response response) {
		List<Object> subscribersId = new ArrayList<>();
		PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT id FROM customers WHERE customerType = ?";
        try {
        	stmt = conn.prepareStatement(query);
            stmt.setString(1, "Subscriber");
            rs = stmt.executeQuery();
            while (rs.next()) {
            	subscribersId.add(rs.getInt("id"));
            }
            rs.close();
        	editResponse(response, ResponseCode.OK, "Successfully sent all subscribers id",subscribersId);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
	}
	
	
	/**
	 * This method creates a query to the db, the query asks for a specific user from the users table with a specific given id.
	 * @param response - the response object to build and send back to the client.
	 * @param id - the id of wanted user.
	 */
	public void getUserById(Response response, Integer id) {

    	List<Object> userDetails= new ArrayList<>();

        User user;
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM users WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                user = new User(rs.getString("firstName"), rs.getString("lastName"), rs.getInt("id"),
                		rs.getString("email"), rs.getString("phoneNumber"), rs.getString("username"), rs.getString("userPassword"),
                		rs.getBoolean("isLoggedIn"), rs.getString("creditCardNumber"));
                if (user.isLoggedIn()) {
                    editResponse(response, ResponseCode.INVALID_DATA, "The user is already logged in", null);
                }
                else {
                    userDetails.add(user);
                    editResponse(response, ResponseCode.OK, "Successfully got user details", userDetails);
                	changeLoggedInUser(response, user.getId(), true);
                	getCustomer(response, user);
                }
            }
            else {
                editResponse(response, ResponseCode.INVALID_DATA, "The username or password are incorrect", null);
            }
            	
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    /**
     * This method creates a query to the db, the query requests all the sales from a specific region and a specific type.
     * This method handles the special case where a manager wants the sales from All regions.
     * @param response - the response object to build and send back to the client.
     * @param wantedRegion - the sales from this region.
     * @param wantedType - the sales of this type.
     */
    public void getSales(Response response, String wantedRegion, String wantedType) {
    	checkOutDatedSales();//updating all outdated sales.
        List<Object> salesWithWantedRegionAndType = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query;
        boolean allFlag = false;
        if (wantedRegion.equals("All")) {
            allFlag = true;
            query = "SELECT * FROM sales WHERE saleStatus = ?";
        } else {
            query = "SELECT * FROM sales WHERE saleStatus = ? and (saleRegion = ? or saleRegion = ?)";
        }
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, wantedType);
            if (!allFlag) {
                stmt.setString(2, wantedRegion);
                stmt.setString(3, "All");

            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                Sale sale = new Sale(Integer.toString(rs.getInt("id")), rs.getString("saleStartDate"),
                        rs.getString("saleEndDate"), TimeSale.valueOf(rs.getString("saleTime")),
                        rs.getString("saleName"), Regions.valueOf(rs.getString("saleRegion")),
                        SaleStatus.valueOf(rs.getString("saleStatus")), rs.getString("salePercentage"),
                        rs.getString("saleDescription"), TypeSale.valueOf(rs.getString("saleType")));

                salesWithWantedRegionAndType.add(sale);
            }
            if (salesWithWantedRegionAndType.size() == 0) {
                salesWithWantedRegionAndType = null;
            }
            editResponse(response, ResponseCode.OK, "Successfully sent the sales with the wanted type and region",
                    salesWithWantedRegionAndType);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    private String getStringStatus(TypeSale type) {
        if (type == null)
            return "null";
        switch (type) {
            case Sale2Plus1:
                return "Sale2Plus1";
            case Sale1Plus1:
                return "Sale1Plus1";
            case Sale2Plus2:
                return "Sale2Plus2";
            case PercentageDiscount:
                return "PercentageDiscount";
            case GetSecondOneAtDiscount:
                return "GetSecondOneAtDiscount";
        }
        return "null";
    }
    /**
     * This method creates a query to the db, the query requests to add new sale into the db, with a specific given sale.
     * All the sales created are of type Template.
     * @param response - the response object to build and send back to the client.
     * @param sale - the specific sale added to the db.
     */
    public void postSales(Response response, Sale sale) {
        PreparedStatement stmt;
        String query = "INSERT into sales (saleName, saleType, saleRegion, saleStatus, saleStartDate, saleEndDate, saleTime, saleDescription, salePercentage) VALUES (?, ?, ?, ?, ?, ?, ?,?,?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, sale.getSaleName());
            stmt.setString(2, getStringStatus(sale.getSaleType()));
            stmt.setString(3, getStringStatus(sale.getSaleRegion()));
            stmt.setString(4, getStringStatus(sale.getSaleStatus()));
            stmt.setString(5, sale.getSaleStartDate());
            stmt.setString(6, sale.getSaleEndDate());
            stmt.setString(7, getStringStatus(sale.getSaleTime()));
            stmt.setString(8, sale.getSaleDiscription());
            stmt.setString(9, sale.getSalePercentage());
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully added sale template", null);
            ServerGui.serverGui.printToConsole("Successfully added sale template");
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    /**
     * This method creates a query to the db, the query requests to change a specific sale (by a given sale id) to a specific type (with a given type).
     * @param response - the response object to build and send back to the client.
     * @param saleID - the id of the specific sale wanted to be updated.
     * @param wantedType - the type the method wants to change the sale into.
     */
    public void changeSaleStatus(Response response, String saleID, String wantedType) {

        PreparedStatement stmt;

        String query = "UPDATE sales SET saleStatus= ? WHERE id= ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, wantedType);
            stmt.setString(2, saleID);
            stmt.executeUpdate();
            ServerGui.serverGui.printToConsole("Update sale status - changed successfully");
            editResponse(response, ResponseCode.OK, "Successfully Updated sale status", null);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Communication problem, try again", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    /**
     * this method return all the users from the DB
     */
    public void getUsersWithTheirStatus(Response response) { // BADIHI
        User user;
        List<Object> users = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM users";

        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                user = new User(rs.getString("firstName"), rs.getString("lastName"), rs.getInt("id"),
                        rs.getString("email"), rs.getString("phoneNumber"), rs.getString("username"), rs.getString("userPassword"),
                        rs.getBoolean("isLoggedIn"), rs.getString("creditCardNumber"));


                users.add(user);
            }
            editResponse(response, ResponseCode.OK, "Successfully import all", users);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }


    }
    /**
     * this method get a user id check if he is client or subscriber
     */
    public void getUsersStatus(Response response, Integer id) { // BADIHI
        String status;
        List<Object> statusList = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM customers WHERE id = ?";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            while (rs.next()) {
                status = rs.getString("customerType");
                statusList.add(status);
            }
            if(statusList.isEmpty())
            {
                statusList.add("User");
            }
            editResponse(response, ResponseCode.OK, "Successfully import all", statusList);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    /**
     * this method get a user id and request to upgrade him to client
     */

    public void UpgradeUserToClient(Response response, Integer userId,String region) { // Badhi

        PreparedStatement stmt;
        String query = "INSERT into pending_users_for_upgrade (id,region) VALUES (?,?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2,region);
            stmt.executeUpdate();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        ServerGui.serverGui.printToConsole("Subscriber update done successfully");
        editResponse(response, ResponseCode.OK, "Successfully updated subscriber", null);

    }
    /**
     * this method get a client id and upgrade him to subscriber
     */
    public void UpgradeClientToSubscriber(Response response, Integer userId) { // Badihi

        PreparedStatement stmtForMax;
        ResultSet rs;
        Integer maxId=0;

        String queryGetMaxId = "SELECT MAX(subscriberNumber) FROM customers";
        try {
            stmtForMax = conn.prepareStatement(queryGetMaxId);
            rs = stmtForMax.executeQuery();
            while (rs.next()) {
                maxId = rs.getInt(1);
            }

        } catch (SQLException e1) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e1.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return;}


        PreparedStatement stmt;
        String query = "UPDATE customers SET customerType= ?,subscriberNumber = ? WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, "Subscriber");
            stmt.setInt(2,maxId+1 );
            stmt.setInt(3,userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        ServerGui.serverGui.printToConsole("Subscriber update done successfully");
        editResponse(response, ResponseCode.OK, "Successfully updated subscriber", null);
    }
    /**
     * this method get a user id and return true/false if he is pending to upgrade or not
     */
    public void checkIfUserPending(Response response, Integer id) { // BADIHI
        List<Object> statusList = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM pending_users_for_upgrade WHERE id = ?";

        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            while (rs.next()) {
                statusList.add(true);

            }
            if (statusList.isEmpty()) {
                statusList.add(false);

            }
            editResponse(response, ResponseCode.OK, "Successfully import all", statusList);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, EXECUTE_UPDATE_ERROR_MSG, null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }


    }
    
    
    /**
     * This method creates a mysql query, the query logs out all the users.
     * @param response - the response built for the client side.
     */
    public static void disconnectServer(Response response) {
        PreparedStatement stmt;
        String query = "UPDATE users SET isLoggedIn = ? WHERE isLoggedIn = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setBoolean(1, false);
            stmt.setBoolean(2, true);
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, response.getDescription(), response.getBody());
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
        }
    }

    /**
     * this method returns all users that are pending to upgrade to customer or not
     */
    public void getAllPendingUsers(Response response) {
        List<Integer> usersIdList = new ArrayList<>();
        List<Object> usersList = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM pending_users_for_upgrade";
        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                usersIdList.add(rs.getInt("id"));
            }
            rs.close();
        }  catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "There was an error while fetching data", null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return;
        }

        // getting all users
        Response innerResponse = new Response();
        getUsersWithTheirStatus(innerResponse);
        if (innerResponse.getCode() != ResponseCode.OK) { // if getUsersWithTheirStatus was failed
            response.setCode(innerResponse.getCode());
            response.setDescription(innerResponse.getDescription());
            response.setBody(innerResponse.getBody());
            return;
        }
        // filtering only pending users data
        List<User> allUser = innerResponse.getBody().stream()
                .map(userObject -> (User) userObject)
                .collect(Collectors.toList());
        for (User user : allUser) {
            if (usersIdList.contains(user.getId()))
                usersList.add(user);
        }
        if (usersList.isEmpty())
            usersList = null;
        editResponse(response, ResponseCode.OK, "Successfully import all pending users", usersList);
    }


    /**
     * this method returns if pending users upgrade has been done successfully or not
     * @param response - Response object for the user
     * @param idsToUpgrade - list of id of users to be upgraded
     */
    public void upgradeUsersToCostumers(Response response, List<Object> idsToUpgrade) {
        // checking if users are valid for upgrade (exists in pending table)
        for (Object objId : idsToUpgrade) {
            Integer id = (Integer) objId;
            upgradeUserToCostumer(response, id);
            if (response.getCode() != ResponseCode.OK) {
                return;
            }
        }
        editResponse(response, ResponseCode.OK, "Successfully confirmed all pending accounts", null);
        ServerGui.serverGui.printToConsole("Successfully confirmed all pending accounts");
    }

    /**
     * this method returns if pending user upgrade has been done successfully or not
     * @param response - Response object for the user
     * @param id - id of user to be upgraded
     */
    public void upgradeUserToCostumer(Response response, int id) {
        // checking if user is valid for upgrade (exists in pending table)
        checkIfUserPending(response, id);
        if (response.getCode() != ResponseCode.OK) {
            return;
        }

        // upgrading pending user
        PreparedStatement stmt;
        String insertNewCustomerQuery = "INSERT into customers (id, customerType, subscriberNumber, monthlyBill) " +
                "VALUES (?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(insertNewCustomerQuery);
            stmt.setInt(1, id);
            stmt.setString(2, CustomerType.Client.toString());
            stmt.setString(3, null);
            stmt.setDouble(4, 0.0);
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully confirmed pending account", null);
            ServerGui.serverGui.printToConsole("Successfully confirmed pending account");
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "There was an error while trying to confirm user", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return;
        }

        // removing from pending Table
        String deletePendingUserQuery = "DELETE from pending_users_for_upgrade WHERE id = ?";
        try {
            stmt = conn.prepareStatement(deletePendingUserQuery);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully deleted pending account", null);
            ServerGui.serverGui.printToConsole("Successfully deleted pending account from table");
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while trying to deleted pending account", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    /**
     * get the region of machine by the machineId
     * @param response - Response object
     * @param machineId - machine id
     */
    public void getRegionByMachineId(Response response, Integer machineId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM machine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, machineId);
            rs = stmt.executeQuery();
            if(rs.next())
                res.add(rs.getString("region"));
            editResponse(response, ResponseCode.OK, "Successfully get region by machine id", res);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * get the regional managers ids by the region
     * @param response Response object
     * @param region Regions enum
     */
    public void getRegionalManagersIds(Response response, Regions region){
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM workers WHERE region = ? and workerType = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, getStringStatus(region));
            stmt.setString(2, "RegionalManager");
            rs = stmt.executeQuery();
            while(rs.next())
                res.add(rs.getInt("id"));
            editResponse(response, ResponseCode.OK, "Successfully get regional manager ids", res);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * This method checks for all sales with endDate<now.
     * if the method finds finished sales it changes their status to Oudated.
     */
    void checkOutDatedSales() {
    	 PreparedStatement stmt;

         String query = "UPDATE sales SET saleStatus= ? WHERE STR_TO_DATE(saleEndDate, '%d-%m-%Y') < NOW()";
         try {
             stmt = conn.prepareStatement(query);
             stmt.setString(1, "Outdated");
             stmt.executeUpdate();
             ServerGui.serverGui.printToConsole("Update sale status - changed successfully");
//             editResponse(response, ResponseCode.OK, "Successfully Updated sale status", null);
         } catch (SQLException e) {
//             editResponse(response, ResponseCode.DB_ERROR, "Communication problem, try again", null);
             e.printStackTrace();
             ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
         }
    }

    void getOpenInventoryFillTasks(Response response, Integer assignedWorkerId) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Object> openedTasks;
        Map<Integer, InventoryFillTask> tasksMap = new HashMap<>();

        String query = "SELECT * FROM inventory_fill_tasks WHERE assignedWorker = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, assignedWorkerId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                TaskStatus status = TaskStatus.valueOf(rs.getString("status"));
                if (status == TaskStatus.CLOSED)
                    continue;
                String creationDate = rs.getString("creationDate");
                int machineId = rs.getInt("machineId");
                int assignedWorker = rs.getInt("assignedWorker");
                InventoryFillTask task = new InventoryFillTask(creationDate, machineId, status, assignedWorker);
                tasksMap.put(machineId, task);
            }
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while trying to get all opened tasks", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }

        // retrieving machine name and it's region
        query = "SELECT * FROM machine";
        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int machineId = rs.getInt("machineId");
                String machineName = rs.getString("machineName");
                String machineRegion = rs.getString("region");
                if (tasksMap.keySet().contains(machineId)) {
                    tasksMap.get(machineId).setMachineName(machineName);
                    tasksMap.get(machineId).setRegion(Regions.valueOf(machineRegion));
                }
            }
            openedTasks = Arrays.asList(tasksMap.values().toArray());
            if (openedTasks.isEmpty()) {
                openedTasks = null;
            }
            editResponse(response, ResponseCode.OK, "Successfully retrieved all opened tasks for worker", openedTasks);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while trying to get all machines data", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * @param response response to return to client if the request was done successfully
     * @param body body the has InventoryFillTask in it's first value
     */
    public void setInventoryTaskStatus(Response response, List<Object> body) {
        PreparedStatement stmt;
        InventoryFillTask task = (InventoryFillTask) body.get(0);

        String query = "UPDATE inventory_fill_tasks SET status = ? WHERE machineId = ? AND assignedWorker = ? AND creationDate = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, task.getStatus().dbName());
            stmt.setInt(2, task.getMachineId());
            stmt.setInt(3, task.getAssignedWorker());
            stmt.setString(4, task.getCreationDate());
            stmt.executeUpdate();
            ServerGui.serverGui.printToConsole("Inventory task status has been changed successfully");
            editResponse(response, ResponseCode.OK, "Successfully changed inventory task status", null);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while trying to set inventory task status", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * Finding the requested report and returning it (response.body)
     * @param response response to return to client if the request was done successfully and the report instance
     * @param reportRequest the report to search for in DB
     */
    public void getReport(Response response, SavedReportRequest reportRequest) {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM saved_reports WHERE year_creation = ? AND month_creation = ? AND report_type = ? AND region = ? AND machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, reportRequest.getYear());
            stmt.setInt(2, reportRequest.getMonth());
            stmt.setString(3, reportRequest.getReportType().name());
            stmt.setString(4, reportRequest.getRegion().name());
            if (reportRequest.getReportType() == ReportType.INVENTORY)
                stmt.setInt(5, reportRequest.getMachineId());
            else
                stmt.setInt(5, -1); // default value for orderReport and usersReport
            rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] bytes = rs.getBytes("report_data");
                List<Object> responseBody = new ArrayList<>();
                responseBody.add(getReportObject(bytes));
                editResponse(response, ResponseCode.OK, "Report data was fetched successfully", responseBody);
            } else {
                editResponse(response, ResponseCode.INVALID_DATA,
                        "Couldn't find report for the specified parameters",null);
            }
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while trying to fetch report data", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * Generating All types of reports and saves them in DB, with timestamp of current year + month
     * @param response response to return to client if the request was done successfully
     */
    public void generateAllReports(Response response) {
        // Checking if reports are already exists
        Boolean isReportsAlreadyCreated = checkIfReportsAreAlreadyCreated();
        if (isReportsAlreadyCreated == null) {
            editResponse(response, ResponseCode.DB_ERROR, "Failed while checking if reports are exists", null);
            return;
        }
        if (isReportsAlreadyCreated) {
            editResponse(response, ResponseCode.OK, REPORTS_ALREADY_EXISTS, null);
            return;
        }

        boolean isReportCreationFailed = false;
        List<ReportType> failedReports = new ArrayList<>();
        if (!generateInventoryReport(response)) {
            isReportCreationFailed = true;
            failedReports.add(ReportType.INVENTORY);
            ServerGui.serverGui.printToConsole(
                    "Failed Generating Report because of " + response.getDescription(), true);
        }
        if (!generateOrdersReport(response)) {
            isReportCreationFailed = true;
            failedReports.add(ReportType.ORDERS);
            ServerGui.serverGui.printToConsole(
                    "Failed Generating Report because of " + response.getDescription(), true);
        }
        if (!generateUsersReport(response)) {
            isReportCreationFailed = true;
            failedReports.add(ReportType.USERS);
            ServerGui.serverGui.printToConsole(
                    "Failed Generating Report because of " + response.getDescription(), true);
        }

        if (!isReportCreationFailed) {
            editResponse(response, ResponseCode.OK, NEW_REPORTS_CREATED, null);
        } else {
            StringBuilder failedString = new StringBuilder();
            for (ReportType failedReport : failedReports) {
                failedString.append(failedReport.name()).append(" | ");
            }
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while trying to generate all reports", null);
            System.out.println("Generating Reports: " + failedString);
            ServerGui.serverGui.printToConsole("Failed Generating Reports: " + failedString, true);
        }
    }

    private Object getReportObject(byte[] bytes) {
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
            return objectInputStream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean generateInventoryReport(Response response) {
        boolean isReportOfARegionFailed = false;
        // map that holds key: machineId, value: it's Region (as String)
        Map<Integer, String> regionByMachineId = new HashMap<>();
        // map that holds key: machineId, value: it's Name (as String)
        Map<Integer, String> nameByMachineId = new HashMap<>();
        // map that holds key: productId, value: it's Name (as String)
        Map<Integer, String> nameByProductId = new HashMap<>();
        // map that holds key: region, value: it's productsInMachine
        Map<String, List<Integer>> machineIdsByRegion = new HashMap<>();
        // map that holds key: machineId, value: it's productsInMachine
        Map<Integer, List<ProductInMachineHistory>> productsInMachineByMachineId = new HashMap<>();

        List<ProductInMachineHistory> filteredByDateProducts = getAllProductsHistory();
        if (filteredByDateProducts == null) {
            response.setDescription("failed in getAllProductsHistory");
            return false;
        }

        // mapping product ids to their names
        Set<Integer> productIdsSet = filteredByDateProducts.stream()
                .map(ProductInMachineHistory::getProductId)
                .map(Integer::parseInt).collect(Collectors.toSet());

        for (Integer productId : productIdsSet) {
            Response responseToName = new Response();
            getNameByProductId(responseToName, productId);
            if (responseToName.getCode() != ResponseCode.OK) {
                response.setDescription("failed in getNameByProductId");
                return false;
            }
            String productName = responseToName.getBody().get(0).toString();
            nameByProductId.put(productId, productName);
        }

        // mapping machine ids to their names
        Set<Integer> machineIdsSet = filteredByDateProducts.stream()
                .map(ProductInMachineHistory::getMachineId)
                .map(Integer::parseInt).collect(Collectors.toSet());

        // mapping all machines to their regions and names
        for (Integer machineId : machineIdsSet) {
            Response responseToRegion = new Response();
            getRegionAndNameByMachineId(responseToRegion, machineId);
            if (responseToRegion.getCode() != ResponseCode.OK) {
                response.setDescription("failed in getRegionByMachineId");
                return false;
            }
            String machineRegion = responseToRegion.getBody().get(0).toString();
            String machineName = responseToRegion.getBody().get(1).toString();
            regionByMachineId.put(machineId, machineRegion);
            nameByMachineId.put(machineId, machineName);
        }

        // init all machine ids by region
        machineIdsByRegion.put(Regions.North.name(), new ArrayList<>());
        machineIdsByRegion.put(Regions.South.name(), new ArrayList<>());
        machineIdsByRegion.put(Regions.UAE.name(), new ArrayList<>());
        // iterating over all machine ids and mapping them to their region
        for (Map.Entry<Integer, String> regionMachineId : regionByMachineId.entrySet()) {
            Integer mappedMachineId = regionMachineId.getKey();
            String mappedRegion = regionMachineId.getValue();
            // adding machineId to it's region
            machineIdsByRegion.get(mappedRegion).add(mappedMachineId);
        }

        for (ProductInMachineHistory productHistory : filteredByDateProducts) {
            Integer machineId = Integer.valueOf(productHistory.getMachineId());
            if (!productsInMachineByMachineId.containsKey(machineId)) {
                productsInMachineByMachineId.put(machineId, new ArrayList<>());
            }
            productsInMachineByMachineId.get(machineId).add(productHistory);
        }

        int daysInMonth = getDaysInMonthOfReport();
        // ---- Generating Reports ----
        // Iterating over all regions and filling relevant data
        for (Map.Entry<String, List<Integer>> machineIdsOfARegion : machineIdsByRegion.entrySet()) {
            for (Map.Entry<Integer, List<ProductInMachineHistory>> productsOfAMachineId : productsInMachineByMachineId.entrySet()) {
                // excluding all non-current region machines
                if (!machineIdsOfARegion.getValue().contains(productsOfAMachineId.getKey()))
                    continue;

                // List of days with map that holds <key: ProductName, value: amount>
                List<Map<String, Integer>> dailyInventory = new ArrayList<>();
                // List of days with map that holds amount of products below threshold
                List<Integer> belowThresholdAmount = new ArrayList<>();
                // List of days with map that holds amount of unavailable products
                List<Integer> unavailableAmount = new ArrayList<>();

                // init lists with default values
                for (int iteratedDay = 1; iteratedDay <= daysInMonth; iteratedDay++) {
                    dailyInventory.add(new HashMap<>());
                    belowThresholdAmount.add(0);
                    unavailableAmount.add(0);
                }

                // iterating over all days in current month and filling with actual values
                for (int iteratedDay = 1; iteratedDay <= daysInMonth; iteratedDay++) {
                    int finalIteratedDay = iteratedDay;
                    List<ProductInMachineHistory> dailyProductsHistory = productsOfAMachineId.getValue().stream()
                            .filter(productInMachineHistory -> productInMachineHistory.getDay() == finalIteratedDay)
                            .collect(Collectors.toList());
                    fillInventoryOfCurrentAndFollowingDays(dailyProductsHistory, nameByProductId,
                            iteratedDay, daysInMonth,
                            dailyInventory, belowThresholdAmount, unavailableAmount);
                }

                String machineName = nameByMachineId.get(productsOfAMachineId.getKey());
                InventoryReport inventoryReport = new InventoryReport(
                        machineName, String.valueOf(getReportsMonth()), String.valueOf(getReportsYear()),
                        dailyInventory, belowThresholdAmount, unavailableAmount);

                byte[] reportBytes = getSerializedObject(inventoryReport);
                Response responseForSaveReport = new Response();
                saveReportInDB(responseForSaveReport, ReportType.INVENTORY,
                        Regions.valueOf(machineIdsOfARegion.getKey()), productsOfAMachineId.getKey(), reportBytes);
                if (responseForSaveReport.getCode() != ResponseCode.OK) {
                    appendDescription(responseForSaveReport, response);
                    isReportOfARegionFailed = true;
                }
            }
        }
        if (!isReportOfARegionFailed) {
            Response responseForCleaning = new Response();
            deleteLastMonthFromTable(responseForCleaning);
            if (responseForCleaning.getCode() != ResponseCode.OK) {
                appendDescription(responseForCleaning, response);
                isReportOfARegionFailed = true;
            }

            transferDataFromProductInMachineToHistory(responseForCleaning);
            if (responseForCleaning.getCode() != ResponseCode.OK) {
                appendDescription(responseForCleaning, response);
                isReportOfARegionFailed = true;
            }
        }
        return !isReportOfARegionFailed;
    }

    private boolean generateOrdersReport(Response response) {
        boolean isReportOfARegionFailed = false;
        // map that holds key: region, value: it's orders
        Map<String, List<Order>> ordersByRegion = new HashMap<>();
        // map that holds key: machineId, value: it's Region (as String)
        Map<Integer, String> regionByMachineId = new HashMap<>();
        // map that holds key: machineId, value: it's Name (as String)
        Map<Integer, String> nameByMachineId = new HashMap<>();
        List<Order> filteredByDateOrder = getAllNonDeliveryOrders();
        if (filteredByDateOrder == null) {
            response.setDescription("failed in getAllNonDeliveryOrders");
            return false;
        }
        Set<Integer> machineIdsSet = filteredByDateOrder.stream()
                .map(Order::getMachineId)
                .map(Integer::parseInt).collect(Collectors.toSet());

        // mapping all machines to their regions and names
        for (Integer integer : machineIdsSet) {
            Response responseToRegion = new Response();
            getRegionAndNameByMachineId(responseToRegion, integer);
            if (responseToRegion.getCode() != ResponseCode.OK) {
                response.setDescription("failed in getRegionByMachineId");
                return false;
            }
            String machineRegion = responseToRegion.getBody().get(0).toString();
            String machineName = responseToRegion.getBody().get(1).toString();
            regionByMachineId.put(integer, machineRegion);
            nameByMachineId.put(integer, machineName);
        }
        // init all orders by region
        ordersByRegion.put(Regions.North.name(), new ArrayList<>());
        ordersByRegion.put(Regions.South.name(), new ArrayList<>());
        ordersByRegion.put(Regions.UAE.name(), new ArrayList<>());

        for (Order order : filteredByDateOrder) {
            String orderRegion = regionByMachineId.get(Integer.parseInt(order.getMachineId()));
            ordersByRegion.get(orderRegion).add(order);
        }

        // ---- Generating Reports ----
        // Iterating over all regions and filling relevant data
        for (Map.Entry<String,List<Order>> orderOfRegion : ordersByRegion.entrySet()) {
            // List of days with map that holds <key: MachineName, value: amountOfOrders in EK>
            List<Map<String, Integer>> ekOrders = new ArrayList<>();
            // List of days with map that holds <key: MachineName, value: amountOfOrders in latePickup>
            List<Map<String, Integer>> latePickupOrders = new ArrayList<>();

            // init maps with default values
            for (int i = 1; i <= getDaysInMonthOfReport(); i++) {
                Map<String, Integer> ekMap = new HashMap<>();
                Map<String, Integer> latePickupMap = new HashMap<>();
                for (Map.Entry<Integer, String> machineNameById : nameByMachineId.entrySet()) {
                    String regionOfMachine = regionByMachineId.get(machineNameById.getKey());
                    if (orderOfRegion.getKey().equals(regionOfMachine)) {
                        ekMap.put(machineNameById.getValue(), 0);
                        latePickupMap.put(machineNameById.getValue(), 0);
                    }
                }
                ekOrders.add(ekMap);
                latePickupOrders.add(latePickupMap);
            }
            // iterating over all days in current month and filling with actual values
            for (int iteratedDay = 1; iteratedDay <= getDaysInMonthOfReport(); iteratedDay++) {
                int dayIndex = iteratedDay-1;

                // filling with actual values
                for (Order order : orderOfRegion.getValue()) {
                    // excluding orders that was done in day != to iterated day
                    if (!isDayInDate(iteratedDay, order.getDate()))
                        continue;
                    if (order.getPickUpMethod() == PickUpMethod.selfPickUp) {
                        String machineName = nameByMachineId.get(Integer.parseInt(order.getMachineId()));
                        Map<String, Integer> dayMap = ekOrders.get(dayIndex);
                        dayMap.put(machineName, dayMap.get(machineName) + 1); // increment counter of orders
                    }
                    else if (order.getPickUpMethod() == PickUpMethod.latePickUp) {
                        String machineName = nameByMachineId.get(Integer.parseInt(order.getMachineId()));
                        Map<String, Integer> dayMap = latePickupOrders.get(dayIndex);
                        dayMap.put(machineName, dayMap.get(machineName) + 1); // increment counter of orders
                    }
                }
            }

            OrdersReport ordersReport = new OrdersReport(
                    orderOfRegion.getKey(),
                    String.valueOf(getReportsMonth()),
                    String.valueOf(getReportsYear()),
                    ekOrders,
                    latePickupOrders);
            byte[] reportBytes = getSerializedObject(ordersReport);
            Response responseForSaveReport = new Response();
            saveReportInDB(
                    responseForSaveReport, ReportType.ORDERS, Regions.valueOf(orderOfRegion.getKey()), reportBytes);
            if (responseForSaveReport.getCode() != ResponseCode.OK) {
                appendDescription(responseForSaveReport, response);
                isReportOfARegionFailed = true;
            }
        }

        return !isReportOfARegionFailed;
    }

    private boolean generateUsersReport(Response response) {
        boolean isReportOfARegionFailed = false;
        // map that holds key: machineId, value: it's Region (as String)
        Map<Integer, String> regionByMachineId = new HashMap<>();
        // map that holds key: region, value: it's orders
        Map<String, List<Order>> ordersByRegion = new HashMap<>();
        List<Order> filteredByDateOrder = getAllNonDeliveryOrders();
        if (filteredByDateOrder == null) {
            response.setDescription("failed in getAllNonDeliveryOrders");
            return false;
        }

        // init set of all machineIds from all orders
        Set<Integer> machineIdsSet = filteredByDateOrder.stream()
                .map(Order::getMachineId)
                .map(Integer::parseInt).collect(Collectors.toSet());

        // mapping all machines to their regions
        for (Integer integer : machineIdsSet) {
            Response responseToRegion = new Response();
            getRegionAndNameByMachineId(responseToRegion, integer);
            if (responseToRegion.getCode() != ResponseCode.OK) {
                response.setDescription("failed in getRegionByMachineId");
                return false;
            }
            String machineRegion = responseToRegion.getBody().get(0).toString();
            regionByMachineId.put(integer, machineRegion);
        }

        // init all orders by region
        ordersByRegion.put(Regions.North.name(), new ArrayList<>());
        ordersByRegion.put(Regions.South.name(), new ArrayList<>());
        ordersByRegion.put(Regions.UAE.name(), new ArrayList<>());

        for(Order currentOrder : filteredByDateOrder) {
            Integer currentMachineId = Integer.parseInt(currentOrder.getMachineId());
            String region = regionByMachineId.get(currentMachineId);
            ordersByRegion.get(region).add(currentOrder);
        }
        // now we have the orders based on the region

        // ---- Generating Reports ----
        // Iterating over all regions and filling relevant data
        for (Map.Entry<String,List<Order>> orderOfRegion : ordersByRegion.entrySet()) {
            // map of all orders of clients that holds <key: clientId, value: amountOfOrders>
            Map<String, Integer> clientsOrders = new HashMap<>();
            // map of all orders of subscribers that holds <key: clientId, value: amountOfOrders>
            Map<String, Integer> subscribersOrders = new HashMap<>();

            // filling with actual values
            for (Order order : orderOfRegion.getValue()) {
                // checking of orderId starts with "0" or "1"
                if (order.getOrderId().startsWith("0")) {
                    String currentClientId = order.getCustomerId().toString();
                    if(!clientsOrders.containsKey(currentClientId))
                        clientsOrders.put(currentClientId, 0);
                    int newValue = clientsOrders.get(currentClientId) + 1;
                    clientsOrders.put(currentClientId, newValue);
                } else { // starts with "1"
                    String currentSubscriberId = order.getCustomerId().toString();
                    if(!subscribersOrders.containsKey(currentSubscriberId))
                        subscribersOrders.put(currentSubscriberId, 0);
                    int newValue = subscribersOrders.get(currentSubscriberId) + 1;
                    subscribersOrders.put(currentSubscriberId, newValue);
                }
            }

            UsersReport usersReport = new UsersReport(orderOfRegion.getKey(),
                    String.valueOf(getReportsMonth()),
                    String.valueOf(getReportsYear()),
                    clientsOrders,
                    subscribersOrders,
                    new ArrayList<>());

            // this map has for key: userId, and for value: amount of orders
            // map is organized from most to least orders
            Map<String, Integer> top3UserIdAndAmount = usersReport.getTop3UserIdAndAmount();
            List<String> top3Names = new LinkedList<>();
            for (String Id : top3UserIdAndAmount.keySet()) {
                Response userDataResponse = new Response();
                User currentUser = getUserDatabyId(userDataResponse, Integer.parseInt(Id));
                if (currentUser == null) {
                    isReportOfARegionFailed = true;
                    break;
                }
                top3Names.add(currentUser.getFirstName() + " " + currentUser.getLastName());
            }
            // if there is an error importing data of top3clients
            if (isReportOfARegionFailed)
                continue;

            usersReport.setTop3ClientNames(top3Names);

            byte[] reportBytes = getSerializedObject(usersReport);
            Response responseForSaveReport = new Response();
            saveReportInDB(
                    responseForSaveReport, ReportType.USERS, Regions.valueOf(orderOfRegion.getKey()), reportBytes);
            if (responseForSaveReport.getCode() != ResponseCode.OK) {
                appendDescription(responseForSaveReport, response);
                isReportOfARegionFailed = true;
            }
        }

        return !isReportOfARegionFailed;
    }

    /**
     * Saves the Requested report (in bytes - blob) in save_reports table in DB
     * @param response holds status of the perform function
     * @param type Report type to save
     * @param region Region area the report to save relate to
     * @param machineId MachineId the report to save relate to (if not
     * @param blobBytes Byte of the Report Object to save
     */
    private void saveReportInDB(Response response, ReportType type, Regions region, int machineId, byte[] blobBytes) {
        PreparedStatement stmt;
        String query = "INSERT into " +
                "saved_reports (year_creation, month_creation, report_type, region, machineId, report_data) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, getReportsYear());
            stmt.setInt(2, getReportsMonth());
            stmt.setString(3, type.name());
            stmt.setString(4, region.name());
            stmt.setInt(5, machineId);
            stmt.setBytes(6, blobBytes);

            stmt.executeUpdate();
            String msg = String.format("Successfully saved report %s of region %s", type.name(), region.name());
            if (machineId != -1)
                msg = String.format("%s of machine %s", msg, machineId);
            editResponse(response, ResponseCode.OK, msg, null);
            ServerGui.serverGui.printToConsole(msg);
        } catch (SQLException e) {
            String msg = String.format("There was an error in saving report %s of region %s", type.name(), region.name());
            if (machineId != -1)
                msg = String.format("%s of machine %s", msg, machineId);
            editResponse(response, ResponseCode.DB_ERROR, msg, null);
            ServerGui.serverGui.printToConsole(msg);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * Calls the actual saveReportInDB with default machineId value of -1 (not relevant to that reports)
     */
    private void saveReportInDB(Response response, ReportType type, Regions region, byte[] blobBytes) {
        saveReportInDB(response, type, region, -1, blobBytes);
    }

    /**
     * @return True if reports are already been created earlier (they are saved in DB)
     */
    private Boolean checkIfReportsAreAlreadyCreated() {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM saved_reports WHERE year_creation = ? AND month_creation = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, getReportsYear());
            stmt.setInt(2, getReportsMonth());
            rs = stmt.executeQuery();
            boolean hasReport = rs.next();
            rs.close();
            return hasReport;
        } catch (SQLException e) {
            return null;
        }
    }


    /**
     * Filling dailyInventory & belowThresholdAmount & unavailableAmount
     * in indexes of fromDay to toDay by dailyProductsHistory data
     * @param dailyProductsHistory The product in machine of a specific day (=fromDay)
     * @param nameByProductId All Names of products mapped by their ID
     * @param fromDay the day to start filling all result lists
     * @param toDay day number to stop filling list
     * @param dailyInventory List of days with map that holds -> key: ProductId, value: amount
     * @param belowThresholdAmount List of days with map that holds amount of products below threshold
     * @param unavailableAmount List of days with map that holds amount of unavailable products
     */
    private void fillInventoryOfCurrentAndFollowingDays(List<ProductInMachineHistory> dailyProductsHistory,
                                                        Map<Integer, String> nameByProductId,
                                                        int fromDay,
                                                        int toDay,
                                                        List<Map<String, Integer>> dailyInventory,
                                                        List<Integer> belowThresholdAmount,
                                                        List<Integer> unavailableAmount) {
        for (int day = fromDay; day <= toDay; day++) {
            int dayIndex = day - 1;
            Map<String, Integer> inventoryOfADay = dailyInventory.get(dayIndex);
            for (ProductInMachineHistory product : dailyProductsHistory) {
                Integer productId = Integer.parseInt(product.getProductId());
                // if productId already exists, it's replacing its amount
                inventoryOfADay.put(nameByProductId.get(productId), product.getAmount());
                if (product.getStatusInMachine() == StatusInMachine.Below)
                    belowThresholdAmount.set(dayIndex, belowThresholdAmount.get(dayIndex) + 1);
                else if (product.getStatusInMachine() == StatusInMachine.Not_Available) {
                    belowThresholdAmount.set(dayIndex, belowThresholdAmount.get(dayIndex) + 1);
                    unavailableAmount.set(dayIndex, unavailableAmount.get(dayIndex) + 1);
                }
            }
        }
    }

    private List<Order> getOrdersByUserType(String typeOfClient) {
        PreparedStatement stmt;
        ResultSet rs;
        List<Order> orders = new ArrayList<>();

        String query = "SELECT * FROM orders WHERE orderId LIKE '?%' AND pickUpMethod != ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, typeOfClient);
            stmt.setString(2, PickUpMethod.delivery.toString());
            rs = stmt.executeQuery();
            while(rs.next()) {
                String orderDate = rs.getString("orderDate");
                if (!isInCurrentMonth(orderDate))
                    continue;
                Order order = new Order(
                        rs.getString("orderId"),
                        orderDate,
                        rs.getInt("price"),
                        rs.getString("machineId"),
                        OrderStatus.valueOf(rs.getString("orderStatus")),
                        PickUpMethod.valueOf(rs.getString("pickUpMethod")),
                        rs.getInt("customerId"));
                orders.add(order);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
        return orders;
    }

    /**
     * @return List of products in machine of the report's month
     */
    private List<ProductInMachineHistory> getAllProductsHistory() {
        PreparedStatement stmt;
        ResultSet rs;
        List<ProductInMachineHistory> productInMachineHistories = new ArrayList<>();

        String query = "SELECT * FROM product_in_machine_history WHERE updated_month = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, getReportsMonth());
            rs = stmt.executeQuery();
            while (rs.next()) {
                String machineId = String.valueOf(rs.getInt("machineId"));
                String productId = String.valueOf(rs.getInt("productId"));
                StatusInMachine statusInMachine = StatusInMachine.valueOf(rs.getString("statusInMachine"));
                Integer amount = rs.getInt("amountInMachine");
                int month = rs.getInt("updated_month");
                int day = rs.getInt("updated_day");
                ProductInMachineHistory product = new ProductInMachineHistory(
                        machineId, productId, statusInMachine, amount, month, day);
                productInMachineHistories.add(product);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
        return productInMachineHistories;
    }

    /**
     * @return List of monthly orders excluding delivery orders
     */
    private List<Order> getAllNonDeliveryOrders() {
        PreparedStatement stmt;
        ResultSet rs;
        List<Order> orders = new ArrayList<>();

        String query = "SELECT * FROM orders WHERE pickUpMethod != ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, PickUpMethod.delivery.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                String orderDate = rs.getString("orderDate");
                if (!isInCurrentMonth(orderDate))
                    continue;
                Order order = new Order(
                        rs.getString("orderId"),
                        orderDate,
                        rs.getInt("price"),
                        rs.getString("machineId"),
                        OrderStatus.valueOf(rs.getString("orderStatus")),
                        PickUpMethod.valueOf(rs.getString("pickUpMethod")),
                        rs.getInt("customerId"));
                orders.add(order);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
        return orders;
    }

    /**
     * Retrieve Machine's region and Name by its ID
     */
    private void getRegionAndNameByMachineId(Response response, Integer machineId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM machine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, machineId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                res.add(rs.getString("region"));
                res.add(rs.getString("machineName"));
            }
            editResponse(response, ResponseCode.OK, "Successfully get region by machine id", res);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    /**
     * Retrieve Machine's region and Name by its ID
     */
    private void getNameByProductId(Response response, Integer productId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM products WHERE productId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                res.add(rs.getString("productName"));
            }
            editResponse(response, ResponseCode.OK, "Successfully get name by product id", res);
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while searching for name of product", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    private void deleteLastMonthFromTable(Response response) {
        int currentMonth = getCurrentMonth();
        PreparedStatement stmt;
        String query = "DELETE FROM product_in_machine_history WHERE updated_month != ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentMonth);
            stmt.executeUpdate();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "[Report] Failed to delete previous month's inventory data", null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }

        editResponse(response, ResponseCode.OK,
                "[Report] Successfully deleted previous month's inventory data", null);
    }

    private void transferDataFromProductInMachineToHistory(Response response) {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM productinmachine";
        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                ProductInMachine productInMachine = new ProductInMachine(
                        Integer.toString(rs.getInt("machineId")),
                        Integer.toString(rs.getInt("productId")),
                        StatusInMachine.valueOf(rs.getString("statusInMachine")),
                        rs.getInt("amountInMachine"));
                insertProductInMachineToHistory(response, productInMachine);
                if (response.getCode() != ResponseCode.OK)
                    break;
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (response.getCode() != ResponseCode.OK)
                ServerGui.serverGui.printToConsole(response.getDescription(), true);
            else
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        editResponse(response, ResponseCode.OK, "[Report] Successfully inserting all of current month's inventory data to table", null);
    }

    private void insertProductInMachineToHistory(Response response, ProductInMachine productInMachine) {
        PreparedStatement stmt;
        String query = "INSERT INTO product_in_machine_history " +
                "(productId, machineId, amountInMachine, statusInMachine, updated_month, updated_day)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(productInMachine.getProductId()));
            stmt.setInt(2, Integer.parseInt(productInMachine.getMachineId()));
            stmt.setInt(3, productInMachine.getAmount());
            stmt.setString(4, productInMachine.getStatusInMachine().name());
            stmt.setInt(5, getCurrentMonth());
            stmt.setInt(6, getCurrentDay());
            stmt.executeUpdate();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR,
                    "[Report] Failed to insert current month's inventory data to table", null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        editResponse(response, ResponseCode.OK,
                "[Report] Successfully inserting current month's inventory data to table", null);
    }

    private int getDaysInMonthOfReport() {
        // Get the number of days in that month
        YearMonth yearMonthObject = YearMonth.of(getReportsYear(), getReportsMonth());
        return yearMonthObject.lengthOfMonth();
    }

    /**
     * @return true if day of dbDate is equal to specified day
     */
    private boolean isDayInDate(int day, String dbDate) {
        return day == extractDay(dbDate);
    }

    private boolean isInCurrentMonth(String dbDate) {
        return getReportsYear() == extractYear(dbDate) && getReportsMonth() == extractMonth(dbDate);
    }

    private int extractYear(String yearByFormat) {
        // according to the format models.StyleConstants.DATE_FORMAT
        return Integer.parseInt(yearByFormat.split("-")[2]);
    }

    private int extractMonth(String monthByFormat) {
        // according to the format models.StyleConstants.DATE_FORMAT
        String month = monthByFormat.split("-")[1];
        if (month.startsWith("0"))
            month = month.substring(1);
        return Integer.parseInt(month);
    }

    private int extractDay(String dayByFormat) {
        // according to the format models.StyleConstants.DATE_FORMAT
        String month = dayByFormat.split("-")[0];
        if (month.startsWith("0"))
            month = month.substring(1);
        return Integer.parseInt(month);
    }

    /**
     * @return The year of report to be created
     */
    private int getReportsYear() {
        int currentMonth = getCurrentMonth();
        int currentYear = Year.now().getValue();

        if (currentMonth == 1) {
            return currentYear - 1;
        }
        return currentYear;
    }

    /**
     * @return The month of report to be created
     */
    private int getReportsMonth() {
        int currentMonth = getCurrentMonth();

        if (currentMonth == 1) {
            return 12;
        }
        return currentMonth - 1;
    }

    private int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    private int getCurrentDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    private void appendDescription(Response fromResponse, Response toResponse) {
        if (toResponse.getDescription() == null)
            toResponse.setDescription(fromResponse.getDescription());
        else
            toResponse.setDescription(toResponse.getDescription() + " | " + fromResponse.getDescription());
    }

    private byte[] getSerializedObject(Object object) {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(arrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return arrayOutputStream.toByteArray();
    }
}
