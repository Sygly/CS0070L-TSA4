package com.mycompany.bookbridge.gui;

import com.mycompany.bookbridge.dao.ReportDAO;
import com.mycompany.bookbridge.dao.LoanDAO;
import com.mycompany.bookbridge.dao.MemberDAO;
import com.mycompany.bookbridge.dao.ReservationDAO;
import com.mycompany.bookbridge.model.Member;
import com.mycompany.bookbridge.model.StudentMember;
import com.mycompany.bookbridge.model.FacultyMember;
import com.mycompany.bookbridge.model.GuestMember;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import com.mycompany.bookbridge.dao.BookDAO;
import com.mycompany.bookbridge.model.LibraryBook;
import java.util.List;

public class BookBridgeFrame extends JFrame {

    private final JTabbedPane tabs = new JTabbedPane();
    private final ReportDAO reportDAO = new ReportDAO();
    private final LoanDAO loanDAO = new LoanDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final JLabel statusLabel =
            new JLabel("Ready - BookBridge TSA4");

    public BookBridgeFrame() {
        setTitle("BookBridge TSA4 - Library Management System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "BOOKBRIDGE - LIBRARY MANAGEMENT SYSTEM",
                SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        add(heading, BorderLayout.NORTH);

        tabs.addTab("Catalog", createCatalogPanel());
        tabs.addTab("Members", createMembersPanel());
        tabs.addTab("Circulation", createCirculationPanel());
        tabs.addTab("Reservations", createReservationsPanel());
        tabs.addTab("Reports", createReportsPanel());

        add(tabs, BorderLayout.CENTER);

        statusLabel.setBorder(
                BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(statusLabel, BorderLayout.SOUTH);
    }

    
private JPanel createCatalogPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel form = new JPanel(new GridLayout(2, 5, 8, 8));

    JTextField isbn = new JTextField();
    JTextField title = new JTextField();
    JTextField author = new JTextField();
    JTextField copies = new JTextField();

    JButton addButton = new JButton("Add Book");
    JButton searchButton = new JButton("Search");
    JButton refreshButton = new JButton("Refresh");

    form.add(new JLabel("ISBN"));
    form.add(new JLabel("Title"));
    form.add(new JLabel("Author"));
    form.add(new JLabel("Copies"));
    form.add(new JLabel("Actions"));

    form.add(isbn);
    form.add(title);
    form.add(author);
    form.add(copies);
    form.add(addButton);

    JPanel top = new JPanel(new BorderLayout(5, 5));
    top.add(form, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(searchButton);
    actions.add(refreshButton);
    top.add(actions, BorderLayout.SOUTH);

    panel.add(top, BorderLayout.NORTH);

    JTable table = createTable(
            "ISBN", "Title", "Author",
            "Total Copies", "Available Copies");

    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    DefaultTableModel model =
            (DefaultTableModel) table.getModel();

    // Loads books without blocking the Swing Event Dispatch Thread.
    java.util.function.Consumer<String> loadBooks = keyword -> {

        addButton.setEnabled(false);
        searchButton.setEnabled(false);
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading catalog...");

        new SwingWorker<List<LibraryBook>, Void>() {

            @Override
            protected List<LibraryBook> doInBackground()
                    throws Exception {

                if (keyword == null) {
                    return bookDAO.findAll();
                }

                return bookDAO.searchByTitle(keyword);
            }

            @Override
            protected void done() {
                try {
                    List<LibraryBook> books = get();

                    model.setRowCount(0);

                    for (LibraryBook book : books) {
                        model.addRow(new Object[]{
                            book.getIsbn(),
                            book.getTitle(),
                            book.getAuthor(),
                            book.getTotalCopies(),
                            book.getAvailableCopies()
                        });
                    }

                    statusLabel.setText(
                            "Catalog loaded: " + books.size() + " book(s)");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Catalog loading interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    statusLabel.setText("Catalog loading failed.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            ex.getCause().getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    addButton.setEnabled(true);
                    searchButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    };

    // Add Book button.
    addButton.addActionListener(event -> {

        String bookIsbn = isbn.getText().trim();
        String bookTitle = title.getText().trim();
        String bookAuthor = author.getText().trim();
        String copyText = copies.getText().trim();

        if (bookIsbn.isEmpty() || bookTitle.isEmpty()
                || bookAuthor.isEmpty() || copyText.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this, "All book fields are required.");
            return;
        }

        final int totalCopies;

        try {
            totalCopies = Integer.parseInt(copyText);

            if (totalCopies <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this, "Copies must be a positive whole number.");
            return;
        }

        addButton.setEnabled(false);
        statusLabel.setText("Saving book...");

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                bookDAO.addBook(new LibraryBook(
                        bookIsbn, bookTitle,
                        bookAuthor, totalCopies));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    statusLabel.setText("Book added successfully.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            "Book successfully saved to database.");

                    isbn.setText("");
                    title.setText("");
                    author.setText("");
                    copies.setText("");

                    loadBooks.accept(null);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Book save interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText("Failed to add book.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown database error."
                                    : cause.getMessage(),
                            "Add Book Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    addButton.setEnabled(true);
                }
            }
        }.execute();
    });

    // Search books by title.
    searchButton.addActionListener(event ->
            loadBooks.accept(title.getText().trim()));

    // Refresh the complete catalog.
    refreshButton.addActionListener(event ->
            loadBooks.accept(null));

    // Load existing database records when the tab is created.
    loadBooks.accept(null);

    return panel;
    }

  
private JPanel createMembersPanel() {

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));

    JTextField id = new JTextField();
    JTextField name = new JTextField();

    JComboBox<String> type = new JComboBox<>(
            new String[]{"STUDENT", "FACULTY", "GUEST"});

    JButton registerButton = new JButton("Register Member");
    JButton refreshButton = new JButton("Refresh Members");

    form.add(new JLabel("Member ID"));
    form.add(new JLabel("Full Name"));
    form.add(new JLabel("Membership Type"));
    form.add(new JLabel("Action"));

    form.add(id);
    form.add(name);
    form.add(type);
    form.add(registerButton);

    JPanel top = new JPanel(new BorderLayout(5, 5));
    top.add(form, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(refreshButton);

    top.add(actions, BorderLayout.SOUTH);
    panel.add(top, BorderLayout.NORTH);

    JTable table = createTable(
            "Member ID", "Full Name", "Membership Type");

    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    DefaultTableModel model =
            (DefaultTableModel) table.getModel();

    // Load members in the background.
    Runnable loadMembers = () -> {

        registerButton.setEnabled(false);
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading members...");

        new SwingWorker<List<Member>, Void>() {

            @Override
            protected List<Member> doInBackground()
                    throws Exception {
                return memberDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Member> members = get();

                    model.setRowCount(0);

                    for (Member member : members) {
                        model.addRow(new Object[]{
                            member.getMemberId(),
                            member.getMemberName(),
                            member.getMembershipType()
                        });
                    }

                    statusLabel.setText(
                            "Members loaded: "
                            + members.size());

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(
                            "Member loading interrupted.");

                } catch (ExecutionException ex) {
                    statusLabel.setText(
                            "Failed to load members.");

                    Throwable cause = ex.getCause();

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown error."
                                    : cause.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    registerButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    };

    // Register Member button.
    registerButton.addActionListener(event -> {

        String memberId = id.getText().trim();
        String memberName = name.getText().trim();
        String membershipType =
                (String) type.getSelectedItem();

        if (memberId.isEmpty() || memberName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Member ID and Full Name are required.");
            return;
        }

        final Member member;

        switch (membershipType) {
            case "STUDENT" ->
                member = new StudentMember(
                        memberId, memberName);

            case "FACULTY" ->
                member = new FacultyMember(
                        memberId, memberName);

            case "GUEST" ->
                member = new GuestMember(
                        memberId, memberName);

            default -> {
                JOptionPane.showMessageDialog(
                        this, "Invalid membership type.");
                return;
            }
        }

        registerButton.setEnabled(false);
        refreshButton.setEnabled(false);
        statusLabel.setText("Registering member...");

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground()
                    throws Exception {
                memberDAO.registerMember(member);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    statusLabel.setText(
                            "Member registered successfully.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            "Member successfully saved to database.");

                    id.setText("");
                    name.setText("");
                    type.setSelectedIndex(0);

                    loadMembers.run();

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(
                            "Member registration interrupted.");

                } catch (ExecutionException ex) {
                    statusLabel.setText(
                            "Failed to register member.");

                    Throwable cause = ex.getCause();

                    String message = cause == null
                            ? "Unknown error."
                            : cause.getMessage();

                    if (cause instanceof java.sql.SQLException sqlEx
                            && sqlEx.getErrorCode() == 1062) {
                        message = "Member ID already exists.";
                    }

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            message,
                            "Registration Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    registerButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    });

    refreshButton.addActionListener(event ->
            loadMembers.run());

    // Load existing members when the GUI opens.
    loadMembers.run();

    return panel;
}

   
private JPanel createCirculationPanel() {

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));

    JTextField memberId = new JTextField();
    JTextField isbn = new JTextField();
    JTextField days = new JTextField();

    JButton issueButton = new JButton("Issue Loan");
    JButton returnButton = new JButton("Return Book");
    JButton refreshButton = new JButton("Refresh Loans");

    form.add(new JLabel("Member ID"));
    form.add(new JLabel("ISBN"));
    form.add(new JLabel("Loan Days"));
    form.add(new JLabel("Action"));

    form.add(memberId);
    form.add(isbn);
    form.add(days);
    form.add(issueButton);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(returnButton);
    actions.add(refreshButton);

    JPanel top = new JPanel(new BorderLayout(5, 5));
    top.add(form, BorderLayout.CENTER);
    top.add(actions, BorderLayout.SOUTH);

    panel.add(top, BorderLayout.NORTH);

    JTable table = createTable(
            "Loan ID", "Member ID", "Name", "ISBN",
            "Book Title", "Issue Date", "Due Date",
            "Return Date", "Status");

    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    DefaultTableModel model =
            (DefaultTableModel) table.getModel();

    // Loads loan records without blocking the EDT.
    Runnable loadLoans = () -> {

        issueButton.setEnabled(false);
        returnButton.setEnabled(false);
        refreshButton.setEnabled(false);

        statusLabel.setText("Loading loans...");

        new SwingWorker<java.util.List<Object[]>, Void>() {

            @Override
            protected java.util.List<Object[]> doInBackground()
                    throws Exception {
                return loanDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    java.util.List<Object[]> loans = get();

                    model.setRowCount(0);

                    for (Object[] loan : loans) {
                        model.addRow(loan);
                    }

                    statusLabel.setText(
                            "Loans loaded: " + loans.size());

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(
                            "Loan loading interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText("Failed to load loans.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown database error."
                                    : cause.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    issueButton.setEnabled(true);
                    returnButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    };

    // Issue Loan button.
    issueButton.addActionListener(event -> {

        String id = memberId.getText().trim();
        String bookIsbn = isbn.getText().trim();
        String daysText = days.getText().trim();

        if (id.isEmpty() || bookIsbn.isEmpty()
                || daysText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "All loan fields are required.");
            return;
        }

        final int requestedDays;

        try {
            requestedDays = Integer.parseInt(daysText);

            if (requestedDays < 1 || requestedDays > 30) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this, "Loan days must be between 1 and 30.");
            return;
        }

        issueButton.setEnabled(false);
        returnButton.setEnabled(false);
        refreshButton.setEnabled(false);

        statusLabel.setText("Issuing loan...");

        new SwingWorker<Integer, Void>() {

            @Override
            protected Integer doInBackground()
                    throws Exception {
                return loanDAO.issueLoan(
                        id, bookIsbn, requestedDays);
            }

            @Override
            protected void done() {
                try {
                    int loanId = get();

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            "Loan issued successfully!\nLoan ID: "
                                    + loanId);

                    statusLabel.setText(
                            "Loan issued successfully.");

                    memberId.setText("");
                    isbn.setText("");
                    days.setText("");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Loan issue interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText("Failed to issue loan.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown error."
                                    : cause.getMessage(),
                            "Issue Loan Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    issueButton.setEnabled(true);
                    returnButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                    loadLoans.run();
                }
            }
        }.execute();
    });

    // Return Book button.
    returnButton.addActionListener(event -> {

        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a loan from the table first.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);

        int loanId = ((Number) model.getValueAt(
                modelRow, 0)).intValue();

        String status = String.valueOf(
                model.getValueAt(modelRow, 8));

        if (!"ACTIVE".equals(status)) {
            JOptionPane.showMessageDialog(
                    this, "This loan is already returned.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Return Loan ID " + loanId + "?",
                "Confirm Return",
                JOptionPane.YES_NO_OPTION);

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        issueButton.setEnabled(false);
        returnButton.setEnabled(false);
        refreshButton.setEnabled(false);

        statusLabel.setText("Returning book...");

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground()
                    throws Exception {
                loanDAO.returnBook(loanId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            "Book returned successfully.");

                    statusLabel.setText(
                            "Book returned successfully.");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Book return interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText("Failed to return book.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown error."
                                    : cause.getMessage(),
                            "Return Book Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    issueButton.setEnabled(true);
                    returnButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                    loadLoans.run();
                }
            }
        }.execute();
    });

    refreshButton.addActionListener(event -> loadLoans.run());

    loadLoans.run();

    return panel;
}

    
private JPanel createReservationsPanel() {

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel form = new JPanel(new GridLayout(2, 3, 8, 8));

    JTextField memberId = new JTextField();
    JTextField isbn = new JTextField();

    JButton reserveButton = new JButton("Reserve");
    JButton cancelButton = new JButton("Cancel Reservation");
    JButton refreshButton = new JButton("Refresh");

    form.add(new JLabel("Member ID"));
    form.add(new JLabel("ISBN"));
    form.add(new JLabel("Action"));

    form.add(memberId);
    form.add(isbn);
    form.add(reserveButton);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(cancelButton);
    actions.add(refreshButton);

    JPanel top = new JPanel(new BorderLayout(5, 5));
    top.add(form, BorderLayout.CENTER);
    top.add(actions, BorderLayout.SOUTH);

    panel.add(top, BorderLayout.NORTH);

    JTable table = createTable(
            "Reservation ID", "Member ID", "Name",
            "ISBN", "Book Title", "Date", "Status");

    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    DefaultTableModel model =
            (DefaultTableModel) table.getModel();

    // Load reservations in a background thread.
    Runnable loadReservations = () -> {

        reserveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        refreshButton.setEnabled(false);

        statusLabel.setText("Loading reservations...");

        new SwingWorker<java.util.List<Object[]>, Void>() {

            @Override
            protected java.util.List<Object[]> doInBackground()
                    throws Exception {
                return reservationDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    java.util.List<Object[]> records = get();

                    model.setRowCount(0);

                    for (Object[] record : records) {
                        model.addRow(record);
                    }

                    statusLabel.setText(
                            "Reservations loaded: " + records.size());

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(
                            "Reservation loading interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText(
                            "Failed to load reservations.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown database error."
                                    : cause.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    reserveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    };

    // Reserve button.
    reserveButton.addActionListener(event -> {

        String id = memberId.getText().trim();
        String bookIsbn = isbn.getText().trim();

        if (id.isEmpty() || bookIsbn.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Member ID and ISBN are required.");
            return;
        }

        reserveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        refreshButton.setEnabled(false);

        statusLabel.setText("Creating reservation...");

        new SwingWorker<Integer, Void>() {

            @Override
            protected Integer doInBackground()
                    throws Exception {
                return reservationDAO.reserveBook(id, bookIsbn);
            }

            @Override
            protected void done() {
                boolean success = false;

                try {
                    int reservationId = get();
                    success = true;

                    statusLabel.setText(
                            "Reservation created successfully.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            "Reservation created!\nReservation ID: "
                                    + reservationId);

                    memberId.setText("");
                    isbn.setText("");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(
                            "Reservation interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText(
                            "Failed to create reservation.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown error."
                                    : cause.getMessage(),
                            "Reservation Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    reserveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    refreshButton.setEnabled(true);

                    if (success) {
                        loadReservations.run();
                    }
                }
            }
        }.execute();
    });

    // Cancel Reservation button.
    cancelButton.addActionListener(event -> {

        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this, "Select a reservation first.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);

        int reservationId = ((Number) model.getValueAt(
                modelRow, 0)).intValue();

        String status = String.valueOf(
                model.getValueAt(modelRow, 6));

        if (!"WAITING".equals(status)
                && !"READY".equals(status)) {
            JOptionPane.showMessageDialog(
                    this, "This reservation cannot be cancelled.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Cancel Reservation ID " + reservationId + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION);

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        reserveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        refreshButton.setEnabled(false);

        statusLabel.setText("Cancelling reservation...");

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground()
                    throws Exception {
                reservationDAO.cancelReservation(reservationId);
                return null;
            }

            @Override
            protected void done() {
                boolean success = false;

                try {
                    get();
                    success = true;

                    statusLabel.setText(
                            "Reservation cancelled successfully.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            "Reservation cancelled successfully.");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(
                            "Cancellation interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    statusLabel.setText(
                            "Failed to cancel reservation.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown error."
                                    : cause.getMessage(),
                            "Cancellation Error",
                            JOptionPane.ERROR_MESSAGE);

                } finally {
                    reserveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    refreshButton.setEnabled(true);

                    if (success) {
                        loadReservations.run();
                    }
                }
            }
        }.execute();
    });

    refreshButton.addActionListener(event ->
            loadReservations.run());

    loadReservations.run();

    return panel;
}

    
private JPanel createReportsPanel() {

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JButton generateButton = new JButton("Generate Report");

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(generateButton);

    panel.add(top, BorderLayout.NORTH);

    JTextArea reportArea = new JTextArea();
    reportArea.setEditable(false);
    reportArea.setFont(
            new Font("Monospaced", Font.PLAIN, 14));

    panel.add(
            new JScrollPane(reportArea),
            BorderLayout.CENTER);

    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(false);
    panel.add(progressBar, BorderLayout.SOUTH);

    generateButton.addActionListener(event -> {

        generateButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Generating circulation report...");
        reportArea.setText("Loading report from database...");

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground()
                    throws Exception {

                return reportDAO.generateReport();
            }

            @Override
            protected void done() {

                progressBar.setIndeterminate(false);
                generateButton.setEnabled(true);

                try {
                    String report = get();

                    reportArea.setText(report);
                    reportArea.setCaretPosition(0);

                    statusLabel.setText(
                            "Report generated successfully.");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                    reportArea.setText(
                            "Report generation interrupted.");

                    statusLabel.setText(
                            "Report interrupted.");

                } catch (java.util.concurrent.ExecutionException ex) {

                    Throwable cause = ex.getCause();

                    reportArea.setText(
                            "Unable to generate report.");

                    statusLabel.setText(
                            "Report generation failed.");

                    JOptionPane.showMessageDialog(
                            BookBridgeFrame.this,
                            cause == null
                                    ? "Unknown database error."
                                    : cause.getMessage(),
                            "Report Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    });

    return panel;
}

    private JTable createTable(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        return table;
    }
}