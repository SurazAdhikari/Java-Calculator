import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CalculatorServer {
    private static final int PORT = 1234;
    private static final String DB_URL = "jdbc:sqlite:mydatabase.db";

    private ServerSocket serverSocket;
    private Connection connection;

    public CalculatorServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server socket initialized and listening on port " + PORT);
        } catch (IOException e) {
            System.out.println("Failed to initialize server socket: " + e.getMessage());
        }
        connectToDatabase();
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the SQLite database");
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
        }
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleClientRequest(clientSocket);
            } catch (IOException e) {
                System.out.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    private void handleClientRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String request = in.readLine();
            String[] parts = request.split(",");
            if (parts.length != 3) {
                out.println("Invalid request");
                clientSocket.close();
                return;
            }

            String operator = parts[0];
            double num1 = Double.parseDouble(parts[1]);
            double num2 = Double.parseDouble(parts[2]);

            double result;
            switch (operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        out.println("Result: Division by zero error");
                        clientSocket.close();
                        return;
                    }
                    result = num1 / num2;
                    break;
                default:
                    out.println("Invalid operator");
                    clientSocket.close();
                    return;
            }

            saveResultToDatabase(operator, num1, num2, result);
            out.println("Result: " + result);

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error handling client request: " + e.getMessage());
        }
    }

    private void saveResultToDatabase(String operator, double num1, double num2, double result) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO calculations (operator, num1, num2, result) VALUES (?, ?, ?, ?)");
            statement.setString(1, operator);
            statement.setDouble(2, num1);
            statement.setDouble(3, num2);
            statement.setDouble(4, result);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save result to the database: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Disconnected from the database");
            }
            if (serverSocket != null) {
                serverSocket.close();
                System.out.println("Server socket closed");
            }
        } catch (IOException | SQLException e) {
            System.out.println("Error closing server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CalculatorServer server = new CalculatorServer();
        server.start();
        server.stop();
    }
}

/*
 * 
 * javac -cp ".:sqlite-jdbc-3.42.0.0.jar" CalculatorApp.java
 * javac -cp ".:sqlite-jdbc-3.42.0.0.jar" CalculatorServer.java
 * 
 * java -cp ".:sqlite-jdbc-3.42.0.0.jar" CalculatorServer
 * java -cp ".:sqlite-jdbc-3.42.0.0.jar" CalculatorApp
 * 
 */
