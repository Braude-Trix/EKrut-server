package server;

import gui.ServerGui;
import javafx.scene.image.Image;
import serverModels.ServerConf;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.*;
import sun.misc.IOUtils;
import com.mysql.cj.conf.ConnectionUrl.Type;

public class mysqlController {
    public Connection conn;
    public Connection externalDBSchemeConn;
    private final String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";

    public mysqlController(ServerConf serverConf, String externalDBSchemeName) {
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
            externalDBSchemeConn = DriverManager.getConnection(
                    String.format("jdbc:mysql://localhost/%s?serverTimezone=IST&useSSL=false", externalDBSchemeName),
                    dbUserName,
                    dbPassword);
            ServerGui.serverGui.printToConsole("SQL connection succeed");
            ServerGui.serverGui.setConnected(true);
        } catch (SQLException ex) { // handle any errors
            ServerGui.serverGui.printToConsole("SQLException: " + ex.getMessage(), true);
            ServerGui.serverGui.printToConsole("SQLState: " + ex.getSQLState(), true);
            ServerGui.serverGui.printToConsole("VendorError: " + ex.getErrorCode(), true);
            ServerGui.serverGui.setConnected(false);
        }
        importUsersDateFromExternalDB(serverConf, externalDBSchemeName);
    }

    public boolean closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                ServerGui.serverGui.printToConsole("SQL connection was closed");
            } catch (SQLException e) {
                ServerGui.serverGui.printToConsole("Couldn't close SQL connection", true);
                return false;
            }
        }
        return true;
    }

    public void importUsersDateFromExternalDB(ServerConf serverConf, String externalDBSchemeName){
        String queryUsers = "INSERT INTO " + serverConf.getDbScheme() + "." + "users" + " SELECT * FROM " + externalDBSchemeName + "." + "users";
        String queryWorkers = "INSERT INTO " + serverConf.getDbScheme() + "." + "workers" + " SELECT * FROM " + externalDBSchemeName + "." + "workers";
        String queryCustomers = "INSERT INTO " + serverConf.getDbScheme() + "." + "customers" + " SELECT * FROM " + externalDBSchemeName + "." + "customers";
        try {
            Statement stmtUsers = externalDBSchemeConn.createStatement();
            stmtUsers.executeUpdate(queryUsers);
            Statement stmtWorkers = externalDBSchemeConn.createStatement();
            stmtWorkers.executeUpdate(queryWorkers);
            Statement stmtCustomers = externalDBSchemeConn.createStatement();
            stmtCustomers.executeUpdate(queryCustomers);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * this method get an Subscriber object and save his details to DB.
     * NOTE: Existing ID Subscriber OR null value except from subscriberNumber will raise an exception!
     */
//    public void saveSubscriberToDB(Subscriber subscriber) {
//        PreparedStatement stmt;
//        ServerGui.serverGui.printToConsole("Inserting Subscriber to DB");
//        String query = "INSERT into Subscriber VALUES (?, ?, ?, ?, ?, ?, ?)";
//        try {
//            stmt = conn.prepareStatement(query);
//            stmt.setString(1, subscriber.getFirstName());
//            stmt.setString(2, subscriber.getLastName());
//            stmt.setString(3, subscriber.getId());
//            stmt.setString(4, subscriber.getPhoneNumber());
//            stmt.setString(5, subscriber.getEmailAddress());
//            stmt.setString(6, subscriber.getCreditCardNumber());
//            stmt.setString(7, subscriber.getSubscriberNumber());
//            stmt.executeUpdate();
//            ServerGui.serverGui.printToConsole("Subscriber update done successfully");
//        } catch (SQLException e) {
//            e.printStackTrace();
//            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
//        }
//    }


    public void updateSubscriberNumberAndCreditCard(String id, String newSubscriberNumber,
                                                    String newCreditCardNumber, Response response) {
        /*
          this method get id of subscriber and update his subscriberNumber to 'newSubscriberNumber' in DB.
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

//    public Subscriber getSubscriberDetails(String id) {
//        /**
//         * this method get a subscriber id and return Subscriber object with all his details from the DB.
//         * NOTE: method will return null if subscriber ID isn't exists in DB.
//         */
//        //return null if Subscriber id does not exists
//        PreparedStatement stmt;
//        ResultSet rs;
//        String query = "SELECT * FROM Subscriber WHERE id = ?";
//        try {
//            stmt = conn.prepareStatement(query);
//            stmt.setString(1, id);
//            rs = stmt.executeQuery();
//            if (rs.next()) {
//                return new Subscriber(
//                        rs.getString("firstName"), rs.getString("lastName"),
//                        rs.getString("id"), rs.getString("phoneNumber"),
//                        rs.getString("emailAddress"), rs.getString("creditCardNumber"),
//                        rs.getString("subscriberNumber"));
//            }
//            rs.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
//        }
//        return null; //Subscriber id does not exists
//    }


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

//    public List<Object> getAllSubscribersFromDB(Response response) {
//        /**
//         * this method get a subscriber id and return true/false if he is exists in DB or not
//         */
//        Subscriber subscriber;
//        List<Object> subscribersList = new ArrayList<>();
//        PreparedStatement stmt;
//        ResultSet rs;
//        String query = "SELECT * FROM Subscriber";
//        try {
//            stmt = conn.prepareStatement(query);
//            rs = stmt.executeQuery();
//            while (rs.next()) {
//                subscriber = new Subscriber(
//                        rs.getString("firstName"), rs.getString("lastName"),
//                        rs.getString("id"), rs.getString("phoneNumber"),
//                        rs.getString("emailAddress"), rs.getString("creditCardNumber"),
//                        rs.getString("subscriberNumber"));
//                subscribersList.add(subscriber);
//            }
//            editResponse(response, ResponseCode.OK, "Successfully sent all subscribers", subscribersList);
//            rs.close();
//        } catch (SQLException e) {
//            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
//            e.printStackTrace();
//            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
//        }
//        return subscribersList;
//    }
    
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
	
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

    void editResponse(Response response, ResponseCode code, String description, List<Object> body) {
        response.setBody(body);
        response.setCode(code);
        response.setDescription(description);
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
                FileInputStream input = null;
                try {

                    input = new FileInputStream("src/styles/" + productName +".png");
                } catch (Exception e) {
                    input = new FileInputStream("src/styles/defultProductImage.png");
                }

                BufferedInputStream buffer = new BufferedInputStream(input);
                DataInputStream image = new DataInputStream(buffer);
                //byte[] imageBytes = buffer.readAllBytes();
                byte[] imageBytes = IOUtils.readAllBytes(buffer);


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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
            stmt.setInt(1, Integer.valueOf(machineId));
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
                System.out.println("XXX");
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

    /**
     * function that update in inventory in DB after a new order.  edit the response accordingly.
     * @param response - Response object for the user
     * @param updatedInventory - list of ProductInMachine objects
     */
    public void updateInventoryInDB(Response response, List<Object> updatedInventory) {
        ProductInMachine productInMachineCasted;
        for(Object productInMachine : updatedInventory) {
            productInMachineCasted = (ProductInMachine)productInMachine;
            PreparedStatement stmt;
            String query = "UPDATE ProductInMachine SET amountInMachine= ?, statusInMachine= ? WHERE productId = ? AND machineId = ?";
            try {
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, productInMachineCasted.getAmount());
                stmt.setString(2, productInMachineCasted.getStatusInMachine().toString());
                stmt.setString(3, productInMachineCasted.getProductId());
                stmt.setString(4, productInMachineCasted.getMachineId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
                e.printStackTrace();
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            }
        }
        ServerGui.serverGui.printToConsole("Subscriber update done successfully");
        editResponse(response, ResponseCode.OK, "Successfully updated subscriber credentials", null);
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
        String query = "SELECT * FROM orders WHERE customerId = ?";
        Boolean idExist = false;

        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer id = rs.getInt("customerId");
                if(id.equals(customerId))
                {
                    idExist = true;
                }

            }
            OrderedIds.add(idExist);
            editResponse(response, ResponseCode.OK, "Successfully import all products from machine",OrderedIds);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
                editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
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
        	stmt.setString(1, LocalDate.now().toString());
        	stmt.setString(2, orderId);
        	stmt.setString(3, pickupCode);
    		stmt.executeUpdate();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
	}
	
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }
    
	
	
}


