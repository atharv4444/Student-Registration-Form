import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StudentRegistrationGUI extends JFrame {

    // --- Database Configuration (SQLite) ---
    private static final String JDBC_URL = "jdbc:sqlite:student_db.sqlite";

    // --- GUI Components ---
    private JTextField nameField, rollField, courseField, emailField;
    private JLabel statusLabel;
    private JButton registerButton;

    public StudentRegistrationGUI() {
        super("Student Registration Form");
        initializeDatabase();
        setupGUI();
    }

    // --- Database Setup and Operations ---

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS students (\n"
                    + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                    + "    name TEXT NOT NULL,\n"
                    + "    roll_number TEXT UNIQUE NOT NULL,\n"
                    + "    course TEXT NOT NULL,\n"
                    + "    email TEXT UNIQUE NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            System.out.println("Database table 'students' initialized.");
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error initializing database: " + e.getMessage(), 
                "Database Connection Error", JOptionPane.ERROR_MESSAGE);
            // Fatal error, exit application
            System.exit(1);
        }
    }

    private void registerStudent() {
        // 1. Get input data
        String name = nameField.getText().trim();
        String roll = rollField.getText().trim();
        String course = courseField.getText().trim();
        String email = emailField.getText().trim();

        // 2. Input Validation
        if (!validateInput(name, roll, course, email)) {
            return;
        }

        // 3. Database Insertion
        String sql = "INSERT INTO students (name, roll_number, course, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, roll);
            pstmt.setString(3, course);
            pstmt.setString(4, email);
            pstmt.executeUpdate();

            // Success feedback
            setStatus("Success: Student " + name + " registered!", false);
            clearFields();

        } catch (SQLException e) {
            // Check for unique constraint violation (roll_number or email)
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                 setStatus("Error: Roll Number or Email already exists.", true);
            } else {
                 setStatus("Database Error: " + e.getMessage(), true);
            }
        }
    }

    private boolean validateInput(String name, String roll, String course, String email) {
        if (name.isEmpty() || roll.isEmpty() || course.isEmpty() || email.isEmpty()) {
            setStatus("Error: All fields must be filled out.", true);
            return false;
        }

        // Simple Email Validation (using regex)
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        if (!email.matches(emailRegex)) {
            setStatus("Error: Invalid email format.", true);
            return false;
        }
        
        // Simple Roll Number validation (digits only)
        if (!roll.matches("\\d+")) {
            setStatus("Error: Roll Number must be numeric.", true);
            return false;
        }

        return true;
    }

    private void clearFields() {
        nameField.setText("");
        rollField.setText("");
        courseField.setText("");
        emailField.setText("");
        nameField.requestFocus(); // Set focus back to the first field
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : new Color(0, 100, 0)); // Dark Green for success
    }

    // --- GUI Layout and Initialization ---

    private void setupGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 350);
        setLocationRelativeTo(null); // Center the window
        
        // Main panel using BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Form Panel (Center) using GridBagLayout for flexible alignment
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create fields
        nameField = new JTextField(20);
        rollField = new JTextField(20);
        courseField = new JTextField(20);
        emailField = new JTextField(20);
        
        String[] labels = {"Full Name:", "Roll Number:", "Course:", "Email:"};
        JTextField[] fields = {nameField, rollField, courseField, emailField};

        for (int i = 0; i < labels.length; i++) {
            // Label (Column 0)
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(new JLabel(labels[i]), gbc);

            // Field (Column 1)
            gbc.gridx = 1;
            gbc.gridy = i;
            gbc.weightx = 1.0;
            formPanel.add(fields[i], gbc);
        }

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // 2. Button Panel (South)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerButton = new JButton("Register Student");
        JButton viewAllButton = new JButton("View All Students (Console)");

        // Action Listeners
        registerButton.addActionListener(e -> registerStudent());
        // For simplicity, we print all records to the console (as a view-all GUI is complex)
        viewAllButton.addActionListener(e -> listAllStudentsToConsole()); 

        buttonPanel.add(registerButton);
        buttonPanel.add(viewAllButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 3. Status Bar (South/Below Buttons)
        statusLabel = new JLabel("Enter student details and click Register.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(buttonPanel, BorderLayout.NORTH);
        statusPanel.add(statusLabel, BorderLayout.SOUTH);
        
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        setVisible(true);
    }
    
    // --- Console View Utility (Optional but helpful) ---
    private void listAllStudentsToConsole() {
        System.out.println("\n--- All Registered Students ---");
        String sql = "SELECT id, name, roll_number, course, email FROM students";
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.isBeforeFirst()) {
                System.out.println("No students registered yet.");
                setStatus("No students registered yet.", false);
                return;
            }

            System.out.printf("%-5s | %-20s | %-12s | %-15s | %-30s\n", "ID", "Name", "Roll No", "Course", "Email");
            System.out.println("--------------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-5d | %-20s | %-12s | %-15s | %-30s\n",
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("roll_number"),
                    rs.getString("course"),
                    rs.getString("email")
                );
            }
            setStatus("Student list printed to console.", false);
        } catch (SQLException e) {
            setStatus("Error listing students: " + e.getMessage(), true);
        }
    }


    public static void main(String[] args) {
        // Run the GUI creation on the Event Dispatch Thread (EDT)
        try {
            // Set system look and feel for native look
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set System Look and Feel.");
        }
        
        SwingUtilities.invokeLater(StudentRegistrationGUI::new);
    }
}