package com.example.library.service;

import com.example.library.model.Loan;
import com.example.library.model.LoanDetail;
import com.example.library.model.LoanStatus;
import com.example.library.model.User;
import com.example.library.support.TestLibrary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CirculationServiceTest {

    private TestLibrary library;
    private User member;
    private long bookId;

    @BeforeEach
    void setUp() {
        library = TestLibrary.empty();
        member = library.addMember("alan");
        bookId = library.addBook("Effective Java", 2);
    }

    @AfterEach
    void tearDown() {
        library.close();
    }

    @Nested
    @DisplayName("issuing")
    class Issuing {

        @Test
        @DisplayName("sets a due date one loan period out")
        void setsDueDate() {
            Loan loan = library.circulation.issue(bookId, member.id());

            assertEquals(library.today(), loan.issueDate());
            assertEquals(library.today().plusDays(library.config.loanDays()), loan.dueDate());
            assertNull(loan.returnDate());
            assertEquals(0, loan.renewals());
        }

        @Test
        @DisplayName("reduces the copies shown as available")
        void reducesAvailability() {
            library.circulation.issue(bookId, member.id());

            var summary = library.books.findSummaryById(bookId).orElseThrow();
            assertEquals(1, summary.onLoan());
            assertEquals(1, summary.available());
            assertEquals("1 of 2", summary.availabilityLabel());
        }

        @Test
        @DisplayName("refuses once every copy is out")
        void refusesWhenNoCopiesLeft() {
            User second = library.addMember("grace");
            User third = library.addMember("linus");
            library.circulation.issue(bookId, member.id());
            library.circulation.issue(bookId, second.id());

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.issue(bookId, third.id()));

            assertTrue(thrown.getMessage().contains("currently on loan"), thrown.getMessage());
            assertEquals(2, library.loans.countOutstandingForBook(bookId));
        }

        @Test
        @DisplayName("refuses a second copy of a title the member already holds")
        void refusesDuplicateTitle() {
            library.circulation.issue(bookId, member.id());

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.issue(bookId, member.id()));

            assertTrue(thrown.getMessage().contains("already holding"), thrown.getMessage());
        }

        @Test
        @DisplayName("refuses past the per-member loan limit")
        void refusesPastLoanLimit() {
            for (int i = 0; i < library.config.maxLoansPerMember(); i++) {
                library.circulation.issue(library.addBook("Title " + i, 1), member.id());
            }
            long oneMore = library.addBook("One Too Many", 1);

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.issue(oneMore, member.id()));

            assertTrue(thrown.getMessage().contains("limit is"), thrown.getMessage());
        }

        @Test
        @DisplayName("refuses while the member has something overdue")
        void refusesWithOverdueBooks() {
            library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 1);
            long another = library.addBook("Clean Code", 1);

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.issue(another, member.id()));

            assertTrue(thrown.getMessage().contains("overdue"), thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("returning")
    class Returning {

        @Test
        @DisplayName("keeps the loan as history rather than deleting it")
        void keepsHistory() {
            Loan loan = library.circulation.issue(bookId, member.id());

            library.circulation.returnBook(loan.id(), true);

            Loan stored = library.loans.findById(loan.id()).orElseThrow();
            assertEquals(library.today(), stored.returnDate());
            assertEquals(1, library.circulation.loansForUser(member.id()).size());
            assertEquals(0, library.loans.countOutstandingForBook(bookId));
        }

        @Test
        @DisplayName("frees the copy for the next borrower")
        void freesTheCopy() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.circulation.returnBook(loan.id(), true);

            var summary = library.books.findSummaryById(bookId).orElseThrow();
            assertEquals(0, summary.onLoan());
            assertEquals(2, summary.available());
            assertEquals(1, summary.timesIssued(), "the loan still counts towards history");
        }

        @Test
        @DisplayName("charges nothing when returned on time")
        void noFineWhenOnTime() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays());

            var receipt = library.circulation.returnBook(loan.id(), true);

            assertEquals(0, receipt.fine().signum());
            assertFalse(receipt.wasLate());
        }

        @Test
        @DisplayName("charges the daily rate for each late day")
        void chargesFineWhenLate() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 5);

            var receipt = library.circulation.returnBook(loan.id(), true);

            assertEquals(5, receipt.daysLate());
            assertEquals(new BigDecimal("10.00"), receipt.fine());
            assertTrue(receipt.wasLate());
        }

        @Test
        @DisplayName("rejects a second return of the same loan")
        void rejectsDoubleReturn() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.circulation.returnBook(loan.id(), true);

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.returnBook(loan.id(), true));

            assertTrue(thrown.getMessage().contains("already been returned"), thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("renewing")
    class Renewing {

        @Test
        @DisplayName("pushes the due date out by another loan period")
        void extendsDueDate() {
            Loan loan = library.circulation.issue(bookId, member.id());
            LocalDate originalDue = loan.dueDate();

            Loan renewed = library.circulation.renew(loan.id());

            assertEquals(originalDue.plusDays(library.config.loanDays()), renewed.dueDate());
            assertEquals(1, renewed.renewals());
        }

        @Test
        @DisplayName("refuses beyond the renewal limit")
        void refusesPastRenewalLimit() {
            Loan loan = library.circulation.issue(bookId, member.id());
            for (int i = 0; i < library.config.maxRenewals(); i++) {
                library.circulation.renew(loan.id());
            }

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.renew(loan.id()));

            assertTrue(thrown.getMessage().contains("limit is"), thrown.getMessage());
        }

        @Test
        @DisplayName("refuses an overdue loan")
        void refusesOverdueLoan() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 1);

            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> library.circulation.renew(loan.id()));

            assertTrue(thrown.getMessage().contains("overdue"), thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("status and fines")
    class StatusAndFines {

        @Test
        @DisplayName("moves a loan through on-loan, due-soon and overdue")
        void statusProgression() {
            Loan loan = library.circulation.issue(bookId, member.id());
            LocalDate issued = library.today();

            assertEquals(LoanStatus.ON_LOAN,
                    loan.status(issued, CirculationService.DUE_SOON_WINDOW_DAYS));
            assertEquals(LoanStatus.DUE_SOON,
                    loan.status(loan.dueDate().minusDays(1), CirculationService.DUE_SOON_WINDOW_DAYS));
            assertEquals(LoanStatus.OVERDUE,
                    loan.status(loan.dueDate().plusDays(1), CirculationService.DUE_SOON_WINDOW_DAYS));
        }

        @Test
        @DisplayName("marks a late return as returned late")
        void returnedLateStatus() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 2);
            library.circulation.returnBook(loan.id(), true);

            LoanDetail detail = library.circulation.loansForUser(member.id()).get(0);
            assertEquals(LoanStatus.RETURNED_LATE,
                    detail.status(library.today(), CirculationService.DUE_SOON_WINDOW_DAYS));
        }

        @Test
        @DisplayName("accumulates a fine while the copy is still out")
        void fineGrowsWhileOverdue() {
            library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 3);

            assertEquals(new BigDecimal("6.00"),
                    library.circulation.outstandingFineForUser(member.id()));

            library.advanceDays(2);
            assertEquals(new BigDecimal("10.00"),
                    library.circulation.outstandingFineForUser(member.id()));
        }

        @Test
        @DisplayName("settling a fine clears what the member owes")
        void payingFineClearsBalance() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 4);
            library.circulation.returnBook(loan.id(), false);

            assertEquals(new BigDecimal("8.00"),
                    library.circulation.outstandingFineForUser(member.id()));

            library.circulation.payFine(loan.id(), new BigDecimal("8.00"));

            assertEquals(0, library.circulation.outstandingFineForUser(member.id()).signum());
        }

        @Test
        @DisplayName("refuses to collect more than is owed")
        void refusesOverpayment() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() + 1);
            library.circulation.returnBook(loan.id(), false);

            assertThrows(ValidationException.class,
                    () -> library.circulation.payFine(loan.id(), new BigDecimal("99.00")));
        }

        @Test
        @DisplayName("lists overdue loans separately from those merely due soon")
        void listsOverdueAndDueSoon() {
            library.circulation.issue(bookId, member.id());
            library.advanceDays(library.config.loanDays() - 1);

            List<LoanDetail> dueSoon = library.circulation.loansDueSoon();
            assertEquals(1, dueSoon.size());
            assertTrue(library.circulation.overdueLoans().isEmpty());

            library.advanceDays(2);
            assertTrue(library.circulation.loansDueSoon().isEmpty());
            assertEquals(1, library.circulation.overdueLoans().size());
        }
    }

    @Nested
    @DisplayName("deletion guards")
    class DeletionGuards {

        @Test
        @DisplayName("blocks removing a title while copies are out")
        void blocksBookDeletionWhileOnLoan() {
            library.circulation.issue(bookId, member.id());

            assertThrows(ValidationException.class, () -> library.catalogue.delete(bookId));
            assertNotNull(library.books.findById(bookId).orElse(null));
        }

        @Test
        @DisplayName("allows removing a title once everything is back")
        void allowsBookDeletionAfterReturn() {
            Loan loan = library.circulation.issue(bookId, member.id());
            library.circulation.returnBook(loan.id(), true);

            library.catalogue.delete(bookId);

            assertTrue(library.books.findById(bookId).isEmpty());
        }

        @Test
        @DisplayName("blocks removing a member holding books")
        void blocksUserDeletionWhileHoldingBooks() {
            library.circulation.issue(bookId, member.id());

            assertThrows(ValidationException.class,
                    () -> library.circulation.checkUserCanBeDeleted(member.id()));
        }
    }
}
