CREATE DATABASE IF NOT EXISTS bookbridge_tsa4_db;
USE bookbridge_tsa4_db;

CREATE TABLE members (
    member_id VARCHAR(30) PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    membership_type ENUM('STUDENT', 'FACULTY', 'GUEST') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (TRIM(member_id) <> ''),
    CHECK (TRIM(full_name) <> '')
) ENGINE=InnoDB;

CREATE TABLE books (
    isbn VARCHAR(20) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (total_copies > 0),
    CHECK (available_copies >= 0),
    CHECK (available_copies <= total_copies),
    CHECK (TRIM(isbn) <> ''),
    CHECK (TRIM(title) <> '')
) ENGINE=InnoDB;

CREATE TABLE loans (
    loan_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(30) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE DEFAULT NULL,
    status ENUM('ACTIVE', 'RETURNED')
        NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (member_id)
        REFERENCES members(member_id),

    FOREIGN KEY (isbn)
        REFERENCES books(isbn),

    CHECK (due_date >= issue_date),
    CHECK (return_date IS NULL OR return_date >= issue_date)
) ENGINE=InnoDB;

CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(30) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    reservation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM(
        'WAITING',
        'READY',
        'FULFILLED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'WAITING',

    FOREIGN KEY (member_id)
        REFERENCES members(member_id),

    FOREIGN KEY (isbn)
        REFERENCES books(isbn)
) ENGINE=InnoDB;
