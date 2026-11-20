package com.mycompany.bookbridge.service;

import com.mycompany.bookbridge.model.*;
import com.mycompany.bookbridge.exception.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryService {

    private final Map<String, LibraryBook> books =
            new HashMap<>();

    private final Map<String, Member> members =
            new HashMap<>();

    private final Map<String, LoanRecord> activeLoans =
            new HashMap<>();

    private final Queue<Reservation> reservations =
            new ArrayDeque<>();

    private final Set<String> checkedOutIsbns =
            new HashSet<>();

    private final List<LoanRecord> history =
            new ArrayList<>();

    private int nextLoanNumber = 1;

    public void registerMember(Member member) {
        Objects.requireNonNull(member, "Member required");

        if (members.containsKey(member.getMemberId())) {
            throw new DuplicateEntityException(
                    "Duplicate member ID: " + member.getMemberId());
        }

        members.put(member.getMemberId(), member);
    }

    public void addBook(LibraryBook book) {
        Objects.requireNonNull(book, "Book required");

        if (books.containsKey(book.getIsbn())) {
            throw new DuplicateEntityException(
                    "Duplicate ISBN: " + book.getIsbn());
        }

        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    "New catalog books must be available.");
        }

        books.put(book.getIsbn(), book);
    }

    public Optional<Member> findMember(String id) {
        return Optional.ofNullable(members.get(id));
    }

    public Optional<LibraryBook> findBook(String isbn) {
        return Optional.ofNullable(books.get(isbn));
    }

    public Optional<LoanRecord> findActiveLoan(String isbn) {
        return Optional.ofNullable(activeLoans.get(isbn));
    }

    public List<LibraryBook> searchByTitle(String keyword) {
        if (keyword == null) {
            throw new IllegalArgumentException(
                    "Keyword cannot be null.");
        }

        String query = keyword.trim().toLowerCase(Locale.ROOT);

        return books.values().stream()
                .filter(b -> b.getTitle()
                        .toLowerCase(Locale.ROOT)
                        .contains(query))
                .sorted(Comparator.comparing(
                        LibraryBook::getTitle)
                        .thenComparing(LibraryBook::getIsbn))
                .toList();
    }

    public List<LibraryBook> listBooks() {
        return books.values().stream()
                .sorted(Comparator.comparing(
                        LibraryBook::getIsbn))
                .toList();
    }

    public List<Member> listMembers() {
        return members.values().stream()
                .sorted(Comparator.comparing(
                        Member::getMemberId))
                .toList();
    }

    public List<LibraryBook> filterBooks(BookStatus status) {
        Objects.requireNonNull(status);

        return books.values().stream()
                .filter(b -> b.getStatus() == status)
                .sorted(Comparator.comparing(
                        LibraryBook::getIsbn))
                .toList();
    }

    public List<Member> filterMembers(MembershipType type) {
        Objects.requireNonNull(type);

        return members.values().stream()
                .filter(m -> m.getMembershipType() == type)
                .sorted(Comparator.comparing(
                        Member::getMemberId))
                .toList();
    }

    public List<LoanRecord> filterLoans(LoanStatus status) {
        Objects.requireNonNull(status);

        return history.stream()
                .filter(l -> l.getStatus() == status)
                .toList();
    }

    public LoanRecord issueLoan(String memberId,
            String isbn, int requestedDays) {

        return issueLoan(memberId, isbn,
                requestedDays, LocalDate.now());
    }

    public LoanRecord issueLoan(String memberId,
            String isbn, int requestedDays,
            LocalDate startDate) {

        Objects.requireNonNull(startDate, "Start date required");

        Member member = requireMember(memberId);
        LibraryBook book = requireBook(isbn);

        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException(
                    "Requested days must be 1-30.");
        }

        if (requestedDays > member.getMaximumLoanDays()) {
            throw new LoanLimitExceededException(
                    "Loan limit exceeded for "
                    + member.getMembershipType());
        }

        if (book.getStatus() != BookStatus.AVAILABLE
                || checkedOutIsbns.contains(isbn)
                || activeLoans.containsKey(isbn)) {
            throw new ItemUnavailableException(
                    "Book is unavailable: " + isbn);
        }

        String loanId = "L" + nextLoanNumber;

        LoanRecord loan = new LoanRecord(
                loanId, member, book, requestedDays, startDate);

        // Validate all conditions before changing collections.
        book.markBorrowed();
        checkedOutIsbns.add(isbn);
        activeLoans.put(isbn, loan);
        history.add(loan);
        nextLoanNumber++;

        return loan;
    }

    public LoanRecord returnBook(String isbn) {
        return returnBook(isbn, LocalDate.now());
    }

    public LoanRecord returnBook(String isbn,
            LocalDate returnDate) {

        Objects.requireNonNull(returnDate, "Return date required");

        LoanRecord loan = activeLoans.get(isbn);

        if (loan == null) {
            throw new ItemUnavailableException(
                    "No active loan for ISBN: " + isbn);
        }

        if (returnDate.isBefore(
                loan.getDueDate().minusDays(
                        loan.getRequestedDays()))) {
            throw new IllegalArgumentException(
                    "Return date precedes loan start.");
        }

        loan.markReturned(returnDate);
        loan.getBook().markReturned();
        activeLoans.remove(isbn);
        checkedOutIsbns.remove(isbn);

        return loan;
    }

    public Reservation reserveBook(String memberId,
            String isbn, int requestedDays) {

        Member member = requireMember(memberId);
        LibraryBook book = requireBook(isbn);

        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException(
                    "Requested days must be 1-30.");
        }

        if (requestedDays > member.getMaximumLoanDays()) {
            throw new LoanLimitExceededException(
                    "Reservation exceeds membership loan limit.");
        }

        if (book.getStatus() == BookStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Book is available; issue a loan instead.");
        }

        boolean duplicate = reservations.stream()
                .anyMatch(r ->
                        r.getMember().getMemberId().equals(memberId)
                        && r.getBook().getIsbn().equals(isbn));

        if (duplicate) {
            throw new DuplicateEntityException(
                    "Member already reserved this book.");
        }

        Reservation reservation =
                new Reservation(member, book, requestedDays);

        reservations.add(reservation);
        return reservation;
    }

    public Optional<LoanRecord> processNextReservation() {
        return processNextReservation(LocalDate.now());
    }

    public Optional<LoanRecord> processNextReservation(
            LocalDate date) {

        Objects.requireNonNull(date, "Date required");

        Reservation next = reservations.peek();

        if (next == null) {
            return Optional.empty();
        }

        String isbn = next.getBook().getIsbn();

        if (next.getBook().getStatus()
                != BookStatus.AVAILABLE) {
            return Optional.empty();
        }

        LoanRecord loan = issueLoan(
                next.getMember().getMemberId(),
                isbn,
                next.getRequestedDays(),
                date);

        reservations.remove();
        return Optional.of(loan);
    }

    public List<LoanRecord> getSortedLoanView() {
        return getSortedLoanView(LocalDate.now());
    }

    public List<LoanRecord> getSortedLoanView(LocalDate today) {
        Objects.requireNonNull(today);

        List<LoanRecord> copy = new ArrayList<>(history);

        copy.sort(
                Comparator.comparing(
                        (LoanRecord l) -> !l.isOverdue(today))
                        .thenComparing(LoanRecord::getDueDate)
                        .thenComparing(LoanRecord::getLoanId));

        return List.copyOf(copy);
    }

    public List<LoanRecord> listLoanHistory() {
        return List.copyOf(history);
    }

    public List<Reservation> listReservations() {
        return List.copyOf(reservations);
    }

    public CirculationSummary getSummary() {
        return getSummary(LocalDate.now());
    }

    public CirculationSummary getSummary(LocalDate today) {
        Objects.requireNonNull(today);

        long available = books.values().stream()
                .filter(b -> b.getStatus()
                        == BookStatus.AVAILABLE)
                .count();

        long checkedOut = checkedOutIsbns.size();

        Map<MembershipType, Long> byType =
                Arrays.stream(MembershipType.values())
                        .collect(Collectors.toMap(
                                type -> type,
                                type -> activeLoans.values()
                                        .stream()
                                        .filter(l -> l.getMember()
                                                .getMembershipType()
                                                == type)
                                        .count()));

        long fines = history.stream()
                .mapToLong(l -> l.getEstimatedFine(today))
                .sum();

        return new CirculationSummary(
                available, checkedOut, byType, fines);
    }

    public boolean isConsistent() {
        if (activeLoans.size() != checkedOutIsbns.size()) {
            return false;
        }

        for (String isbn : checkedOutIsbns) {
            if (!activeLoans.containsKey(isbn)) {
                return false;
            }
        }

        for (LibraryBook book : books.values()) {
            boolean tracked = checkedOutIsbns.contains(
                    book.getIsbn());

            if (tracked != (book.getStatus()
                    == BookStatus.CHECKED_OUT)) {
                return false;
            }
        }

        for (Map.Entry<String, LoanRecord> entry
                : activeLoans.entrySet()) {
            LoanRecord loan = entry.getValue();

            if (loan.getStatus() != LoanStatus.ACTIVE
                    || !entry.getKey().equals(
                            loan.getBook().getIsbn())) {
                return false;
            }
        }

        return true;
    }

    private Member requireMember(String id) {
        Member member = members.get(id);

        if (member == null) {
            throw new NoSuchElementException(
                    "Member not found: " + id);
        }

        return member;
    }

    private LibraryBook requireBook(String isbn) {
        LibraryBook book = books.get(isbn);

        if (book == null) {
            throw new NoSuchElementException(
                    "Book not found: " + isbn);
        }

        return book;
    }
}