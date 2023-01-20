package server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import gui.ServerGUIController;
import gui.ServerGui;
import models.Method;
import models.Regions;
import models.ReportType;
import models.Request;
import models.Response;
import models.ResponseCode;
import models.SavedReportRequest;
import ocsf.server.ConnectionToClient;


import models.User;
import serverModels.ServerConf;

class ServerTest {
	private static Server server;
	private static ServerConf conf;
	private static mysqlController mysqlMock;
	private static final User user = new User("Din", "Til", 111111112, "Din@gmail.com", "0528888887", "customer2", "1234" , false, "2222222222222223");
	private static final ReportType REPORT_TYPE = ReportType.INVENTORY;
	private static final Regions REGION = Regions.North;
	static ConnectionToClient connectionToClientMock;

	@BeforeAll
	static void beforeAll() throws Exception {
		conf = Server.getDefaultServerConf();
	}
	
	@BeforeEach
	void setUp() throws Exception {
		mysqlMock = mock(mysqlController.class);
		connectionToClientMock = mock(ConnectionToClient.class);
		ServerGui.serverGui = mock(ServerGUIController.class);
		server = new Server(conf.getPort());
		server.mysqlController = mysqlMock;
	}

	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getUser, Method GET, Body customer2 details)
	// expected result:Successfully reached to the correct method.
	@Test
	void getUserServerPath_Success() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList("Customer2","1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(1)).getAndLoginUserFromDB(any(), any(), any());
	}
	

	
	// Functionality: Failure when calling a nonexistence request.
	// input data:Request req(Path /login/getUser, Method PUT, Body customer2 details)
	// expected result:Failure - no such request.(Expected to not enter the method)
	@Test
	void getUserServerPath_methodFail() {
		Request req = getRequest("/login/getUser",Method.PUT, new ArrayList<>(Arrays.asList("Customer2","1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getUser, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNullFail() {
		Request req = getRequest("/login/getUser",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains only 1 items, expecting 2)
	// input data:Request req(Path /login/getUser, Method GET, Body Customer2 username)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNotEnoughSizeFail() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList("Customer2")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (expecting string username and got int)
	// input data:Request req(Path /login/getUser, Method GET, Body int username and string password)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNotStringInFirstObjectFail() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList(1,"1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (expecting string password and got int)
	// input data:Request req(Path /login/getUser, Method GET, Body string username and int password)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getUserServerPath_bodyNotStringInSecondObjectFail() {
		Request req = getRequest("/login/getUser",Method.GET, new ArrayList<>(Arrays.asList("Customer2",1)));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getAndLoginUserFromDB(any(), any(), any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body user)
	// expected result:Successfully reached to the correct method.
	@Test
	void getCustomerServerPath_ForEk_Success() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMock, times(1)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a nonexistence getUserForEkConfiguration request
	// input data:Request req(Path /login/getUserForEkConfiguration, Method PUT, Body user)
	// expected result:Failure - no such request.(Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_methodFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.PUT, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_bodyNullFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (not a user)
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body String "1234")
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_bodyNotUserFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, new ArrayList<>(Arrays.asList("1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains not correct amount of objects)
	// input data:Request req(Path /login/getUserForEkConfiguration, Method GET, Body user,String "1234" )
	// expected result:Failure - no such request (expected different body). (Expected to not enter the method)
	@Test
	void getCustomerServerPath_ForEk_bodyBiggerSizeFail() {
		Request req = getRequest("/login/getUserForEkConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user,"1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomer(any(), any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body user)
	// expected result:Successfully reached to the correct method.
	@Test
	void getCustomerOrWorkerServerPath_ForOL_Success() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMock, times(1)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when entering the incorrect method with a request
	// input data:Request req(Path /login/getUserForOLConfiguration, Method PUT, Body user)
	// expected result:Failure -no such request (expected only Get). (Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_methodFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.PUT, new ArrayList<>(Arrays.asList(user)));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_bodyNullFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (not a user)
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body String "1234")
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_bodyNotUserFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, new ArrayList<>(Arrays.asList("1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains not correct amount of objects)
	// input data:Request req(Path /login/getUserForOLConfiguration, Method GET, Body user,String "1234" )
	// expected result:Failure - no such request (expected different body). (Expected to not enter the method)
	@Test
	void getCustomerOrWorkerServerPath_ForOL_bodyBiggerSizeFail() {
		Request req = getRequest("/login/getUserForOLConfiguration",Method.GET, new ArrayList<>(Arrays.asList(user,"1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getUserForOL(any(), any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path /login/getAllSubscriberForFastLogin, Method GET, Body null)
	// expected result:Successfully reached to the correct method.
	@Test
	void getAllSubscribersForFastLoginServerPath_ForOL_Success() {
		Request req = getRequest("/login/getAllSubscriberForFastLogin",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMock, times(1)).getSubscribersForFastLogin(any());
	}
	
	// Functionality: Failure when entering the incorrect method with a request
	// input data:Request req(Path /login/getAllSubscriberForFastLogin, Method PUT, Body user)
	// expected result:Failure -no such request (expected only Get). (Expected to not enter the method)
	@Test
	void getAllSubscribersForFastLoginServerPath_ForOL_methodFail() {
		Request req = getRequest("/login/getAllSubscriberForFastLogin",Method.PUT, null);
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getSubscribersForFastLogin(any());
	}
	
	// Functionality: Successfully entering the correct method with the correct request
	// input data:Request req(Path //login/getCustomerById, Method GET, Body users id)
	// expected result:Successfully reached to the correct method.
	@Test
	void getCustomerById_ForEKFastLogin_Success() {
		Request req = getRequest("/login/getCustomerById",Method.GET, new ArrayList<>(Arrays.asList(user.getId())));
		server.parseClientRequest(req);
		verify(mysqlMock, times(1)).getCustomerById(any(), any());
	}
	
	// Functionality: Failure when entering the incorrect method with a request
	// input data:Request req(Path /login/getCustomerById, Method PUT, Body user)
	// expected result:Failure -no such request (expected only Get). (Expected to not enter the method)
	@Test
	void getCustomerById_ForEKFastLogin_methodFail() {
		Request req = getRequest("/login/getCustomerById",Method.PUT, new ArrayList<>(Arrays.asList(user.getId())));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomerById(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (null)
	// input data:Request req(Path /login/getCustomerById, Method GET, Body null)
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerById_ForEKFastLogin_bodyNullFail() {
		Request req = getRequest("/login/getCustomerById",Method.GET, null);
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomerById(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (not a Integer)
	// input data:Request req(Path /login/getCustomerById, Method GET, Body String "1234")
	// expected result:Failure - no such request (expected different body).(Expected to not enter the method)
	@Test
	void getCustomerById_ForEKFastLogin_bodyNotIntegerFail() {
		Request req = getRequest("/login/getCustomerById",Method.GET, new ArrayList<>(Arrays.asList("1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomerById(any(), any());
	}
	
	// Functionality: Failure when calling a getUser request with bad body (contains not correct amount of objects)
	// input data:Request req(Path /login/getCustomerById, Method GET, Body user id,String "1234" )
	// expected result:Failure - no such request (expected different body). (Expected to not enter the method)
	@Test
	void getCustomerById_ForEKFastLogin_bodyBiggerSizeFail() {
		Request req = getRequest("/login/getCustomerById",Method.GET, new ArrayList<>(Arrays.asList(user.getId(),"1234")));
		server.parseClientRequest(req);
		verify(mysqlMock, times(0)).getCustomerById(any(), any());
	}
	
	@Test
	void getReportsRequestSuccessfully() {
        SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 123);
    	Request expectedRequest = getReportRequest(expectedRequestBody);

    	server.handleMessageFromClient(expectedRequest, connectionToClientMock);

		verify(mysqlMock).getReport(new Response(), expectedRequestBody);
		try {
			verify(connectionToClientMock).sendToClient(new Response());
		} catch (IOException e) {
			fail();
		}
	}

	@Test
	void getReportsRequestFailInvalidRequest() {
        SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 123);
    	Request expectedRequest = getReportRequest(expectedRequestBody);
    	expectedRequest.setMethod(Method.POST);
    	Response expectedResponse = new Response();
    	mysqlController.editResponse(expectedResponse, ResponseCode.SERVER_ERROR, "Operation doesn't exist", null);


    	server.handleMessageFromClient(expectedRequest, connectionToClientMock);

		verify(mysqlMock, times(0)).getReport(new Response(), expectedRequestBody);
		try {
			verify(connectionToClientMock).sendToClient(expectedResponse);
		} catch (IOException e) {
			fail();
		}
	}
	
	
	///////////////////////////////////helpful Method////////////////////////////////
	private Request getRequest(String path, Method method, List<Object> body) {
		Request req = new Request();
		req.setBody(body);
		req.setMethod(method);
		req.setPath(path);
		return req;
	}
	
	private Request getReportRequest(SavedReportRequest reportRequest) {
		List<Object> paramList = new ArrayList<>();
		paramList.add(reportRequest);
		return getRequest("/reports", models.Method.GET, paramList);
	}

}
