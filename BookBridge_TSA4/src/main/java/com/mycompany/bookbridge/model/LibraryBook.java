package com.mycompany.bookbridge.model;

public class LibraryBook {

    private final String isbn;
    private final String title;
    private final String author;
    private final int totalCopies;
    private int availableCopies;

    // Constructor compatible with the original TSA3 model.
    public LibraryBook(String isbn, String title) {
        this(isbn, title, "Unknown", 1, 1);
    }

    // Constructor for creating a new catalog book.
    public LibraryBook(String isbn, String title,
                       String author, int totalCopies) {
        this(isbn, title, author, totalCopies, totalCopies);
    }

    // Constructor for loading existing book data from JDBC.
    public LibraryBook(String isbn, String title,
                       String author, int totalCopies,
                       int availableCopies) {

        if (isbn == null || isbn.isBlank()
                || title == null || title.isBlank()
                || author == null || author.isBlank()) {
            throw new IllegalArgumentException(
                    "ISBN, title, and author are required.");
        }

        if (totalCopies < 1 || availableCopies < 0
                || availableCopies > totalCopies) {
            throw new IllegalArgumentException(
                    "Invalid book copy counts.");
        }

        this.isbn = isbn.trim();
        this.title = title.trim();
        this.author = author.trim();
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public BookStatus getStatus() {
        return availableCopies > 0
                ? BookStatus.AVAILABLE
                : BookStatus.CHECKED_OUT;
    }

    public void markBorrowed() {
        if (availableCopies <= 0) {
            throw new IllegalStateException(
                    "No available copies.");
        }
        availableCopies--;
    }

    public void markReturned() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException(
                    "All copies are already available.");
        }
        availableCopies++;
    }
}