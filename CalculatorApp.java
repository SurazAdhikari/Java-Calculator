import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CalculatorApp extends JFrame {
    private JTextField displayField;
    private JButton[] numberButtons;
    private JButton[] operationButtons;
    private JButton clearButton;
    private JButton equalsButton;

    private final String serverAddress = "localhost";
    private final int serverPort = 1234;

    private StringBuilder inputBuffer;

    public CalculatorApp() {
        super("Calculator");
        initializeGUI();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void initializeGUI() {
        inputBuffer = new StringBuilder();

        displayField = new JTextField(15);
        displayField.setEditable(false);

        numberButtons = new JButton[10];
        for (int i = 0; i < 10; i++) {
            numberButtons[i] = new JButton(String.valueOf(i));
            numberButtons[i].addActionListener(new NumberButtonListener());
        }

        operationButtons = new JButton[5];
        operationButtons[0] = new JButton("+");
        operationButtons[1] = new JButton("-");
        operationButtons[2] = new JButton("*");
        operationButtons[3] = new JButton("/");
        operationButtons[4] = new JButton("=");
        for (JButton button : operationButtons) {
            button.addActionListener(new OperationButtonListener());
        }

        clearButton = new JButton("AC");
        clearButton.addActionListener(new ClearButtonListener());

        equalsButton = new JButton("=");
        equalsButton.addActionListener(new OperationButtonListener());

        JPanel buttonPanel = new JPanel(new GridLayout(4, 4));
        buttonPanel.add(numberButtons[7]);
        buttonPanel.add(numberButtons[8]);
        buttonPanel.add(numberButtons[9]);
        buttonPanel.add(operationButtons[0]);
        buttonPanel.add(numberButtons[4]);
        buttonPanel.add(numberButtons[5]);
        buttonPanel.add(numberButtons[6]);
        buttonPanel.add(operationButtons[1]);
        buttonPanel.add(numberButtons[1]);
        buttonPanel.add(numberButtons[2]);
        buttonPanel.add(numberButtons[3]);
        buttonPanel.add(operationButtons[2]);
        buttonPanel.add(clearButton);
        buttonPanel.add(numberButtons[0]);
        buttonPanel.add(equalsButton);
        buttonPanel.add(operationButtons[3]);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(displayField, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);

        this.add(panel);
    }

    private void sendInputToServer(String input) {
        try {
            Client client = new Client(serverAddress, serverPort);
            String operator = "";
            String num1 = "";
            String num2 = "";

            // Find the operator
            for (JButton operationButton : operationButtons) {
                if (input.contains(operationButton.getText())) {
                    operator = operationButton.getText();
                    break;
                }
            }

            // Split the input based on the operator
            String[] parts = input.split("\\" + operator);
            if (parts.length != 2) {
                displayField.setText("Invalid input");
                return;
            }

            num1 = parts[0].trim();
            num2 = parts[1].trim();

            String request = operator + "," + num1 + "," + num2;
            client.sendRequest(request, new ResponseHandler() {
                public void handleSuccessResponse(String response) {
                    displayField.setText(response);
                }

                public void handleErrorResponse(String errorMessage) {
                    displayField.setText("Error occurred: " + errorMessage);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class NumberButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            String buttonText = button.getText();
            inputBuffer.append(buttonText);
            displayField.setText(inputBuffer.toString());
        }
    }

    private class OperationButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            String buttonText = button.getText();

            if (buttonText.equals("=")) {
                String input = inputBuffer.toString();
                sendInputToServer(input);
                inputBuffer.setLength(0);
            } else {
                inputBuffer.append(buttonText);
                displayField.setText(inputBuffer.toString());
            }
        }
    }

    private class ClearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            inputBuffer.setLength(0);
            displayField.setText("");
        }
    }

    private interface ResponseHandler {
        void handleSuccessResponse(String response);

        void handleErrorResponse(String errorMessage);
    }

    private class Client {
        private String serverAddress;
        private int serverPort;

        public Client(String serverAddress, int serverPort) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        public void sendRequest(String input, ResponseHandler responseHandler) {
            try {
                Socket socket = new Socket(serverAddress, serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(input);
                String response = in.readLine();

                if (response.startsWith("Result: ")) {
                    responseHandler.handleSuccessResponse(response.substring(8));
                } else if (response.startsWith("Error: ")) {
                    responseHandler.handleErrorResponse(response.substring(7));
                } else {
                    responseHandler.handleErrorResponse("Unknown error occurred");
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new CalculatorApp();
            }
        });
    }
}
