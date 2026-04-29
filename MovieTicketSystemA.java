package oracle;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class MovieTicketSystemA extends JFrame {
    // Database connection
    private Connection conn;
    
    // UI components
    private CardLayout cardLayout = new CardLayout();
    private JPanel cards = new JPanel(cardLayout);

    // Login components
    private JTextField loginEmailField;
    private JPasswordField loginPasswordField;
    private JLabel loginStatusLabel;

    // Signup components
    private JTextField signupNameField, signupEmailField, signupPhoneField;
    private JPasswordField signupPasswordField;
    private JLabel signupStatusLabel;

    // Dashboard components
    private JLabel dashboardWelcomeLabel;

    // Booking components
    private JComboBox<String> bookingMovieComboBox;
    private JComboBox<String> bookingShowtimeComboBox;
    private JComboBox<String> bookingRangeComboBox;
    private JComboBox<Integer> bookingTicketCountComboBox;
    private JPanel seatPanel;
    private Set<String> selectedSeats = new HashSet<>();
    private JTextArea bookingConfirmationArea;

    // Current logged-in user
    private int currentUserId = -1;
    private String currentUserName = "";

    public MovieTicketSystemA() {
        // Initialize database connection
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@//10.11.0.22:1521/xe",
                    "FA24cs094", "oracle");
            
            // Create tables if they don't exist
            initializeDatabase();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "DB Connection failed: " + e.getMessage());
            System.exit(1);
        }

        setTitle("🎟️ Movie Ticket Booking System (DB)");
        setSize(950, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Setup cards
        cards.add(createMainMenuPanel(), "MainMenu");
        cards.add(createLoginPanel(), "Login");
        cards.add(createSignupPanel(), "Signup");
        cards.add(createDashboardPanel(), "Dashboard");
        cards.add(createBookingPanel(), "Booking");

        add(cards);
        showMainMenu();

        setVisible(true);
    }

    private void initializeDatabase() throws SQLException {
        // Create sequence for user IDs
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE SEQUENCE user_seq START WITH 1 INCREMENT BY 1");
        } catch (SQLException e) {
            // Sequence already exists
        }

        // Create users table
        String createUsersTable = "CREATE TABLE users (" +
                "user_id NUMBER PRIMARY KEY, " +
                "name VARCHAR2(100) NOT NULL, " +
                "email VARCHAR2(100) UNIQUE NOT NULL, " +
                "password VARCHAR2(100) NOT NULL, " +
                "phone VARCHAR2(20))";
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(createUsersTable);
        } catch (SQLException e) {
            // Table already exists
        }

        // Create movies table
        String createMoviesTable = "CREATE TABLE movies (" +
                "movie_id NUMBER PRIMARY KEY, " +
                "title VARCHAR2(100) NOT NULL, " +
                "available_seats NUMBER NOT NULL)";
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(createMoviesTable);
            
            // Insert sample movies if table was just created
            if (stmt.executeQuery("SELECT COUNT(*) FROM movies").getInt(1) == 0) {
                stmt.execute("INSERT INTO movies VALUES (1, 'Avengers: Endgame', 50)");
                stmt.execute("INSERT INTO movies VALUES (2, 'Inception', 40)");
                stmt.execute("INSERT INTO movies VALUES (3, 'The Dark Knight', 30)");
                stmt.execute("INSERT INTO movies VALUES (4, 'Interstellar', 25)");
                stmt.execute("INSERT INTO movies VALUES (5, 'Titanic', 20)");
            }
        } catch (SQLException e) {
            // Table already exists
        }

        // Create showtimes table
        String createShowtimesTable = "CREATE TABLE showtimes (" +
                "show_id NUMBER PRIMARY KEY, " +
                "movie_id NUMBER REFERENCES movies(movie_id), " +
                "show_time VARCHAR2(20) NOT NULL)";
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(createShowtimesTable);
            
            // Insert sample showtimes if table was just created
            if (stmt.executeQuery("SELECT COUNT(*) FROM showtimes").getInt(1) == 0) {
                stmt.execute("INSERT INTO showtimes VALUES (1, 1, '10:00 AM')");
                stmt.execute("INSERT INTO showtimes VALUES (2, 1, '1:00 PM')");
                stmt.execute("INSERT INTO showtimes VALUES (3, 1, '4:00 PM')");
                stmt.execute("INSERT INTO showtimes VALUES (4, 2, '10:00 AM')");
                stmt.execute("INSERT INTO showtimes VALUES (5, 2, '1:00 PM')");
                // Add more showtimes as needed
            }
        } catch (SQLException e) {
            // Table already exists
        }

        // Create bookings table
        String createBookingsTable = "CREATE TABLE bookings (" +
                "booking_id NUMBER PRIMARY KEY, " +
                "user_id NUMBER REFERENCES users(user_id), " +
                "movie_id NUMBER REFERENCES movies(movie_id), " +
                "show_id NUMBER REFERENCES showtimes(show_id), " +
                "tickets NUMBER NOT NULL, " +
                "seats VARCHAR2(200) NOT NULL, " +
                "range_type VARCHAR2(50) NOT NULL, " +
                "total_price NUMBER NOT NULL, " +
                "booking_date DATE DEFAULT SYSDATE)";
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(createBookingsTable);
        } catch (SQLException e) {
            // Table already exists
        }
    }

    // Main Menu
    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 240, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        JLabel welcomeLabel = new JLabel("Welcome to Movie Ticket Booking System", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 28));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(welcomeLabel, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 24));
        loginBtn.setBackground(new Color(100, 149, 237));
        loginBtn.setForeground(Color.WHITE);
        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        panel.add(loginBtn, gbc);

        JButton signupBtn = new JButton("Sign Up");
        signupBtn.setFont(new Font("Arial", Font.BOLD, 24));
        signupBtn.setBackground(new Color(60, 179, 113));
        signupBtn.setForeground(Color.WHITE);
        gbc.gridx = 1;
        panel.add(signupBtn, gbc);

        loginBtn.addActionListener(e -> showLogin());
        signupBtn.addActionListener(e -> showSignup());

        return panel;
    }

    // Login Panel
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy++;

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gbc.gridx = 0;
        panel.add(emailLabel, gbc);

        loginEmailField = new JTextField(25);
        loginEmailField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(loginEmailField, gbc);

        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gbc.gridx = 0;
        panel.add(passwordLabel, gbc);

        loginPasswordField = new JPasswordField(25);
        loginPasswordField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(loginPasswordField, gbc);

        gbc.gridy++;
        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        gbc.gridy++;
        loginStatusLabel = new JLabel("", SwingConstants.CENTER);
        loginStatusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        loginStatusLabel.setForeground(Color.RED);
        panel.add(loginStatusLabel, gbc);

        gbc.gridy++;
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(backBtn, gbc);

        loginBtn.addActionListener(e -> loginUser());
        backBtn.addActionListener(e -> showMainMenu());

        return panel;
    }

    // Signup Panel
    private JPanel createSignupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 255, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Sign Up", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy++;

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gbc.gridx = 0;
        panel.add(nameLabel, gbc);

        signupNameField = new JTextField(25);
        signupNameField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(signupNameField, gbc);

        gbc.gridy++;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gbc.gridx = 0;
        panel.add(emailLabel, gbc);

        signupEmailField = new JTextField(25);
        signupEmailField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(signupEmailField, gbc);

        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gbc.gridx = 0;
        panel.add(passwordLabel, gbc);

        signupPasswordField = new JPasswordField(25);
        signupPasswordField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(signupPasswordField, gbc);

        gbc.gridy++;
        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gbc.gridx = 0;
        panel.add(phoneLabel, gbc);

        signupPhoneField = new JTextField(25);
        signupPhoneField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(signupPhoneField, gbc);

        gbc.gridy++;
        JButton signupBtn = new JButton("Sign Up");
        signupBtn.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(signupBtn, gbc);

        gbc.gridy++;
        signupStatusLabel = new JLabel("", SwingConstants.CENTER);
        signupStatusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        signupStatusLabel.setForeground(Color.RED);
        panel.add(signupStatusLabel, gbc);

        gbc.gridy++;
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(backBtn, gbc);

        signupBtn.addActionListener(e -> signupUser());
        backBtn.addActionListener(e -> showMainMenu());

        return panel;
    }

    // Dashboard Panel
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(7, 1, 20, 20));
        panel.setBackground(new Color(255, 250, 240));

        dashboardWelcomeLabel = new JLabel("", SwingConstants.CENTER);
        dashboardWelcomeLabel.setFont(new Font("Arial", Font.BOLD, 28));
        panel.add(dashboardWelcomeLabel);

        JButton movieListBtn = createButton("🎞️ Movie List");
        JButton bookTicketBtn = createButton("🎫 Book Ticket");
        JButton showTimesBtn = createButton("⏰ Show Times");
        JButton myBookingsBtn = createButton("📄 My Bookings");
        JButton logoutBtn = createButton("🚪 Logout");

        panel.add(movieListBtn);
        panel.add(bookTicketBtn);
        panel.add(showTimesBtn);
        panel.add(myBookingsBtn);
        panel.add(logoutBtn);

        movieListBtn.addActionListener(e -> showMovieList());
        bookTicketBtn.addActionListener(e -> showBookingPanel());
        showTimesBtn.addActionListener(e -> showShowTimes());
        myBookingsBtn.addActionListener(e -> showMyBookings());
        logoutBtn.addActionListener(e -> logout());

        return panel;
    }

    // Booking Panel
    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        topPanel.setBackground(new Color(240, 248, 255));

        bookingMovieComboBox = new JComboBox<>();
        bookingShowtimeComboBox = new JComboBox<>();
        bookingRangeComboBox = new JComboBox<>(new String[]{
                "Front Row (₹150)", "Middle Row (₹200)", "Back Row (₹180)"
        });

        bookingTicketCountComboBox = new JComboBox<>();
        for (int i = 1; i <= 10; i++) bookingTicketCountComboBox.addItem(i);

        topPanel.add(new JLabel("🎬 Movie:"));
        topPanel.add(bookingMovieComboBox);
        topPanel.add(new JLabel("⏰ Show Time:"));
        topPanel.add(bookingShowtimeComboBox);
        topPanel.add(new JLabel("🪑 Range:"));
        topPanel.add(bookingRangeComboBox);
        topPanel.add(new JLabel("🎫 Tickets:"));
        topPanel.add(bookingTicketCountComboBox);

        panel.add(topPanel, BorderLayout.NORTH);

        seatPanel = new JPanel(new GridLayout(5, 10, 5, 5));
        seatPanel.setBorder(BorderFactory.createTitledBorder("🪑 Select Your Seats"));
        panel.add(new JScrollPane(seatPanel), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        JButton confirmBtn = new JButton("💳 Confirm Booking");
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 18));
        confirmBtn.setBackground(new Color(60, 179, 113));
        confirmBtn.setForeground(Color.WHITE);

        JButton backToDashboardBtn = new JButton("⬅ Back to Dashboard");
        backToDashboardBtn.setFont(new Font("Arial", Font.BOLD, 18));
        backToDashboardBtn.setBackground(new Color(220, 20, 60));
        backToDashboardBtn.setForeground(Color.WHITE);

        JPanel bottomBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomBtnPanel.add(backToDashboardBtn);
        bottomBtnPanel.add(confirmBtn);

        bookingConfirmationArea = new JTextArea(6, 40);
        bookingConfirmationArea.setEditable(false);
        bookingConfirmationArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        bottomPanel.add(bottomBtnPanel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(bookingConfirmationArea), BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        confirmBtn.addActionListener(e -> processBooking());
        backToDashboardBtn.addActionListener(e -> cardLayout.show(cards, "Dashboard"));

        bookingMovieComboBox.addActionListener(e -> {
            loadShowtimesForMovie();
            resetSeatSelection();
        });
        bookingTicketCountComboBox.addActionListener(e -> resetSeatSelection());
        bookingRangeComboBox.addActionListener(e -> resetSeatSelection());

        loadMoviesForBooking();

        return panel;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        return btn;
    }

    // Navigation methods
    private void showMainMenu() {
        cardLayout.show(cards, "MainMenu");
    }

    private void showLogin() {
        loginEmailField.setText("");
        loginPasswordField.setText("");
        loginStatusLabel.setText("");
        cardLayout.show(cards, "Login");
    }

    private void showSignup() {
        signupNameField.setText("");
        signupEmailField.setText("");
        signupPasswordField.setText("");
        signupPhoneField.setText("");
        signupStatusLabel.setText("");
        cardLayout.show(cards, "Signup");
    }

    private void showDashboard() {
        dashboardWelcomeLabel.setText("Welcome, " + currentUserName);
        cardLayout.show(cards, "Dashboard");
        loadMoviesForBooking();
        resetSeatSelection();
    }

    private void showBookingPanel() {
        cardLayout.show(cards, "Booking");
    }

    // Database operations
    private void loginUser() {
        String email = loginEmailField.getText().trim();
        String password = new String(loginPasswordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            loginStatusLabel.setText("Please enter email and password.");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id, name FROM users WHERE email=? AND password=?");
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("user_id");
                currentUserName = rs.getString("name");
                showDashboard();
            } else {
                loginStatusLabel.setText("Invalid email or password.");
            }
        } catch (SQLException e) {
            loginStatusLabel.setText("Database error. Please try again.");
            e.printStackTrace();
        }
    }

    private void signupUser() {
        String name = signupNameField.getText().trim();
        String email = signupEmailField.getText().trim();
        String password = new String(signupPasswordField.getPassword()).trim();
        String phone = signupPhoneField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            signupStatusLabel.setText("Please fill all required fields.");
            return;
        }

        try {
            // Check if email already exists
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT email FROM users WHERE email=?");
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                signupStatusLabel.setText("Email already exists.");
                return;
            }

            // Insert new user
            PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (user_id, name, email, password, phone) " +
                            "VALUES (user_seq.NEXTVAL, ?, ?, ?, ?)");
            insertStmt.setString(1, name);
            insertStmt.setString(2, email);
            insertStmt.setString(3, password);
            insertStmt.setString(4, phone);
            insertStmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
            showLogin();
        } catch (SQLException e) {
            signupStatusLabel.setText("Database error. Please try again.");
            e.printStackTrace();
        }
    }

    private void logout() {
        currentUserId = -1;
        currentUserName = "";
        showMainMenu();
    }

    private void loadMoviesForBooking() {
        bookingMovieComboBox.removeAllItems();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT movie_id, title FROM movies");
            while (rs.next()) {
                bookingMovieComboBox.addItem(rs.getString("title"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading movies from database.");
            e.printStackTrace();
        }
    }

    private void loadShowtimesForMovie() {
        bookingShowtimeComboBox.removeAllItems();
        String selectedMovie = (String) bookingMovieComboBox.getSelectedItem();
        if (selectedMovie == null) return;

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT s.show_id, s.show_time " +
                            "FROM showtimes s JOIN movies m ON s.movie_id = m.movie_id " +
                            "WHERE m.title = ?");
            ps.setString(1, selectedMovie);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                bookingShowtimeComboBox.addItem(rs.getString("show_time"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading showtimes.");
            e.printStackTrace();
        }
    }

    private void resetSeatSelection() {
        selectedSeats.clear();
        seatPanel.removeAll();

        for (int row = 1; row <= 5; row++) {
            for (int col = 1; col <= 10; col++) {
                String seatId = "R" + row + "S" + col;
                JToggleButton seatButton = new JToggleButton(seatId);
                seatButton.setBackground(new Color(173, 216, 230));
                seatButton.addItemListener(e -> {
                    if (seatButton.isSelected()) selectedSeats.add(seatId);
                    else selectedSeats.remove(seatId);
                });
                seatPanel.add(seatButton);
            }
        }
        seatPanel.revalidate();
        seatPanel.repaint();
        bookingConfirmationArea.setText("");
    }

    private void showMovieList() {
        StringBuilder sb = new StringBuilder("Available Movies:\n\n");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT title, available_seats FROM movies");
            while (rs.next()) {
                sb.append(rs.getString("title")).append(" - ")
                  .append(rs.getInt("available_seats")).append(" seats available\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Movie List", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching movies from database.");
            e.printStackTrace();
        }
    }

    private void showShowTimes() {
        StringBuilder sb = new StringBuilder("Show Times:\n\n");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT m.title, s.show_time FROM showtimes s JOIN movies m ON s.movie_id = m.movie_id");
            while (rs.next()) {
                sb.append(rs.getString("title")).append(" - ")
                  .append(rs.getString("show_time")).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Show Times", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching showtimes from database.");
            e.printStackTrace();
        }
    }

    private void showMyBookings() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Movie", "Show Time", "Range", "Tickets", "Seats"}, 0);

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.title, s.show_time, b.range_type, b.tickets, b.seats " +
                    "FROM bookings b " +
                    "JOIN movies m ON b.movie_id = m.movie_id " +
                    "JOIN showtimes s ON b.show_id = s.show_id " +
                    "WHERE b.user_id = ?");
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();

            boolean hasBooking = false;
            while (rs.next()) {
                hasBooking = true;
                model.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("show_time"),
                        rs.getString("range_type"),
                        rs.getInt("tickets"),
                        rs.getString("seats")
                });
            }

            if (!hasBooking) {
                JOptionPane.showMessageDialog(this, "No bookings found.");
                return;
            }

            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(850, 300));
            JOptionPane.showMessageDialog(this, scrollPane, "My Bookings", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching bookings from database.");
            e.printStackTrace();
        }
    }

    private void processBooking() {
        String selectedMovie = (String) bookingMovieComboBox.getSelectedItem();
        if (selectedMovie == null) {
            JOptionPane.showMessageDialog(this, "Please select a movie.");
            return;
        }
        
        String selectedShowTime = (String) bookingShowtimeComboBox.getSelectedItem();
        String selectedRange = (String) bookingRangeComboBox.getSelectedItem();
        int ticketCount = (Integer) bookingTicketCountComboBox.getSelectedItem();
        
        if (selectedSeats.size() != ticketCount) {
            JOptionPane.showMessageDialog(this,
                    "Please select exactly " + ticketCount + " seats.",
                    "Seat Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Get movie ID and available seats
            PreparedStatement movieStmt = conn.prepareStatement(
                    "SELECT movie_id, available_seats FROM movies WHERE title = ?");
            movieStmt.setString(1, selectedMovie);
            ResultSet movieRs = movieStmt.executeQuery();
            
            if (!movieRs.next()) {
                JOptionPane.showMessageDialog(this, "Movie not found in database.");
                return;
            }
            
            int movieId = movieRs.getInt("movie_id");
            int availableSeats = movieRs.getInt("available_seats");
            
            if (ticketCount > availableSeats) {
                JOptionPane.showMessageDialog(this,
                        "Not enough seats available.",
                        "Booking Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get showtime ID
            PreparedStatement showtimeStmt = conn.prepareStatement(
                    "SELECT show_id FROM showtimes WHERE show_time = ? AND movie_id = ?");
            showtimeStmt.setString(1, selectedShowTime);
            showtimeStmt.setInt(2, movieId);
            ResultSet showtimeRs = showtimeStmt.executeQuery();
            
            if (!showtimeRs.next()) {
                JOptionPane.showMessageDialog(this, "Showtime not found in database.");
                return;
            }
            
            int showId = showtimeRs.getInt("show_id");

            // Determine price per ticket based on range
            double pricePerTicket;
            switch (selectedRange) {
                case "Front Row (₹150)":
                    pricePerTicket = 150;
                    break;
                case "Middle Row (₹200)":
                    pricePerTicket = 200;
                    break;
                case "Back Row (₹180)":
                    pricePerTicket = 180;
                    break;
                default:
                    pricePerTicket = 200;
            }
            double totalPrice = pricePerTicket * ticketCount;

            // Create booking
            PreparedStatement bookingStmt = conn.prepareStatement(
                    "INSERT INTO bookings (booking_id, user_id, movie_id, show_id, tickets, seats, range_type, total_price) " +
                    "VALUES (booking_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)");
            bookingStmt.setInt(1, currentUserId);
            bookingStmt.setInt(2, movieId);
            bookingStmt.setInt(3, showId);
            bookingStmt.setInt(4, ticketCount);
            bookingStmt.setString(5, String.join(",", selectedSeats));
            bookingStmt.setString(6, selectedRange);
            bookingStmt.setDouble(7, totalPrice);
            bookingStmt.executeUpdate();

            // Update available seats
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE movies SET available_seats = available_seats - ? WHERE movie_id = ?");
            updateStmt.setInt(1, ticketCount);
            updateStmt.setInt(2, movieId);
            updateStmt.executeUpdate();

            // Show confirmation
            StringBuilder confirmation = new StringBuilder();
            confirmation.append("✅ Booking Confirmation\n");
            confirmation.append("----------------------------\n");
            confirmation.append("🎥 Movie: ").append(selectedMovie).append("\n");
            confirmation.append("⏰ Show Time: ").append(selectedShowTime).append("\n");
            confirmation.append("🪑 Range: ").append(selectedRange).append("\n");
            confirmation.append("🎟 Tickets: ").append(ticketCount).append("\n");
            confirmation.append("💰 Total Price: ₹").append(String.format("%.2f", totalPrice)).append("\n");
            confirmation.append("🪑 Seats: ").append(String.join(", ", selectedSeats)).append("\n");
            confirmation.append("----------------------------\n");

            bookingConfirmationArea.setText(confirmation.toString());
            JOptionPane.showMessageDialog(this, "Booking successful!");

            resetSeatSelection();
            loadMoviesForBooking();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error during booking.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MovieTicketSystemA::new);
    }
}