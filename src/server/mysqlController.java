package server;

import gui.ServerGui;
import models.MyOrders;
import models.OrderStatus;
import models.PickUpMethod;
import models.Response;
import models.ResponseCode;
import models.Subscriber;
import models.User;
import serverModels.ServerConf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mysql.cj.conf.ConnectionUrl.Type;

public class mysqlController {
    public Connection conn;
    private final String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
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
            ServerGui.serverGui.printToConsole("SQL connection succeed");
            ServerGui.serverGui.setConnected(true);
        } catch (SQLException ex) { // handle any errors
            ServerGui.serverGui.printToConsole("SQLException: " + ex.getMessage(), true);
            ServerGui.serverGui.printToConsole("SQLState: " + ex.getSQLState(), true);
            ServerGui.serverGui.printToConsole("VendorError: " + ex.getErrorCode(), true);
            ServerGui.serverGui.setConnected(false);
        }
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
    
	public List<Object> getUserFromDB(Response response, String username, String password) {

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
                userDetails.add(user);
                editResponse(response, ResponseCode.OK, "Successfully got user details", userDetails);
            }
            else {
                editResponse(response, ResponseCode.INVALID_DATA, "Error: invalid user details", null);
            }
            	
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return userDetails;
    }
	
	public List<Object> getMyOrdersFromDB(Response response, Integer customerId) {
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
            editResponse(response, ResponseCode.OK, "Successfully sent all orders of specific user", MyOrders);
            rs.close();

        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return MyOrders;
    }
	
	public List<Object> getRecivedDateDeliveryFromDB(Response response, String orderId) {
    	List<Object> RecivedDate= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT dateReceived FROM deliveryOrder WHERE OrderId = ?";
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
        return RecivedDate;
    }
	
	public List<Object> getRecivedDatePickupFromDB(Response response, String orderId) {
    	List<Object> RecivedDate= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT dateReceived FROM pickupOrder WHERE OrderId = ?";
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
        return RecivedDate;
    }
	
	public List<Object> getPickupCodeFromDB(Response response, String orderId) {
    	List<Object> pickupCode= new ArrayList<>();

        PreparedStatement stmt;
        ResultSet rs ;
        String query = "SELECT pickupCode FROM pickupOrder WHERE OrderId = ?";
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
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return pickupCode;
    }
	
	public void setStatusDeliveryOrderInDB(Response response, String orderId, OrderStatus status, String dateReceived) {
	/*
	this method get id of subscriber and update his subscriberNumber to 'newSubscriberNumber' in DB.
	*/
		PreparedStatement stmt;
		String query = "UPDATE orders SET orderStatus= ? WHERE OrderId= ?";
		try {
		stmt = conn.prepareStatement(query);
		String tempStatusString = getStringStatus(status);
		stmt.setString(1, tempStatusString);
		stmt.setString(2, orderId);
		stmt.executeUpdate();
		ServerGui.serverGui.printToConsole("Update order status - delivery successfully");
		
		query = "UPDATE deliveryOrder SET dateReceived= ? WHERE OrderId= ?";
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
    
    private String getStringStatus(OrderStatus status) {
		switch(status) {
		case NotCollected: return "NotCollected";
		case Collected: return "Collected";
		case WaitingApproveDelivery: return "WaitingApproveDelivery";
		case Done: return "Done";
	    }
		return null;
    }
}
