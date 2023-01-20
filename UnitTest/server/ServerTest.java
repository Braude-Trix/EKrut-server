package server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import models.Method;
import models.Request;

import models.User;
import serverModels.ServerConf;

class ServerTest {
	private Server server;
	private ServerConf conf;
	private mysqlController mysqlMockito;
	private User user;

	
	
	@BeforeEach
	void setUp() throws Exception {
		conf = Server.getDefaultServerConf();
		server = new Server(conf.getPort());
		mysqlMockito = Mockito.mock(mysqlController.class);
		server.mysqlController = mysqlMockito;
		user = new User("Din", "Til", 111111112, "Din@gmail.com", "0528888887", "customer2", "1234" , false, "2222222222222223");

	}
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getUser, Method GET, Body customer2 details)
	// expected result:Successfully reached to the correct method.
	@Test
	void getUserServerPath_Success() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList("Customer2","1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(1)).getAndLoginUserFromDB(any(), any(), any());
	}
	

	
	// Functionality: Failure when calling a nonexistence request.
	// input data:Request req(Path /login/getUser, Method PUT, Body customer2 details)
	// expected result:Failure - no such request.(Expected to not enter the method)
	@Test
	void getUserServerPath_methodFail() {
		Request req = getRequest("/login/getUser",Method.PUT, new ArrayList<>(Arrays.asList("Customer2","1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getUser, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNullFail() {
		Request req = getRequest("/login/getUser",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains only 1 items, expecting 2)
	// input data:Request req(Path /login/getUser, Method GET, Body Customer2 username)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNotEnoughSizeFail() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList("Customer2")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (expecting string username and got int)
	// input data:Request req(Path /login/getUser, Method GET, Body int username and string password)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNotStringInFirstObjectFail() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList(1,"1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (expecting string password and got int)
	// input data:Request req(Path /login/getUser, Method GET, Body string username and int password)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNotStringInSecondObjectFail() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList("Customer2",1)));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body user)
	// expected result:Successfully reached to the correct method.
	@Test
	void getCustomerServerPath_ForEk_Success() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(1)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a nonexistence getUserForEkConfiguration request
	// input data:Request req(Path /login/getUserForEkConfiguration, Method PUT, Body user)
	// expected result:Failure - no such request.(Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_methodFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.PUT, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_bodyNullFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (not a user)
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body String "1234")
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_bodyNotUserFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, new ArrayList<>(Arrays.asList("1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains not correct amount of objects)
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body user,String "1234" )
	// expected result:Failure - no such request (expected different body). (Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_bodyBiggerSizeFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user,"1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body user)
	// expected result:Successfully reached to the correct method.
	@Test
	void getCustomerOrWorkerServerPath_ForOL_Success() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(1)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when entering the incorrect method with a request
	// input data:Request req(Path /login/getUserForOLConfiguration, Method PUT, Body user)
	// expected result:Failure -no such request (expected only Get). (Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_methodFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.PUT, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_bodyNullFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (not a user)
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body String "1234")
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_bodyNotUserFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, new ArrayList<>(Arrays.asList("1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains not correct amount of objects)
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body user,String "1234" )
	// expected result:Failure - no such request (expected different body). (Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_bodyBiggerSizeFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user,"1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getAllSubscriberForFastLogin, Method GET, Body null)
	// expected result:Successfully reached to the correct method.
	@Test
	void getAllSubscribersForFastLoginServerPath_ForOL_Success() {
		Request req = getRequest("/login/getAllSubscriberForFastLogin",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMockito, times(1)).getSubscribersForFastLogin(any());
	}
	
	@Test
	void getAllSubscribersForFastLoginServerPath_ForOL_methodFail() {
		Request req = getRequest("/login/getAllSubscriberForFastLogin",Method.PUT, null);
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getSubscribersForFastLogin(any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path //login/getCustomerById, Method GET, Body users id)
	// expected result:Successfully reached to the correct method.
	@Test
	void getCustomerById_ForEKFastLogin_Success() {
		Request req = getRequest("/login/getCustomerById",Method.GET, new ArrayList<>(Arrays.asList(user.getId())));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(1)).getCustomerById(any(), any());
	}
	
	@Test
	void getCustomerById_ForEKFastLogin_methodFail() {
		Request req = getRequest("/login/getCustomerById",Method.PUT, new ArrayList<>(Arrays.asList(user.getId())));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomerById(any(), any());
	}
	
	
	@Test
	void getCustomerById_ForEKFastLogin_bodyNullFail() {
		Request req = getRequest("/login/getCustomerById",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomerById(any(), any());
	}
	
	@Test
	void getCustomerById_ForEKFastLogin_bodyNotIntegerFail() {
		Request req = getRequest("/login/getCustomerById",Method.GET, new ArrayList<>(Arrays.asList("1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomerById(any(), any());
	}
	
	@Test
	void getCustomerById_ForEKFastLogin_bodyBiggerSizeFail() {
		Request req = getRequest("/login/getCustomerById",Method.GET, new ArrayList<>(Arrays.asList(user.getId(),"1234")));
		server.parseClientRequest(req);
		verify(mysqlMockito, times(0)).getCustomerById(any(), any());
	}
	
	
	///////////////////////////////////helpful Method////////////////////////////////
	private Request getRequest(String path, Method method, List<Object> body) {
		Request req = new Request();
		req.setBody(body);
		req.setMethod(method);
		req.setPath(path);
		return req;
	}

}
