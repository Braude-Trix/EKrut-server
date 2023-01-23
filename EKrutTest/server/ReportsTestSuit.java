package server;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ReportsSqlTest.class, 
	MysqlControllerReportsTest.class})
public class ReportsTestSuit {

}
