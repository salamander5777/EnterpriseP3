/*
    Name: Michael Gilday
    Course: CNT 4714 Fall 2023
    Assignment title: Project 3 – A Three-Tier Distributed Web-Based Application
    Date: December 5, 2023
*/

import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Vector;

public class DatabaseManager {
    private final Connection connection;
    private Vector<String> columns;
    private static DatabaseManager databaseManager;

    //The necessary main function for starting the entire application. All it does is call the function "initializeWindow", which create the GUI.
    public static void main(String[] args) {
    }

    //The function below initializes the buttons.
    private static void initializeButtons() {
        //Listens for when the connectButton button is clicked.
        connectButton.addActionListener(e -> {
            String selectedDatabase = "project4"; //Initializing the string variable "selectedDatabase" which will be changed below.

            //Creating a FileInput to read in the corresponding property value below. Also creating a dataSource which will use the read in values from the properties file.
            FileInputStream fileIn;
            MysqlDataSource dataSource;
            Properties properties = new Properties();

            try {
                //Setting the fileInputStream to the selected users properties file.
                fileIn = new FileInputStream("src/" + userComboBox.getSelectedItem());
                dataSource = new MysqlDataSource();
                properties.load(fileIn);

                //If the input username and password matches what's in the properties file then it will read in the information from the file and connect to the chosen database.
                if(usernameTextField.getText().equals(properties.getProperty("MYSQL_DB_USERNAME")) && passwordTextField.getText().equals(properties.getProperty("MYSQL_DB_PASSWORD"))){
                    dataSource.setURL(properties.getProperty("MYSQL_DB_URL") + "/" + selectedDatabase);
                    dataSource.setUser(properties.getProperty("MYSQL_DB_USERNAME"));
                    dataSource.setPassword(properties.getProperty("MYSQL_DB_PASSWORD"));

                    Connection connection = dataSource.getConnection();

                    connectionStatusLabel.setText("CONNECTED TO: jdbc:mysql://localhost:3306/" + selectedDatabase);
                    connectionStatusLabel.setForeground(Color.YELLOW);
                    connectionStatusPanel.add(connectionStatusLabel);
                    executeButton.setEnabled(true);

                    databaseManager = new DatabaseManager(connection);
                }else{ //This means that the user did not put in the correct credentials for the user, which will lock the executeButton, and change the connection status to "Not Connected".
                    connectionStatusLabel.setText("NOT CONNECTED - User Credentials Do Not Match Properties File!");
                    connectionStatusLabel.setForeground(Color.RED);
                    connectionStatusPanel.add(connectionStatusLabel);
                    executeButton.setEnabled(false);
                }
            } catch (IOException | SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        //Listens for when the executeButton button is clicked.
        executeButton.addActionListener(e -> {
            String query = sqlCommandTextArea.getText(); //Reading in the query from the user input box.
            Vector<String> columns;
            Vector<Vector<String>> results;

            //If the select statement is used then we need to set up the sqlResultsTable using information gotten from the runQuery function.
            if (query.toLowerCase().startsWith("select")) {
                try {
                    results = databaseManager.runQuery(query);
                    columns = databaseManager.getColumns();
                    sqlResultsTable.setModel(new DefaultTableModel(results, columns));

                    databaseManager.updateOperationsCount(usernameTextField.getText(), true); //Passing the value of 'true' to indicate that num_query needs to be incremented by one.
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null,
                            ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
                }
            } else { //This is entered if the initial statement is anything other than select. It will attempt to run the update and state how many rows have been updated, and will state an error if the input is invalid.
                try {
                    int rowsUpdated = databaseManager.runUpdate(query);
                    JOptionPane.showMessageDialog(null,
                            "Successful Update... " + rowsUpdated + " rows updated.", "Update Successful", JOptionPane.INFORMATION_MESSAGE);

                    databaseManager.updateOperationsCount(usernameTextField.getText(), false); //Passing the value of 'false' to indicate that num_updates needs to be incremented.
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null,
                            ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        clearCommandButton.addActionListener(e -> sqlCommandTextArea.setText(""));
        clearResultsButton.addActionListener(e -> sqlResultsTable.setModel(new DefaultTableModel(new String[][]{new String[]{""}}, new String[]{""})));
        mainPanel.add(connectButton);
        mainPanel.add(executeButton);
        mainPanel.add(clearCommandButton);
        mainPanel.add(clearResultsButton);
    }

    public DatabaseManager(Connection connection) {
        this.connection = connection;
    }

    //The function below gets the information needed for the sqlResultsTable display.
    public Vector<Vector<String>> runQuery(String query) throws SQLException {
        Vector<Vector<String>> results = new Vector<>();

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query)) {
            ResultSetMetaData metaData = resultSet.getMetaData();

            int numColumns = metaData.getColumnCount();
            setColumns(numColumns, metaData);

            while (resultSet.next()) {
                Vector<String> row = new Vector<>();
                for (int i = 1; i <= numColumns; i++) {
                    row.add(resultSet.getString(i));
                }
                results.add(row);
            }
        }
        return results;
    }

    //Grabbing the column information from the table in the database.
    public Vector<String> getColumns() throws SQLException {
        return this.columns;
    }

    //The function below attempts to execute the passed in query, changing the database, and passing back the number of changes.
    public int runUpdate(String query) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(query);
        }
    }

    //Populating the columns instance with the names of the columns from the current table.
    public void setColumns(int numColumns, ResultSetMetaData metaData) throws SQLException {
        columns = new Vector<>();
        for (int i = 1; i <= numColumns; i++) {
            columns.add(metaData.getColumnName(i));
        }
    }
}