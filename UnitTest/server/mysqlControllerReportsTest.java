package server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;

import models.Regions;
import models.ReportType;
import models.SavedReportRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gui.IServerGui;
import models.Response;
import models.ResponseCode;
import serverModels.ServerConf;

class mysqlControllerReportsTest {
	private static final ReportType REPORT_TYPE = ReportType.INVENTORY;
	private static final Regions REGION = Regions.North;
	private static Connection connMock;
	private static PreparedStatement stmtMock;

	private mysqlController mySql;
	private ServerConf conf;
	private Response res;
	private IReportsSql reportsSqlMock;
	
    private final static String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
    private final static String REPORT_FOUND_MSG = "Report data was fetched successfully";
    private final static String REPORT_NOT_FOUND_MSG = "Couldn't find report for the specified parameters";
    private final static String REPORT_ERROR_MSG = "There was an error while trying to fetch report data";
    private final static String ERROR_IN_CHECKING_MSG = "Failed while checking if reports are exists";
    private final static String GET_QUERY = "SELECT * FROM saved_reports WHERE year_creation = ? "
    		+ "AND month_creation = ? AND report_type = ? AND region = ? AND machineId = ?";
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

	@SuppressWarnings("unchecked")
	private void setMockQueryAsError() throws SQLException {
		mySql.conn = connMock;
		when(connMock.prepareStatement(anyString())).thenReturn(stmtMock);
		when(stmtMock.executeQuery()).thenThrow(SQLException.class);
	}
   
	@BeforeEach
	void setUp() throws Exception {
		connMock = mock(Connection.class);
		stmtMock = mock(PreparedStatement.class);
		reportsSqlMock = mock(IReportsSql.class);

		//Opening a mysql connection:
		serverGuiDummy = new ServerGuiServiceTest();
		conf = Server.getDefaultServerConf();
		mySql = new mysqlController(conf, serverGuiDummy, reportsSqlMock);
		res = new Response();
		mysqlController.disconnectServer(res);
		msgToConsole = null;
		isConnectedTest = true;
		isErrorTest = false;
	}
	// Functionality: successiding in getting a report of a specific Request
	// input data: a request for a report (YEAR 2022, MONTH 12, 
	// TYPE ReportType.INVENTORY, REGION Regions.North, 2)
	// expected result: the Response isn't null and its code and description are valid
	@Test
	void getReportSuccessfully() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNotNull(res.getBody().get(0));
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), REPORT_FOUND_MSG);
	}
	
	// Functionality: failing in getting a report of a specific Request
	// input data: a request for a report (YEAR 1970, MONTH 1, 
	// TYPE ReportType.INVENTORY, REGION Regions.North, 1)
	// expected result: fail - we check in DB and wont find report with that date -> respons is INVALID_DATA 
	@Test
	void getReportNotFoundInvalidYear() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 12, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}


	@Test
	void getReportNotFoundInvalidMonth() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 13, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	@Test
	void getReportNotFoundInvalidType() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, null, REGION, 2);

		try {
			mySql.getReport(res, expectedRequestBody);
			fail();
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}

	@Test
	void getReportNotFoundInvalidRegion() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, null, 2);

		try {
			mySql.getReport(res, expectedRequestBody);
			fail();
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}

	@Test
	void getReportNotFoundRegionAll() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, Regions.All, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	@Test
	void getReportNotFoundInvalidMachine() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 99999);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	
	// Functionality: failing in getting a report of a specific Request, when we'll look
	// in DB with our mock we will throw an SQLException to get another failed response
	// input data: a request for a report (YEAR 1970, MONTH 1, 
	// TYPE ReportType.INVENTORY, REGION Regions.North, 1)
	// expected result: after using our mock we thorw SQLException, thats edit our response
	// to hold ResponseCode.DB_ERROR, REPORT_ERROR_MSG and null
	@SuppressWarnings({ "unchecked", "static-access" })
	@Test
	void getReportThrowsSqlException() throws SQLException {
		setMockQueryAsError();
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 1, REPORT_TYPE, REGION, 1);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), REPORT_ERROR_MSG);
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
		assertTrue(isErrorTest);
	}

	@Test
	void generateAllReportsCheckIsCreatedError() throws SQLException {
		setMockQueryAsError();

		mySql.generateAllReports(res);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_IN_CHECKING_MSG);
	}

//	public void generateAllReports(Response response) {
//		// Checking if reports are already exists
//		Boolean isReportsAlreadyCreated = checkIfReportsAreAlreadyCreated();
//		if (isReportsAlreadyCreated == null) {
//			editResponse(response, ResponseCode.DB_ERROR, "Failed while checking if reports are exists", null);
//			return;
//		}
}
	
