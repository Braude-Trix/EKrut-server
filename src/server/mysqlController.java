package server;

import gui.ServerGui;
import models.Response;
import models.ResponseCode;
import models.Subscriber;
import serverModels.ServerConf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.*;
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
    public void saveSubscriberToDB(Subscriber subscriber) {
        PreparedStatement stmt;
        ServerGui.serverGui.printToConsole("Inserting Subscriber to DB");
        String query = "INSERT into Subscriber VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, subscriber.getFirstName());
            stmt.setString(2, subscriber.getLastName());
            stmt.setString(3, subscriber.getId());
            stmt.setString(4, subscriber.getPhoneNumber());
            stmt.setString(5, subscriber.getEmailAddress());
            stmt.setString(6, subscriber.getCreditCardNumber());
            stmt.setString(7, subscriber.getSubscriberNumber());
            stmt.executeUpdate();
            ServerGui.serverGui.printToConsole("Subscriber update done successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


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

    public Subscriber getSubscriberDetails(String id) {
        /**
         * this method get a subscriber id and return Subscriber object with all his details from the DB.
         * NOTE: method will return null if subscriber ID isn't exists in DB.
         */
        //return null if Subscriber id does not exists
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM Subscriber WHERE id = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new Subscriber(
                        rs.getString("firstName"), rs.getString("lastName"),
                        rs.getString("id"), rs.getString("phoneNumber"),
                        rs.getString("emailAddress"), rs.getString("creditCardNumber"),
                        rs.getString("subscriberNumber"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return null; //Subscriber id does not exists
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

    public List<Object> getAllSubscribersFromDB(Response response) {
        /**
         * this method get a subscriber id and return true/false if he is exists in DB or not
         */
        Subscriber subscriber;
        List<Object> subscribersList = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM Subscriber";
        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                subscriber = new Subscriber(
                        rs.getString("firstName"), rs.getString("lastName"),
                        rs.getString("id"), rs.getString("phoneNumber"),
                        rs.getString("emailAddress"), rs.getString("creditCardNumber"),
                        rs.getString("subscriberNumber"));
                subscribersList.add(subscriber);
            }
            editResponse(response, ResponseCode.OK, "Successfully sent all subscribers", subscribersList);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        return subscribersList;
    }

    void editResponse(Response response, ResponseCode code, String description, List<Object> body) {
        response.setBody(body);
        response.setCode(code);
        response.setDescription(description);
    }


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
                product = new Product(
                        rs.getString("productName"), rs.getString("productId"),
                        rs.getString("information"),
                        rs.getDouble("price"));
                products.add(product);
            }
            editResponse(response, ResponseCode.OK, "Successfully import all products", products);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    public void getAllProductsInMachine(String machineId, Response response) {
        ProductInMachine productInMachine;
        List<Object> ProductsInMachine = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM ProductInMachine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, machineId);
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
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    public void saveOrderToDB(Order order, Response response) {
        PreparedStatement stmt;
        String query = "INSERT into orders (pickUpMethod, orderDate, price, machineId, orderStatus) VALUES (?, ?, ?, ?, ?)";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, order.getPickUpMethod().toString());
            stmt.setString(2, order.getDate());
            stmt.setDouble(3, order.getPrice());
            stmt.setString(4, order.getMachineId());
            stmt.setString(5, order.getStatus());
            stmt.executeUpdate();
            editResponse(response, ResponseCode.OK, "Successfully save order", null);
            ServerGui.serverGui.printToConsole("Subscriber saved successfully");
        } catch (SQLException e) {
            System.out.println("XXX");
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }


    public void getMyMessages(String customerId, Response response) {
        List<Object> messages = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM messages WHERE to_customerId = ? AND readed = 0";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, customerId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(rs.getString("message_content"));
                messages.add(rs.getInt("from_Id"));
            }
            editResponse(response, ResponseCode.OK, "Successfully query messages", null);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    public void getAllDeliveriesOrdersByRegion(Response response, String region) {
        DeliveryOrder deliveryOrder;
        List<Object> deliveriesOrders = new ArrayList<>();
        PreparedStatement stmt;
        ResultSet rs;
        String query = "SELECT * FROM deliveries WHERE region = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, region);
            rs = stmt.executeQuery();
            while (rs.next()) {
                deliveryOrder = new DeliveryOrder(
                        rs.getString("orderId"), rs.getString("deliveryAddress"),
                        rs.getString("region"),
                        rs.getString("deliveryDate"));
                deliveriesOrders.add(deliveryOrder);
            }
            editResponse(response, ResponseCode.OK, "Successfully get all deliveries orders", deliveriesOrders);
            rs.close();
        } catch (SQLException e) {
            editResponse(response, ResponseCode.DB_ERROR, "Error (FIX ACCORDING TO SPECIFIC EXCEPTION", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

}


