package com.example.libraryserver.book.service;

import com.example.libraryserver.book.data.Book;
import com.example.libraryserver.book.data.BookRepository;
import com.example.libraryserver.security.AuthenticatedUser;
import com.example.libraryserver.user.data.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class BookService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BookService.class);

  private final BookRepository bookRepository;
  private final UserRepository userRepository;
  private final IdGenerator idGenerator;

  public BookService(
      BookRepository bookRepository, UserRepository userRepository, IdGenerator idGenerator) {
    this.bookRepository = bookRepository;
    this.userRepository = userRepository;
    this.idGenerator = idGenerator;
  }

  public Optional<Book> findOneByIdentifier(UUID identifier) {
    LOGGER.trace("find book for identifier {}", identifier);
    return bookRepository.findOneByIdentifier(identifier);
  }

  public List<Book> findAll() {
    LOGGER.trace("find all books");
    return bookRepository.findAll();
  }

  @PreAuthorize("hasRole('LIBRARY_CURATOR')")
  @Transactional
  public Book save(Book book) {
    LOGGER.trace("Save book {}", book);

    if (book.getIdentifier() == null) {
      book.setIdentifier(idGenerator.generateId());
    }
    return bookRepository.save(book);
  }

  @PreAuthorize("hasRole('LIBRARY_USER')")
  @Transactional
  public Optional<Book> borrowForUser(
      UUID bookIdentifier, UUID userIdentifier, AuthenticatedUser authenticatedUser) {
    LOGGER.trace(
        "borrow book with identifier {} for user with identifier {}",
        bookIdentifier,
        userIdentifier);

    return bookRepository
        .findOneByIdentifier(bookIdentifier)
        .filter(
            b ->
                b.getBorrowedByUser() == null
                    && authenticatedUser != null
                    && userIdentifier.equals(authenticatedUser.getIdentifier()))
        .flatMap(
            b ->
                userRepository
                    .findOneByIdentifier(userIdentifier)
                    .map(
                        u -> {
                          b.setBorrowedByUser(u);
                          Book borrowedBook = bookRepository.save(b);
                          LOGGER.info("Borrowed book {} for user {}", borrowedBook, u);
                          return Optional.of(borrowedBook);
                        })
                    .orElse(Optional.empty()));
  }

  @PreAuthorize("hasRole('LIBRARY_USER')")
  @Transactional
  public Optional<Book> returnForUser(
      UUID bookIdentifier, UUID userIdentifier, AuthenticatedUser authenticatedUser) {
    LOGGER.trace(
        "return book with identifier {} of user with identifier {}",
        bookIdentifier,
        userIdentifier);

    return bookRepository
        .findOneByIdentifier(bookIdentifier)
        .filter(
            b ->
                b.getBorrowedByUser() != null
                    && authenticatedUser != null
                    && b.getBorrowedByUser().getIdentifier().equals(userIdentifier)
                    && b.getBorrowedByUser()
                        .getIdentifier()
                        .equals(authenticatedUser.getIdentifier()))
        .flatMap(
            b ->
                userRepository
                    .findOneByIdentifier(userIdentifier)
                    .map(
                        u -> {
                          b.setBorrowedByUser(null);
                          Book returnedBook = bookRepository.save(b);
                          LOGGER.info("Returned book {} for user {}", returnedBook, u);
                          return Optional.of(returnedBook);
                        })
                    .orElse(Optional.empty()));
  }

  @PreAuthorize("hasRole('LIBRARY_CURATOR')")
  @Transactional
  public boolean deleteOneByIdentifier(UUID bookIdentifier) {
    LOGGER.trace("delete book with identifier {}", bookIdentifier);

    return bookRepository
        .findOneByIdentifier(bookIdentifier)
        .map(
            b -> {
              bookRepository.delete(b);
              return true;
            })
        .orElse(false);
  }
}
