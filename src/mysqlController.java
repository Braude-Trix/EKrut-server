import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class mysqlController {
	public Connection conn;

	public void connectToDB() {
	  try 
	  {
	    Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
	    System.out.println("Driver definition succeed");
	    } catch (Exception ex) {
	    	/* handle the error*/
	   System.out.println("Driver definition failed");
	   }
	    
	    try 
	    {
        conn = DriverManager.getConnection("jdbc:mysql://localhost/world?serverTimezone=IST","root","1234");
        System.out.println("SQL connection succeed");
        //printUsers(conn);
//	        conn.close();
	  } catch (SQLException ex) {/* handle any errors*/
	    System.out.println("SQLException: " + ex.getMessage());
	    System.out.println("SQLState: " + ex.getSQLState());
	    System.out.println("VendorError: " + ex.getErrorCode());
	    } 
	  }

	  public void saveSubscriberToDB(Subscriber subscriber) {
		  /**
		   * this method get an Subscriber object and save his details to DB.
		   * NOTE: Existing ID Subscriber OR null value except from subscriberNumber will raise an exception!
		   */
		PreparedStatement stmt;
		System.out.println("Inserting Subscriber to DB");
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
			System.out.println("Subscriber update done sucssesfuly");
		} catch (SQLException e) {
			e.printStackTrace();
		} 		
	}
	
	public void updateSubscriberCreditCardNumber(String id, String newCreditCardNumber) {
		/**
		 * this method get id of subscriber and update his creditCardNumber to 'newCreditCardNumber' in DB.
		 */
		PreparedStatement stmt;
		String query = "UPDATE Subscriber SET creditCardNumber= ? WHERE id= ?";
		try {
			stmt = conn.prepareStatement(query);
			stmt.setString(1, newCreditCardNumber);
			stmt.setString(2, id);
			stmt.executeUpdate();
			System.out.println("Subscriber update done sucssesfuly");
		} catch (SQLException e) {
			e.printStackTrace();
		} 		
	}
	
	public void updateSubscriberNumber(String id, String newSubscriberNumber) {
		/**
		 * this method get id of subscriber and update his subscriberNumber to 'newSubscriberNumber' in DB.
		 */
		PreparedStatement stmt;
		String query = "UPDATE Subscriber SET SubscriberNumber= ? WHERE id= ?";
		try {
			stmt = conn.prepareStatement(query);
			stmt.setString(1, newSubscriberNumber);
			stmt.setString(2, id);
			stmt.executeUpdate();
			System.out.println("Subscriber update done sucssesfuly");
		} catch (SQLException e) {
			e.printStackTrace();
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
	  	try 
	  	{
			stmt = conn.prepareStatement(query);
			stmt.setString(1, id);
			rs = stmt.executeQuery();
	   		if(rs.next()) {
	   			return new Subscriber(rs.getString("firstName"),rs.getString("lastName"),rs.getString("id")
	   					,rs.getString("phoneNumber"),rs.getString("emailAddress"), rs.getString("creditCardNumber"), rs.getString("subscriberNumber"));
	   		}
	  		rs.close();
	  	} catch (SQLException e) {e.printStackTrace();}
		return null; //Subscriber id does not exists
	}


	public boolean isSubscriberExistInDB(String id) {
		/**
		 * this method get a subscriber id and return true/false if he is exists in DB or not
		 */
		PreparedStatement stmt;
		ResultSet rs;
		String query = "SELECT * FROM Subscriber WHERE id = ?";
	  	try 
	  	{
			stmt = conn.prepareStatement(query);
			stmt.setString(1, id);
			rs = stmt.executeQuery();
	   		if(rs.next()) {
	   			return true;
	   		}
	  		rs.close();
	  	} catch (SQLException e) {e.printStackTrace();}
		return false; 
	}
}
