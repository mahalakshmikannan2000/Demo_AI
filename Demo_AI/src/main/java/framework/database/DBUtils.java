package framework.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import framework.config.PropertyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static framework.utils.WebActions.dataDecrypt;

/**
 * Generic JDBC utility (SQL Server) driven entirely by DB.properties, kept isolated per
 * calling thread so parallel suites never share a Connection/Statement/ResultSet.
 */
public class DBUtils {
    private DBUtils() {
        // Prevent instantiation
    }

    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private static final ThreadLocal<Statement> stmtHolder = new ThreadLocal<>();
    private static final ThreadLocal<ResultSet> rsHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> dbServerNameHolder = new ThreadLocal<>();

    private static final Logger logger = LoggerFactory.getLogger(DBUtils.class);

    /*
    Method Name       : dbConnectionUtil - Initializes a database connection using the given server and environment
    Input Parameter(s): String dbServer, String env
    Output Parameter(s) : Connection
    */
    public static Connection dbConnectionUtil(String dbServer, String env) {
        dbServerNameHolder.set(dbServer + "_" + env);
        Connection connection = getConnection();
        connectionHolder.set(connection);
        if (connection != null) {
            logger.info("DB Connection created Successfully");
        }
        return connection;
    }

    /*
    Method Name       : getConnection - Returns a database connection using default configuration
    Input Parameter(s): None
    Output Parameter(s) : Connection
    */
    public static Connection getConnection() {
        String dbServerName = dbServerNameHolder.get();
        String dbUN = PropertyReader.getProperty("DB", dbServerName + "_UserName");
        String dbPass = dataDecrypt(PropertyReader.getProperty("DB", dbServerName + "_Password"));
        try {
            return DriverManager.getConnection(getDbConnectionUrl(), dbUN, dbPass);
        } catch (Exception e) {
            logger.error("Exception While establishing Connection", e);
        }
        return null;
    }

    private static String getDbConnectionUrl() {
        String dbServerName = dbServerNameHolder.get();
        String server = PropertyReader.getProperty("DB", dbServerName + "_Server");
        String dbPort = PropertyReader.getProperty("DB", "port");
        String dbName = PropertyReader.getProperty("DB", dbServerName + "_DB");
        return "jdbc:sqlserver://" + server + ":" + dbPort + ";databaseName=" + dbName;
    }

    public static String getDbConnectionUrl(String server, String dbName) {
        String dbPort = PropertyReader.getProperty("DB", "port");
        return "jdbc:sqlserver://" + server + ":" + dbPort + ";databaseName=" + dbName;
    }

    public static Connection getDynamicConnection(String server, String dbName, String userName, String password) {
        String dbPass = dataDecrypt(password);
        try {
            String url = getDbConnectionUrl(server, dbName);
            return DriverManager.getConnection(url, userName, dbPass);
        } catch (Exception e) {
            logger.error("Exception While establishing Connection {}", e.getMessage());
        }
        return null;
    }

    /*
    Method Name       : getQueryResult - Execute SELECT query (return multiple rows & columns)
    Input Parameter(s): String query
    Output Parameter(s) : List<HashMap<String, String>>
    */
    public static List<HashMap<String, String>> getQueryResult(String query) {
        List<HashMap<String, String>> results = new ArrayList<>();
        try (Connection connection = getConnection()) {
            if (connection == null) {
                logger.error("Connection creation is failing - cannot execute query");
                return results;
            }
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    HashMap<String, String> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getString(i));
                    }
                    results.add(row);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while executing get query result from DB {}", String.valueOf(e));
        }
        return results;
    }

    /*
    Method Name       : getSingleValue - Execute SELECT query (return single value)
    Input Parameter(s): String query
    Output Parameter(s) : String
    */
    public static String getSingleValue(String query) {
        String value = null;
        try (Connection connection = getConnection()) {
            if (connection == null) {
                logger.error("Connection creation is failing - cannot execute query");
                return null;
            }
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    value = rs.getString(1);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while executing get result from DB {}", String.valueOf(e));
        }
        return value;
    }

    /*
    Method Name       : executeUpdate - Executes the given SQL update/insert/delete query
    Input Parameter(s): String query
    Output Parameter(s) : int
    */
    public static int executeUpdate(String query) {
        Connection connection = connectionHolder.get();
        if (connection == null) {
            logger.error("No connection available on this thread. Call dbConnectionUtil() first.");
            return 0;
        }
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.error("Exception while executing update query {}", String.valueOf(e));
        }
        return 0;
    }

    /*
    Method Name       : closeConnection - Closes the active database connection for the calling thread
    Input Parameter(s): None
    Output Parameter(s) : void
    */
    public static void closeConnection() {
        try {
            ResultSet rs = rsHolder.get();
            if (rs != null) rs.close();
        } catch (SQLException e) {
            logger.error("Exception while closing ResultSet", e);
        } finally {
            rsHolder.remove();
        }

        try {
            Statement stmt = stmtHolder.get();
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            logger.error("Exception while closing Statement", e);
        } finally {
            stmtHolder.remove();
        }

        try {
            Connection connection = connectionHolder.get();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            logger.error("Exception while closing Connection", e);
        } finally {
            connectionHolder.remove();
            dbServerNameHolder.remove();
        }
    }
}
