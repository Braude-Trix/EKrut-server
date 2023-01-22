package server;

import static logic.EndOfMonthTask.NEW_REPORTS_CREATED;
import static logic.EndOfMonthTask.REPORTS_ALREADY_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;

import models.InventoryReport;
import models.Regions;
import models.ReportType;
import models.SavedReportRequest;
import models.StatusInMachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gui.IServerGui;
import models.Response;
import models.ResponseCode;
import serverModels.ProductInMachineHistory;
import serverModels.ServerConf;

class mysqlControllerReportsTest {
	private static final ReportType REPORT_TYPE = ReportType.INVENTORY;
	private static final Regions REGION = Regions.North;
	private static ITime timesMock;
	private Connection connMock;
	private PreparedStatement stmtMock;
	private IServerGui serverGuiMock;
	private IReportsSql reportsSqlMock;

	private mysqlController mySql;
	private ServerConf conf;
	private Response res;
	
    private static final String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
    private static final String REPORT_FOUND_MSG = "Report data was fetched successfully";
    private static final String REPORT_NOT_FOUND_MSG = "Couldn't find report for the specified parameters";
    private static final String REPORT_ERROR_MSG = "There was an error while trying to fetch report data";
    private static final String ERROR_IN_CHECKING_MSG = "Failed while checking if reports are exists";
    private static final String ERROR_WHILE_INVENTORY_GEN_MSG = "There was an error while trying to generate all reports";

	private static final int YEAR = 2022;
    private static final int MONTH = 12;
	private static final int DAYS_IM_MONTH = 31;

	private static final String INVALID_MACHINE_ID = "222";
    private static final String REGION_OF_INV_MACHINE_ID = Regions.All.name();
    private static final String MACHINE_ID = "123";
    private static final String REGION_OF_MACHINE_ID = REGION.name();
    private static final String PRODUCT_ID = "999";
    private static final int AMOUNT = 5;
    private static final int DAY = 1;
    private static final String MACHINE_NAME = "EM Building braude";
    private static final String PRODUCT_NAME = "Bamba"; 
    private static final List<ProductInMachineHistory> FILTERED_BY_DATE_PRODUCTS_ABOVE =
    		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
    				new ProductInMachineHistory(INVALID_MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY));
    private static final List<ProductInMachineHistory> FILTERED_BY_DATE_PRODUCTS_BELOW =
    		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY),
    				new ProductInMachineHistory(INVALID_MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY));
    private static final List<ProductInMachineHistory> FILTERED_BY_DATE_PRODUCTS_UNAVAILABLE =
    		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY),
    				new ProductInMachineHistory(INVALID_MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY));

    private InventoryReport getReportObject(boolean isBelow, boolean isUnavailable) {
    	List<Map<String, Integer>> dailyInventory = new ArrayList<>();
    	List<Integer> belowThresholdAmount = new ArrayList<>();
    	List<Integer> unavailableAmount = new ArrayList<>();
		int belowAmount = isBelow ? 1 : 0;
		int unAmount = isUnavailable ? 1 : 0;

		for (int i = 0; i < DAYS_IM_MONTH; i++) {
			dailyInventory.add(new HashMap<>()); // init hashmap
			dailyInventory.get(i).put(PRODUCT_NAME, AMOUNT);
			belowThresholdAmount.add(belowAmount);
			unavailableAmount.add(unAmount);
		}
    	return new InventoryReport(MACHINE_NAME, String.valueOf(MONTH), String.valueOf(YEAR),
				dailyInventory, belowThresholdAmount, unavailableAmount);
    }
    
    private InventoryReport getReportObject(boolean isBelowOnDay1, boolean isUnavailableOnDay1, 
    		boolean isBelowOnDay2, boolean isUnavailableOnDay2) {
    	List<Map<String, Integer>> dailyInventory = new ArrayList<>();
    	List<Integer> belowThresholdAmount = new ArrayList<>();
    	List<Integer> unavailableAmount = new ArrayList<>();
		int belowAmountOnDay1 = isBelowOnDay1 ? 1 : 0;
		int unAmountOnDay1 = isUnavailableOnDay1 ? 1 : 0;
		int belowAmountOnDay2 = isBelowOnDay2 ? 1 : 0;
		int unAmountOnDay2 = isUnavailableOnDay2 ? 1 : 0;

		for (int i = 0; i < DAYS_IM_MONTH; i++) {
			dailyInventory.add(new HashMap<>()); // init hashmap
			dailyInventory.get(i).put(PRODUCT_NAME, AMOUNT);
			if (i == 0) {
				belowThresholdAmount.add(belowAmountOnDay1);
				unavailableAmount.add(unAmountOnDay1);
			} else {
				belowThresholdAmount.add(belowAmountOnDay2);
				unavailableAmount.add(unAmountOnDay2);
			}
		}
    	return new InventoryReport(MACHINE_NAME, String.valueOf(MONTH), String.valueOf(YEAR),
				dailyInventory, belowThresholdAmount, unavailableAmount);
    }
    
	private void initMocksForSuccessfulCreate() {
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID))))
				.thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.deleteLastMonthFromTable(any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.transferDataFromProductInMachineToHistory(any())).thenReturn(Boolean.TRUE);
	}

	@SuppressWarnings("unchecked")
	private void setMockQueryAsError() throws SQLException {
		mySql.conn = connMock;
		when(connMock.prepareStatement(anyString())).thenReturn(stmtMock);
		when(stmtMock.executeQuery()).thenThrow(SQLException.class);
	}
	
	@BeforeAll
	static void beforeAll() {
		timesMock = mock(ITime.class);
		when(timesMock.getReportsYear()).thenReturn(YEAR);
		when(timesMock.getDaysInMonthOfReport()).thenReturn(DAYS_IM_MONTH);
		when(timesMock.getReportsMonth()).thenReturn(MONTH);
	}
   
	@BeforeEach
	void setUp() throws Exception {
		connMock = mock(Connection.class);
		stmtMock = mock(PreparedStatement.class);
		serverGuiMock = mock(IServerGui.class);
		reportsSqlMock = mock(IReportsSql.class);
		when(reportsSqlMock.generateOrdersReport(any())).thenReturn(true);
		when(reportsSqlMock.generateUsersReport(any())).thenReturn(true);
		when(reportsSqlMock.checkIfReportsAreAlreadyCreated()).thenReturn(false);

		//Opening a mysql connection:
		conf = Server.getDefaultServerConf();
		mySql = new mysqlController(conf, serverGuiMock, reportsSqlMock, timesMock);
		res = new Response();
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

	// Functionality: Test the getReport method when an invalid month is passed
	// Input data: A SavedReportRequest with the year = 2022, month = 13,
	// report type = REPORT_TYPE, region = REGION, machineId = 2.
	// Expected result: The res body = null, the code = ResponseCode.INVALID_DATA,
	// and the description = REPORT_NOT_FOUND_MSG.
	@Test
	void getReportNotFoundInvalidMonth() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 13, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	// Functionality: Test getReport method when an invalid report type is passed.
	// Input data: A SavedReportRequest with the year = 2022, month = 12,
	// report type = null, region = REGION, machineId = 2.
	// Expected result: An exception of type NullPointerException should be thrown,
	// and the test should pass.
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

	// Functionality: Test getReport method when an invalid region is passed.
	// Input data: A SavedReportRequest object with the year = 2022, month = 12,
	// report type = REPORT_TYPE, region = null, machineId = 2.
	// Expected result: An exception of type NullPointerException should be thrown,
	// and the test should pass.
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

	// Functionality: Test getReport method when a request for all regions is passed.
	// Input data: A SavedReportRequest with the year = 2022, month = 12,
	// report type = REPORT_TYPE, region = Regions.All, machineId = 2.
	// Expected result: The res body = null, the code = ResponseCode.INVALID_DATA,
	// and the description = REPORT_NOT_FOUND_MSG.
	@Test
	void getReportNotFoundRegionAll() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, Regions.All, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	// Functionality: Test the getReport method when an invalid machineId is passed
	// Input data: A SavedReportRequest with the year = 2022, month = 12,
	// report type = REPORT_TYPE, region = REGION, machineId = 99999.
	// Expected result: The res body = null, the code = ResponseCode.INVALID_DATA,
	// and the description = REPORT_NOT_FOUND_MSG.
	@Test
	void getReportNotFoundInvalidMachine() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 99999);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	
	// Functionality: Test the getReport method when an SQL exception is thrown.
	// Input data: A SavedReportRequest with the year = 1970, month = 1,
	// report type = REPORT_TYPE, region = REGION, machineId = 1.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = REPORT_ERROR_MSG 
	// and the method setPrintToConsole() should be called with EXECUTE_UPDATE_ERROR_MSG.
	@Test
	void getReportThrowsSqlException() throws SQLException {
		setMockQueryAsError();
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 1, REPORT_TYPE, REGION, 1);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), REPORT_ERROR_MSG);
		verify(serverGuiMock).setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
	}

	// Functionality: Test the generateAllReports method when an error occurs 
	// while checking if reports are already created.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_IN_CHECKING_MSG
	@Test
	void generateAllReportsCheckIsCreatedError() throws SQLException {
		when(reportsSqlMock.checkIfReportsAreAlreadyCreated()).thenReturn(null);

		mySql.generateAllReports(res);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_IN_CHECKING_MSG);
	}
	
	// Functionality: Test the generateAllReports method when reports are already created.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = REPORTS_ALREADY_EXISTS
	@Test
	void generateAllReportsCheckIsCreatedTrue() {
		when(reportsSqlMock.checkIfReportsAreAlreadyCreated()).thenReturn(true);

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), REPORTS_ALREADY_EXISTS);
	}

	// Functionality: Test the generateAllReports method when getAllProductsHistory fails.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_WHILE_INVENTORY_GEN_MSG, 
	// the method setPrintToConsole() should be called twice.
	@Test
	void getAllProductsHistoryFailed() {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(null);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}

	// Functionality: Test the generateAllReports method when it fails getNameByProductId.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_WHILE_INVENTORY_GEN_MSG
	@Test
	void getNameByProductIdFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(null);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	// Functionality: Test the generateAllReports method when it fails getRegionAndNameByMachineId.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_WHILE_INVENTORY_GEN_MSG
	@Test
	void getRegionAndNameByMachineIdFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(null);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	// Functionality: Test the generateAllReports method when it fails saveInventoryReportInDb.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_WHILE_INVENTORY_GEN_MSG
	@Test
	void saveInventoryReportInDbFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID)))).thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.FALSE);

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	// Functionality: Test the generateAllReports method when it fails deleteLastMonthFromTable.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_WHILE_INVENTORY_GEN_MSG.
	@Test
	void deleteLastMonthFromTableFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID)))).thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.deleteLastMonthFromTable(any())).thenReturn(Boolean.FALSE);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	// Functionality: Test the generateAllReports method when it fails transferData.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.DB_ERROR,
	// the description = ERROR_WHILE_INVENTORY_GEN_MSG.
	@Test
	void transferDataFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID)))).thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.deleteLastMonthFromTable(any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.transferDataFromProductInMachineToHistory(any())).thenReturn(Boolean.FALSE);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	// Functionality: Test the generateAllReports method of products above threshold.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfAbove() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		initMocksForSuccessfulCreate();
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	// Functionality: Test the generateAllReports method of products below threshold.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfBelow() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_BELOW);
		initMocksForSuccessfulCreate();
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	// Functionality: Test the generateAllReports method of unavailable products.
	// Input data: No input data.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfUnavailable() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_UNAVAILABLE);
		initMocksForSuccessfulCreate();
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, true)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
	
	// Functionality: Test the generateAllReports method of same status of products between days.
	// Input data: Two days of a product with status above.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfSameStatusBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> sameStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(sameStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
	
	// Functionality: Test the generateAllReports method of status above to below of products between days.
	// Input data: Two days of a product with status above and below.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfAboveToBelowBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false, true, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	// Functionality: Test the generateAllReports method of status above to unavailable of products between days.
	// Input data: Two days of a product with status above to unavailable.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfAboveToUnavailableBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false, true, true)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	// Functionality: Test the generateAllReports method of status below to unavailable of products between days.
	// Input data: Two days of a product with status below to unavailable.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfBelowToUnavailableBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, false, true, true)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	// Functionality: Test the generateAllReports method of status below to above of products between days.
	// Input data: Two days of a product with status below to above.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfBelowToAboveBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, false, false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	// Functionality: Test the generateAllReports method of status unavailable to below of products between days.
	// Input data: Two days of a product with status unavailable to below.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfUnavailableToBelowBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, true, true, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
	
	// Functionality: Test the generateAllReports method of status unavailable to above of products between days.
	// Input data: Two days of a product with status unavailable to above.
	// Expected result: The res body = null, the code = ResponseCode.OK,
	// the description = NEW_REPORTS_CREATED,
	// and checking the passed objects to save in db is as expected.
	@Test
	void successfulCreateInventoryReportOfUnavailableToAboveBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, true, false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
}
