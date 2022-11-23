import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;


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
//		    Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/test/flights?serverTimezone=IST","root","Aa123456");
        conn = DriverManager.getConnection("jdbc:mysql://localhost/test?serverTimezone=IST","root","1234");
        System.out.println("SQL connection succeed");
        printUsers(conn);
//	        conn.close();
	  } catch (SQLException ex) {/* handle any errors*/
	    System.out.println("SQLException: " + ex.getMessage());
	    System.out.println("SQLState: " + ex.getSQLState());
	    System.out.println("VendorError: " + ex.getErrorCode());
	    } 
	  }
	  
	public void saveUserToDB(Map<String, String> userData) {
		System.out.println("Inserting User to DB");
		String query = "INSERT into users VALUES (?, ?, ?, ?)";
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(query);
			stmt.setString(1, userData.get("UserName"));
			stmt.setString(2, userData.get("ID"));
			stmt.setString(3, userData.get("Department"));
			stmt.setString(4, userData.get("Tel."));
			stmt.executeUpdate();
			System.out.println("User update done sucssesfuly");
		} catch (SQLException e) {
			e.printStackTrace();
		} 		
	}
	  
  public static void printUsers(Connection con) {
  	Statement stmt;
  	try 
  	{
  		stmt = con.createStatement();
  		ResultSet rs = stmt.executeQuery("SELECT * FROM users;");
   		while(rs.next())
   		{
  			 // Print out the values
  			 System.out.println(rs.getString(1) + "  " + rs.getString(2));
  		} 
  		rs.close();
  		//stmt.executeUpdate("UPDATE course SET semestr=\"W08\" WHERE num=61309");
  	} catch (SQLException e) {e.printStackTrace();}
  }
 }


