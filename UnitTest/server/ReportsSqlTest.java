package server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gui.IServerGui;
import models.Response;
import models.ResponseCode;
import serverModels.ProductInMachineHistory;
import serverModels.ServerConf;

class ReportsSqlTest {
	private static final int YEAR_OF_ALREADY_CREATED = 2022;
    private static final int MONTH_OF_ALREADY_CREATED = 12;

	private static final int YEAR = 2023;
    private static final int MONTH = 1;
    private static final int INVALID_MONTH = 13;    
    private static final int PRODUCT_ID = 1;
    private static final int INVALID_PRODUCT_ID = -1;
    private static final String EXPECTED_PRODUCT_NAME = "Bamba";
    private static final int MACHINE_ID = 2;
    private static final int INVALID_MACHINE_ID = -1;
    private static final String EXPECTED_MACHINE_NAME = "EM Building braude";
    private static final String EXPECTED_MACHINE_REGION = "North";

    private static final String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
    private static final String NAME_BY_PRODUCT_ID_ERROR = "There was an error while searching for name of product";
    private static final String NAME_BY_PRODUCT_ID_NOT_FOUND = "Couldn't find name of given product id";
    private static final String NAME_BY_PRODUCT_ID_SUCCS = "Successfully get name by product id";  
    
    private static final String GET_REGION_AND_NAME_ERROR = "There was an error while searching region and name of a machine id";
    private static final String GET_REGION_AND_NAME_NOT_FOUND = "Couldn't find region and name of given machine id";
    private static final String GET_REGION_AND_NAME_SUCCS = "Successfully get region by machine id";
    
	private static ITime timesMock;
	private Connection connMock;
	private PreparedStatement stmtMock;
	private IServerGui serverGuiMock;

	private mysqlController mySql;
	private ReportsSql reportsSql;
	private ServerConf conf;
	private Response res;

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
		serverGuiMock = mock(IServerGui.class);
		timesMock = mock(ITime.class);
		when(timesMock.getReportsYear()).thenReturn(YEAR);
		when(timesMock.getReportsMonth()).thenReturn(MONTH);

		//Opening a mysql connection:
		conf = Server.getDefaultServerConf();
		mySql = new mysqlController(conf, serverGuiMock);
		res = new Response();
		reportsSql = new ReportsSql(timesMock, serverGuiMock);
	}
	
	@Test
	void checkIfReportsAreAlreadyCreatedError() throws SQLException {
		setMockQueryAsError();

		Boolean actualIsAlreadyCreated = reportsSql.checkIfReportsAreAlreadyCreated();

		assertNull(actualIsAlreadyCreated);
	}
	
	@Test
	void checkIfReportsAreAlreadyCreatedIsFalse() {
		when(timesMock.getReportsMonth()).thenReturn(INVALID_MONTH);

		Boolean actualIsAlreadyCreated = reportsSql.checkIfReportsAreAlreadyCreated();

		assertFalse(actualIsAlreadyCreated);
	}

	@Test
	void checkIfReportsAreAlreadyCreatedIsTrue() {
		when(timesMock.getReportsYear()).thenReturn(YEAR_OF_ALREADY_CREATED);
		when(timesMock.getReportsMonth()).thenReturn(MONTH_OF_ALREADY_CREATED);

		Boolean actualIsAlreadyCreated = reportsSql.checkIfReportsAreAlreadyCreated();

		assertTrue(actualIsAlreadyCreated);
	}

	@Test
	void getAllProductsHistoryError() throws SQLException {
		setMockQueryAsError();

		List<ProductInMachineHistory> actualProductsHistory = reportsSql.getAllProductsHistory();

		assertNull(actualProductsHistory);
	}
	
	@Test
	void getAllProductsHistoryIsEmpty() {
		when(timesMock.getReportsMonth()).thenReturn(INVALID_MONTH);

		List<ProductInMachineHistory> actualProductsHistory = reportsSql.getAllProductsHistory();

		assertTrue(actualProductsHistory.isEmpty());
	}

	@Test
	void getAllProductsHistorySuccessfully() {
		List<ProductInMachineHistory> actualProductsHistory = reportsSql.getAllProductsHistory();

		assertEquals(177, actualProductsHistory.size());
	}

	@Test
	void getNameByProductIdError() throws SQLException {
		setMockQueryAsError();

		String actualProductName = reportsSql.getNameByProductId(res, PRODUCT_ID);

		assertNull(actualProductName);
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), NAME_BY_PRODUCT_ID_ERROR);
		verify(serverGuiMock).setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
	}
	
	@Test
	void getNameByProductIdNotFound() {
		String actualProductName = reportsSql.getNameByProductId(res, INVALID_PRODUCT_ID);

		assertNull(actualProductName);
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), NAME_BY_PRODUCT_ID_NOT_FOUND);
	}

	@Test
	void getNameByProductIdSuccessfully() {
		List<Object> expectedBody = new ArrayList<>();
		expectedBody.add(EXPECTED_PRODUCT_NAME);
		
		String actualProductName = reportsSql.getNameByProductId(res, PRODUCT_ID);

		assertEquals(EXPECTED_PRODUCT_NAME, actualProductName);
		assertEquals(expectedBody, res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NAME_BY_PRODUCT_ID_SUCCS);
	}

	@Test
	void getRegionAndNameByMachineIdError() throws SQLException {
		setMockQueryAsError();

		List<String> actualRegionAndName = reportsSql.getRegionAndNameByMachineId(res, MACHINE_ID);

		assertNull(actualRegionAndName);
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), GET_REGION_AND_NAME_ERROR);
		verify(serverGuiMock).setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
	}
	
	@Test
	void getRegionAndNameByMachineIdNotFound() {
		List<String> actualRegionAndName = reportsSql.getRegionAndNameByMachineId(res, INVALID_MACHINE_ID);

		assertNull(actualRegionAndName);
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), GET_REGION_AND_NAME_NOT_FOUND);
	}

	@Test
	void getRegionAndNameByMachineIdSuccessfully() {
		List<Object> expectedBody = new ArrayList<>();
		expectedBody.add(EXPECTED_MACHINE_REGION);
		expectedBody.add(EXPECTED_MACHINE_NAME);
		
		List<String> actualRegionAndName = reportsSql.getRegionAndNameByMachineId(res, MACHINE_ID);

		assertEquals(expectedBody, actualRegionAndName);
		assertEquals(expectedBody, res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), GET_REGION_AND_NAME_SUCCS);
	}
}
