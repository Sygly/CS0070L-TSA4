BOOKBRIDGE TSA4 — SETUP INSTRUCTIONS

Project Name: BookBridge_TSA4  
Programming Language: Java  
IDE: Apache NetBeans  
Database: MariaDB (XAMPP)  
Build Tool: Apache Maven

SYSTEM SETUP

1. Install a compatible JDK (Java 26 for the current project), Apache NetBeans, and XAMPP.
2. Open XAMPP Control Panel and start MySQL.
3. Open phpMyAdmin at http://localhost/phpmyadmin.
4. Import the provided schema.sql file to create the bookbridge_tsa4_db database and its four tables.
5. Open the BookBridge_TSA4 Maven project in NetBeans.
6. Allow Maven to download the MariaDB JDBC driver dependency.
7. Configure the database connection using BOOKBRIDGE_DB_USER and BOOKBRIDGE_DB_PASSWORD environment variables if the MariaDB account does not use the local defaults.
8. Ensure MariaDB is accessible on localhost port 3306, or adjust the JDBC URL in DatabaseConnection.java.
9. Right-click the project and select Clean and Build.
10. Run BookBridgeApp.java to launch the Swing desktop application.

APPLICATION FEATURES
- Catalog: Add, search, and view books.
- Members: Register and view members.
- Circulation: Issue and return books.
- Reservations: Create, cancel, and view reservations.
- Reports: Generate library circulation statistics.

DATABASE TABLES

- members
- books
- loans
- reservations

TESTING

The application was tested for book and member registration, loan processing, returns, reservations, validation errors, database recovery, and concurrent borrowing.

Refer to BookBridge_TSA4_Documentation.pdf for the test results and screenshots.