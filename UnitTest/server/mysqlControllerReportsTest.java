package server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;

import models.Regions;
import models.ReportType;
import models.SavedReportRequest;
import ocsf.server.ConnectionToClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Any;

import gui.IServerGui;
import gui.ServerGUIController;
import gui.ServerGui;
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
	
    private final static String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
    private final static String REPORT_FOUND_MSG = "Report data was fetched successfully";
    private final static String REPORT_NOT_FOUND_MSG = "Couldn't find report for the specified parameters";
    private final static String REPORT_ERROR_MSG = "There was an error while trying to fetch report data";
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
    
	@BeforeAll
	static void beforeAll() throws Exception {
		connMock = mock(Connection.class);
		stmtMock = mock(PreparedStatement.class);
	}
   
	@BeforeEach
	void setUp() throws Exception {
		//Opening a mysql connection:
		serverGuiDummy = new ServerGuiServiceTest();
		conf = Server.getDefaultServerConf();
		mySql = new mysqlController(conf, serverGuiDummy);
		res = new Response();
		mysqlController.disconnectServer(res);
		msgToConsole = null;
		isConnectedTest = true;
		isErrorTest = false;
	}

	@Test
	void getReportSuccessfully() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNotNull(res.getBody().get(0));
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), REPORT_FOUND_MSG);
	}

	@Test
	void getReportNotFound() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 1, REPORT_TYPE, REGION, 1);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	@Test
	void getReportThrowsSqlException() throws SQLException {
		mySql.conn = connMock;
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 1, REPORT_TYPE, REGION, 1);
		when(connMock.prepareStatement(GET_QUERY)).thenReturn(stmtMock);
		when(stmtMock.executeQuery()).thenThrow(SQLException.class);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), REPORT_ERROR_MSG);
		assertEquals(msgToConsole, EXECUTE_UPDATE_ERROR_MSG);
		assertTrue(isErrorTest);
	}
}
	
