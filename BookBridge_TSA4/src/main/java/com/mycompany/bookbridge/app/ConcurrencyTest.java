package com.mycompany.bookbridge.app;

import com.mycompany.bookbridge.dao.BookDAO;
import com.mycompany.bookbridge.dao.LoanDAO;
import com.mycompany.bookbridge.dao.MemberDAO;
import com.mycompany.bookbridge.model.*;

import java.sql.SQLException;
import java.util.concurrent.*;

public class ConcurrencyTest {

    public static void main(String[] args) {

        System.out.println(
                "========== TSA4 CONCURRENCY TEST ==========");

        String isbn = "TSA4-CONC-001";
        String member1 = "CONC-M001";
        String member2 = "CONC-M002";

        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        try {
            // Dedicated test records.
            memberDAO.registerMember(
                    new StudentMember(member1, "Concurrency Student"));

            memberDAO.registerMember(
                    new FacultyMember(member2, "Concurrency Faculty"));

            bookDAO.addBook(new LibraryBook(
                    isbn, "Concurrency Test Book",
                    "BookBridge Testing", 1));

            System.out.println("Test data created successfully.");

        } catch (SQLException ex) {
            System.out.println(
                    "Setup failed: " + ex.getMessage());
            System.out.println(
                    "Use a new test ISBN/member IDs before retrying.");
            return;
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> request1 = () ->
                attemptLoan(loanDAO, member1, isbn, ready, start);

        Callable<Boolean> request2 = () ->
                attemptLoan(loanDAO, member2, isbn, ready, start);

        try {
            Future<Boolean> first = executor.submit(request1);
            Future<Boolean> second = executor.submit(request2);

            ready.await();
            start.countDown();

            boolean result1 = first.get();
            boolean result2 = second.get();

            int successfulRequests =
                    (result1 ? 1 : 0) + (result2 ? 1 : 0);

            LibraryBook book = bookDAO.findByIsbn(isbn)
                    .orElseThrow();

            long activeLoans = loanDAO.findAll().stream()
                    .filter(row -> isbn.equals(row[3]))
                    .filter(row -> "ACTIVE".equals(row[8]))
                    .count();

            boolean passed = successfulRequests == 1
                    && book.getAvailableCopies() == 0
                    && activeLoans == 1;

            System.out.println(
                    "\n========== CONCURRENCY RESULT ==========");
            System.out.println(
                    "Successful Loan Requests: "
                    + successfulRequests);
            System.out.println(
                    "Available Copies: "
                    + book.getAvailableCopies());
            System.out.println(
                    "Active Loans for Test Book: "
                    + activeLoans);
            System.out.println(
                    "Overall Concurrency Test: "
                    + (passed ? "PASS" : "FAIL"));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.println("Test interrupted.");

        } catch (ExecutionException | SQLException ex) {
            System.out.println(
                    "Test failed: " + ex.getMessage());

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private static boolean attemptLoan(
            LoanDAO loanDAO,
            String memberId,
            String isbn,
            CountDownLatch ready,
            CountDownLatch start) {

        ready.countDown();

        try {
            start.await();

            int loanId = loanDAO.issueLoan(
                    memberId, isbn, 7);

            System.out.println(
                    memberId + " SUCCESS - Loan ID: " + loanId);

            return true;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;

        } catch (SQLException | IllegalStateException ex) {
            System.out.println(
                    memberId + " REJECTED - " + ex.getMessage());
            return false;
        }
    }
}