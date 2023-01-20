package server;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gui.IServerGui;
import gui.ServerGui;
import models.Customer;
import models.CustomerType;
import models.Regions;
import models.Response;
import models.ResponseCode;
import models.User;
import models.Worker;
import models.WorkerType;
import serverModels.ServerConf;

class mysqlControllerTest {
	private mysqlController mySql;
	private ServerConf conf;
	private Response res;
	private User user;
	private User user1;
	private User unregistered1;
	private Customer customer;
	private Worker worker;
	
	private User ceo_customerAndWorker;
	private Customer ceo_customerAndWorker_CustomerSide;
	private Worker ceo_customerAndWorker_WorkerSide;

	private java.lang.reflect.Method privateGetWorker;
	private java.lang.reflect.Method privateIsLoggedIn;

	
    private final static String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
	private boolean isErrorTest;
	private String msgToConsole;
	private boolean isConnectedTest;
	private ServerGuiServiceTest serverGuiDummy;
	
    public class ServerGuiServiceTest implements IServerGui{
		@Override
		public void setPrintToConsole(String msg, boolean isError) {
			isErrorTest = isError;
			msgToConsole  = msg;
		}

		@Override
		public void setPrintToConsole(String msg) {
			msgToConsole = msg;
		}
		

		@Override
		public void setConnected(boolean isConnected) {
			isConnectedTest = isConnected;
		}
    }
   
	@BeforeEach
	void setUp() throws Exception {
		//Opening a mysql connection:
		serverGuiDummy = new ServerGuiServiceTest();
		conf = Server.getDefaultServerConf();
		mySql = new mysqlController(conf,serverGuiDummy);
		res = new Response();
		mysqlController.disconnectServer(res);
		msgToConsole = null;
		isConnectedTest = true;
		isErrorTest = false;
		
		//mocking Subscriber data:
		user = new User("Din", "Til", 111111112, "Din@gmail.com", "0528888887", "customer2", "1234" , false, "2222222222222223");
		customer = new Customer(user, CustomerType.Subscriber, "1", 60);
		//mocking Worker data:
		user1 = new User("Tali", "Manachem", 311111118, "Tali@gmail.com", "0528488884", "marWorkerSouth", "1234" , false, "5222222222222225");
		worker = new Worker(user1,WorkerType.MarketingWorker,Regions.South);
		//mocking User data: (not a customer nor worker)
		unregistered1 = new User("Shoshana", "Pick", 222222221, "Shoshana@gmail.com", "0521188883", "unregistered1", "1234" , false, "2222222322222226");
		
		//mocking Customer&Worker data:
		ceo_customerAndWorker = new User("Shaked", "Mizrahi", 411111111, "Shaked@gmail.com", "0527888665", "ceo", "1234" , false, "3111122222222225");
		ceo_customerAndWorker_CustomerSide = new Customer(ceo_customerAndWorker, CustomerType.Subscriber, "4", 505);
		ceo_customerAndWorker_WorkerSide = new Worker(ceo_customerAndWorker,WorkerType.CEO,null);
		
		//allowing access to private method.
		privateGetWorker = mysqlController.class.getDeclaredMethod("getWorker",Response.class, User.class);
		privateGetWorker.setAccessible(true);
		
		//allowing access to private method.
		privateIsLoggedIn = mysqlController.class.getDeclaredMethod("isLoggedIn", User.class);
		privateIsLoggedIn.setAccessible(true);
	}

	
	////////////////////////////////getUserTest & change logged in/////////////////////////////////

	// Functionality: successfully getting correct user with "getAndLoginUserFromDB" method.
	// input data:String username(customer2), String password(1234), mysqlController (mySql), Response(res), User user(Mr. "Din Til" Info), String expectedDescription.
	// expected result:correct user achieved. response changed correctly: (response code = OK, response Description = successfully got user msg).
	@Test
	void getUser_ValidUser() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String username="customer2";
		String password="1234";
		String expectedDescription = "Successfully got user details";
		mySql.getAndLoginUserFromDB(res, username, password);
		assertEquals(ResponseCode.OK, res.getCode());
		compareBetweenTwoUsers(user, (User)res.getBody().get(0));
		assertEquals(1, res.getBody().size());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, user);
		assertTrue(actualIsLoggedIn);
	}
	
	
	// Functionality: invoking "getAndLoginUserFromDB" method with invalid login details.
	// input data:String username(Toly), String password(Ro), mysqlController (mySql), Response(res),String expectedDescription.
	// expected result:Failure - No such user, response changed correctly: (response code = INVALID_DATA, response body = empty(null), response Description = incorrect password msg).
	@Test
	void getUser_InvalidUser() {
		String username="Toly";
		String password="Ro";
		String expectedDescription ="The username or password are incorrect";
		mySql.getAndLoginUserFromDB(res, username, password);
		assertEquals(ResponseCode.INVALID_DATA, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
	}

	// Functionality: invoking "getAndLoginUserFromDB" method with an already logged in user details.
	// input data:String username(customer2), String password(1234), mysqlController (mySql), Response(res),String expectedDescription.
	// expected result:Failure - Can't login (getAndLoginUserFromDB), response changed correctly: (response code = INVALID_DATA, response body = empty(null), response Description = user already logged in msg).
	@Test
	void getUser_isALreadyLoggedIn() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String username="customer2";
		String password="1234";
		String expectedDescription ="The user is already logged in";

		mySql.getAndLoginUserFromDB(res, username, password);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, user);
		assertTrue(actualIsLoggedIn);
		mySql.getAndLoginUserFromDB(res, username, password);
		assertEquals(ResponseCode.INVALID_DATA, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
	}
	
	
	// Functionality: invoking "getAndLoginUserFromDB" method without an open connection.
	// input data:String username(customer2), String password(1234), mysqlController (mySql), Response(res),String expectedDescription.
	// expected result:Failure - DB Error , response changed correctly: (response code = DB_ERROR, response body = empty(null), response Description = mysql error msg).
	@Test
	void getUser_DbFail() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String username="customer2";
		String password="1234";
		String expectedDescription =EXECUTE_UPDATE_ERROR_MSG;
		mySql.closeConnection();
		mySql.getAndLoginUserFromDB(res, username, password);
		assertEquals(ResponseCode.DB_ERROR, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
		assertEquals(isErrorTest, true);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, user);
		assertFalse(actualIsLoggedIn);
	}


	// Functionality:trying to change user's activity status (loggedIn/Not LoggedIn) without an open mysql connection, with changeLoggedInUser Method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription.
	// expected result:Failure - DB Error , response changed correctly: (response code = DB_ERROR, response body = empty(null), response Description = mysql error msg).
	@Test
	void changeLoggedInUser_DbFail() {
		String expectedDescription ="Error loading data (DB)";

		mySql.closeConnection();
		mySql.changeLoggedInUser(res, user.getId(), true);
		assertEquals(ResponseCode.DB_ERROR, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
		assertEquals(isErrorTest, true);
	}
	
	////////////////////////////////getCustomer - For EK/////////////////////////////////
	
	// Functionality:successfully getting correct Customer with "getCustomer" method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, Customer customer(Mr. "Din Til" Info).
	// expected result:Customer achieved correctly , response changed correctly: (response code = OK, response body = correct Customer(Mr. "Din Til" Info), response Description = successfully got customer msg).
	@Test
	void getCustomer_ValidCustomer() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String expectedDescription ="Registered customer successfully accepted";
		String username="customer2";
		String password="1234";
		mySql.getAndLoginUserFromDB(res, username, password);
		mySql.getCustomer(res, (User)res.getBody().get(0));
		assertEquals(ResponseCode.OK, res.getCode());
		compareBetweenTwoCustomers(customer, (Customer)res.getBody().get(0));
		assertEquals(1, res.getBody().size());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, user);
		assertTrue(actualIsLoggedIn);
	}
	
	// Functionality:Trying to get an unregistered user (not a customer) with "getCustomer" method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, User unregistered1(Mrs.Shoshana Pick).
	// expected result:No such customer , response changed correctly: (response code = INVALID_DATA, response body = null, response Description = unregistered user msg).
	@Test
	void getCustomer_Unregistered_user() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		mySql.getCustomer(res, unregistered1);
		String expectedDescription ="Unregistered user";

		assertEquals(ResponseCode.INVALID_DATA, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
	}
	
	
	// Functionality:Trying to get a customer without open mysql connection, using getCustomer method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, Customer customer(Mr. "Din Til" Info).
	// expected result:Failure - DB Error , response changed correctly: (response code = DB_ERROR, response body = null, response Description = error in reaching db msg).
	@Test
	void getCustomer_DBFail() {
		String expectedDescription ="Error loading data (DB)";

		mySql.closeConnection();
		mySql.getCustomer(res, customer);
		assertEquals(ResponseCode.DB_ERROR, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
		assertEquals(isErrorTest, true);
	}
	////////////////////////////////getUserForOL - For OL && getWorker/////////////////////////////////
	
	
	
	// Functionality:invoking "getUserForOL" method with invalid (not a customer) login details.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, User unregistered1(Mrs.Shoshana Pick).
	// expected result:Failure - no such customer , response changed correctly: (response code = INVALID_DATA, response body = null, response Description = no such customer msg).
	@Test
	void getUserForOL_invalidUser() {
		String expectedDescription ="Unregistered user";

		mySql.getUserForOL(res, unregistered1);
		assertEquals(ResponseCode.INVALID_DATA, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
	}
	
	
	// Functionality:Successfully getting customer info using "getUserForOL" method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, Customer customer(Mr. "Din Til" Info).
	// expected result:Customer achieved correctly , response changed correctly: (response code = OK, response body = correct customer info, response Description = successfully got customer msg).
	@Test
	void getUserForOL_validCustomer() {
		String expectedDescription ="Registered customer successfully accepted";
		String username="customer2";
		String password="1234";
		mySql.getAndLoginUserFromDB(res, username, password);
		mySql.getUserForOL(res, (User)res.getBody().get(0));
		assertEquals(ResponseCode.OK, res.getCode());
		compareBetweenTwoCustomers(customer, (Customer)res.getBody().get(0));
		assertEquals(1, res.getBody().size());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);

	}
	// Functionality:Successfully getting Worker info using "getUserForOL" method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, Worker worker(Mrs. "Tali Manachem" Info).
	// expected result:Worker achieved correctly , response changed correctly: (response code = OK, response body = correct Worker info, response Description = successfully got Worker msg).
	@Test
	void getUserForOL_validWorker() {
		String expectedDescription ="The employee has successfully logged in";
		String username="marWorkerSouth";
		String password="1234";
		mySql.getAndLoginUserFromDB(res, username, password);
		mySql.getUserForOL(res, (User)res.getBody().get(0));
		assertEquals(ResponseCode.OK, res.getCode());
		compareBetweenTwoWorker(worker, (Worker)res.getBody().get(0));	
		assertEquals(1, res.getBody().size());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);

	}
	
	// Functionality:Successfully getting User info who is Customer and Worker using "getUserForOL" method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, Customer/Worker ceo_customerAndWorker(Mrs. "Shaked Mizrahi" Info).
	// expected result:Customer&Worker achieved correctly , response changed correctly: (response code = OK, response body = correct ceo info, response Description = successfully got Customer&Worker msg).
	@Test
	void getUserForOL_validCustomerAndWorker() {
		String expectedDescription ="The user is both a customer and an employee";
		String username="ceo";
		String password="1234";
		mySql.getAndLoginUserFromDB(res, username, password);
		mySql.getUserForOL(res, (User)res.getBody().get(0));
		assertEquals(ResponseCode.OK, res.getCode());
		compareBetweenTwoCustomers(ceo_customerAndWorker_CustomerSide, (Customer)res.getBody().get(0));
		compareBetweenTwoWorker(ceo_customerAndWorker_WorkerSide, (Worker)res.getBody().get(1));
		assertEquals(2, res.getBody().size());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);

	}

	// Functionality:Successfully getting Worker using private method "getWorker".
	// input data: mysqlController (mySql), Response(res),String expectedDescription, User user1/Worker worker - (Mrs. "Tali Manachem" Info).
	// expected result:Worker achieved correctly , response changed correctly: (response code = OK, response body = correct Worker info, response Description = successfully got Worker msg).
	@Test
	void getWorkerSuccess() throws Exception {
		Worker workerResult;
		workerResult = (Worker) privateGetWorker.invoke(mySql,res,user1);
		compareBetweenTwoWorker(worker,workerResult);
		assertEquals(msgToConsole, null);

	}

	
	// Functionality:Failing to get Worker using private method "getWorker" without open mysql connection.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, User user1 - (Mrs. "Tali Manachem" Info).
	// expected result:Failure - DB Error , response changed correctly: (response code = DB_ERROR, response body = empty (null), response Description = error in getting db msg).
	@Test
	void getWorkerFailure() throws Exception {
		String expectedDescription ="Error loading data (DB)";
		mySql.closeConnection();
		Worker workerResult;
		workerResult = (Worker) privateGetWorker.invoke(mySql,res,user1);
		assertEquals(null, workerResult);
		assertEquals(ResponseCode.DB_ERROR, res.getCode());
		assertEquals(null, res.getBody());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
	}

	////////////////////////////////getSubscribersForFastLogin && getUserById/////////////////////////////////

	// Functionality:Successfully getting Subscribers ids list using 'getSubscribersForFastLogin' method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, 5 subscribers ids
	// expected result:Failure - DB Error , response changed correctly: (response code = OK, response body = List of subscribers ids, response Description = successfully getting subscribers ids msg).
	@Test
	void getSubscribersForFastLoginSuccess() {
		String expectedDescription ="Successfully sent all subscribers id";

		mySql.getSubscribersForFastLogin(res);
		assertEquals(ResponseCode.OK, res.getCode());
		assertEquals(5, res.getBody().size());
		assertEquals(customer.getId(), res.getBody().get(0));
		assertEquals(211111114, res.getBody().get(1));
		assertEquals(411111111, res.getBody().get(2));
		assertEquals(411111113, res.getBody().get(3));
		assertEquals(611111113, res.getBody().get(4));
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);

		
	}
	// Functionality:Failing to get Subscribers ids using 'getSubscribersForFastLogin' method without open mysql connection.
	// input data: mysqlController (mySql), Response(res),String expectedDescription.
	// expected result:Failure - DB Error , response changed correctly: (response code = DB_ERROR, response body = empty (null), response Description = Error getting db msg).
	@Test
	void getSubscribersForFastLoginFailure() {
		String expectedDescription ="Error loading data (DB)";

		mySql.closeConnection();
		mySql.getSubscribersForFastLogin(res);
		assertEquals(ResponseCode.DB_ERROR, res.getCode());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
	}
	
	
	// Functionality:Successfully getting a customer using 'getCustomerById' method.
	// input data: mysqlController (mySql), Response(res),String expectedDescription, int customer.getId() - Din Til id.
	// expected result:Correct customer achieved , response changed correctly: (response code = OK, response body = correct customer info, response Description = successfully got customer msg).
	@Test
	void getCustomerByIdSuccess() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String expectedDescription ="Registered customer successfully accepted";

		mySql.getCustomerById(res,customer.getId());
		assertEquals(ResponseCode.OK, res.getCode());
		compareBetweenTwoUsers(customer,(Customer)res.getBody().get(0));
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(1, res.getBody().size());
		assertEquals(msgToConsole, null);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, customer);
		assertTrue(actualIsLoggedIn);
	}
	
	// Functionality:Failing to get customer ids using 'getSubscribersForFastLogin' method without open mysql connection.
	// input data: mysqlController (mySql), Response(res),String expectedDescription int user.getId() - user's id.
	// expected result:Failure - DB Error , response changed correctly: (response code = DB_ERROR, response body = empty (null), response Description = Error getting db msg).
	@Test
	void getCustomerByIdFailure_DB_Exception() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String expectedDescription =EXECUTE_UPDATE_ERROR_MSG;

		mySql.closeConnection();
		mySql.getCustomerById(res,user.getId());
		assertEquals(ResponseCode.DB_ERROR, res.getCode());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, user);
		assertFalse(actualIsLoggedIn);
	}
	
	
	// Functionality:Failing to get customer ids using 'getSubscribersForFastLogin' method with nonexistent id
	// input data: mysqlController (mySql), Response(res),String expectedDescription int invalidID - nonexistent id
	// expected result:Failure - no such customer , response changed correctly: (response code = INVALID_DATA, response body = empty (null), response Description = invalid details msg).
	@Test
	void getCustomerByIdFailure_invalid_id() {
		String expectedDescription ="The username or password are incorrect";

		int invalidID=5869;
		mySql.getCustomerById(res,invalidID);
		assertEquals(ResponseCode.INVALID_DATA, res.getCode());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);

	}
	
	
	// Functionality:Failing to get customer ids using 'getSubscribersForFastLogin' method with an already logged in user.
	// input data: mysqlController (mySql), Response(res),String expectedDescription int customer.getId() - din tils id
	// expected result:Failure - user already logged in , response changed correctly: (response code = INVALID_DATA, response body = empty (null), response Description = already logged in msg).
	@Test
	void getCustomerByIdFailure_user_already_loggedIn() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String expectedDescription ="The user is already logged in";

		mySql.changeLoggedInUser(res, customer.getId(), true);
		mySql.getCustomerById(res,customer.getId());
		assertEquals(ResponseCode.INVALID_DATA, res.getCode());
		assertEquals(expectedDescription, res.getDescription());
		assertEquals(msgToConsole, null);
		boolean actualIsLoggedIn = (boolean) privateIsLoggedIn.invoke(mySql, user);
		assertTrue(actualIsLoggedIn);
	}
	

	
	
	
	////////////////////////////////helpful method/////////////////////////////////
	private void compareBetweenTwoUsers(User user1, User user2) {
		assertEquals(user1.getFirstName(), user2.getFirstName());
		assertEquals(user1.getLastName(), user2.getLastName());
		assertEquals(user1.getId(), user2.getId());
		assertEquals(user1.getPhoneNumber(), user2.getPhoneNumber());
		assertEquals(user1.getEmail(), user2.getEmail());
		assertEquals(user1.getUsername(), user2.getUsername());
		assertEquals(user1.getPassword(), user2.getPassword());
		assertEquals(user1.getCreditCardNumber(), user2.getCreditCardNumber());
		assertEquals(user1.isLoggedIn(), user2.isLoggedIn());
	}
	
	private void compareBetweenTwoCustomers(Customer customer1, Customer customer2) {
		assertEquals(customer1.getFirstName(), customer2.getFirstName());
		assertEquals(customer1.getLastName(), customer2.getLastName());
		assertEquals(customer1.getId(), customer2.getId());
		assertEquals(customer1.getPhoneNumber(), customer2.getPhoneNumber());
		assertEquals(customer1.getEmail(), customer2.getEmail());
		assertEquals(customer1.getUsername(), customer2.getUsername());
		assertEquals(customer1.getPassword(), customer2.getPassword());
		assertEquals(customer1.getCreditCardNumber(), customer2.getCreditCardNumber());
		assertEquals(customer1.isLoggedIn(), customer2.isLoggedIn());
		assertEquals(customer1.getType(), customer2.getType());
		assertEquals(customer1.getSubscriberNumber(), customer2.getSubscriberNumber());
		assertEquals(customer1.getMonthlyBill(), customer2.getMonthlyBill());
	}
	private void compareBetweenTwoWorker(Worker worker1, Worker worker2) {
		assertEquals(worker1.getFirstName(), worker2.getFirstName());
		assertEquals(worker1.getLastName(), worker2.getLastName());
		assertEquals(worker1.getId(), worker2.getId());
		assertEquals(worker1.getPhoneNumber(), worker2.getPhoneNumber());
		assertEquals(worker1.getEmail(), worker2.getEmail());
		assertEquals(worker1.getUsername(), worker2.getUsername());
		assertEquals(worker1.getPassword(), worker2.getPassword());
		assertEquals(worker1.getCreditCardNumber(), worker2.getCreditCardNumber());
		assertEquals(worker1.isLoggedIn(), worker2.isLoggedIn());
		assertEquals(worker1.getType(), worker2.getType());
		assertEquals(worker1.getRegion(),worker2.getRegion());
	
	
	}
	

}

