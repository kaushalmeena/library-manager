-- Demo data, inserted only when the database is created for the first time.
--
-- Every seeded account uses the password: password123
-- The bcrypt hash below is the same for all of them purely for convenience.

INSERT INTO users (name, email, mobile, username, password_hash, role, created_date) VALUES
    ('Ada Lovelace',    'ada@library.test',    '9876543210', 'admin',     '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'ADMIN',     date('now', '-420 days')),
    ('Rita Levi',       'rita@library.test',   '9876543211', 'librarian', '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'LIBRARIAN', date('now', '-390 days')),
    ('Grace Hopper',    'grace@library.test',  '9876543212', 'grace',     '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'LIBRARIAN', date('now', '-300 days')),
    ('Alan Turing',     'alan@library.test',   '9876543213', 'student',   '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'STUDENT',   date('now', '-240 days')),
    ('Katherine J.',    'kate@library.test',   '9876543214', 'kate',      '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'STUDENT',   date('now', '-180 days')),
    ('Linus T.',        'linus@library.test',  '9876543215', 'linus',     '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'STUDENT',   date('now', '-90 days')),
    ('Margaret H.',     'maggie@library.test', '9876543216', 'maggie',    '$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq', 'STUDENT',   date('now', '-30 days'));

INSERT INTO books (isbn, title, author, publisher, published_year, cover_url, price, total_copies, added_date) VALUES
    ('9780134685991', 'Effective Java',                            'Joshua Bloch',       'Addison-Wesley Professional', 2017, 'https://covers.openlibrary.org/b/isbn/9780134685991-M.jpg',  45.99, 4, date('now', '-400 days')),
    ('9780132350884', 'Clean Code',                                 'Robert C. Martin',   'Prentice Hall',               2008, 'https://covers.openlibrary.org/b/isbn/9780132350884-M.jpg',  39.50, 3, date('now', '-380 days')),
    ('9780201633610', 'Design Patterns',                            'Erich Gamma',        'Addison-Wesley',              1994, 'https://covers.openlibrary.org/b/isbn/9780201633610-M.jpg',  54.00, 2, date('now', '-360 days')),
    ('9780321356680', 'Java Concurrency in Practice',               'Brian Goetz',        'Addison-Wesley',              2006, 'https://covers.openlibrary.org/b/isbn/9780321356680-M.jpg',  49.99, 2, date('now', '-300 days')),
    ('9781617292545', 'Spring in Action',                           'Craig Walls',        'Manning',                     2018, 'https://covers.openlibrary.org/b/isbn/9781617292545-M.jpg',  44.95, 3, date('now', '-260 days')),
    ('9780596009205', 'Head First Design Patterns',                 'Eric Freeman',       'O''Reilly Media',             2004, 'https://covers.openlibrary.org/b/isbn/9780596009205-M.jpg',  37.25, 5, date('now', '-240 days')),
    ('9780262033848', 'Introduction to Algorithms',                 'Thomas H. Cormen',   'MIT Press',                   2009, 'https://covers.openlibrary.org/b/isbn/9780262033848-M.jpg',  89.00, 3, date('now', '-200 days')),
    ('9780137081073', 'The Clean Coder',                            'Robert C. Martin',   'Prentice Hall',               2011, 'https://covers.openlibrary.org/b/isbn/9780137081073-M.jpg',  34.99, 2, date('now', '-160 days')),
    ('9780321125215', 'Domain-Driven Design',                       'Eric Evans',         'Addison-Wesley',              2003, 'https://covers.openlibrary.org/b/isbn/9780321125215-M.jpg',  64.50, 2, date('now', '-120 days')),
    ('9781449331818', 'Learning JavaScript Design Patterns',        'Addy Osmani',        'O''Reilly Media',             2012, 'https://covers.openlibrary.org/b/isbn/9781449331818-M.jpg',  29.99, 2, date('now', '-100 days')),
    ('9780596517748', 'JavaScript: The Good Parts',                 'Douglas Crockford',  'O''Reilly Media',             2008, 'https://covers.openlibrary.org/b/isbn/9780596517748-M.jpg',  24.99, 3, date('now', '-80 days')),
    ('9781491950296', 'Building Microservices',                     'Sam Newman',         'O''Reilly Media',             2015, 'https://covers.openlibrary.org/b/isbn/9781491950296-M.jpg',  49.00, 2, date('now', '-60 days')),
    ('9780321751041', 'Refactoring',                                'Martin Fowler',      'Addison-Wesley',              2012, 'https://covers.openlibrary.org/b/isbn/9780321751041-M.jpg',  54.99, 2, date('now', '-40 days')),
    ('9781593279509', 'Eloquent JavaScript',                        'Marijn Haverbeke',   'No Starch Press',             2018, 'https://covers.openlibrary.org/b/isbn/9781593279509-M.jpg',  27.50, 4, date('now', '-20 days')),
    ('9780134494166', 'Clean Architecture',                         'Robert C. Martin',   'Prentice Hall',               2017, 'https://covers.openlibrary.org/b/isbn/9780134494166-M.jpg',  41.75, 3, date('now', '-10 days'));

-- Circulation history: returned on time, returned late, currently out, and overdue.
INSERT INTO loans (book_id, user_id, issue_date, due_date, return_date, fine_paid) VALUES
    (1, 4, date('now', '-200 days'), date('now', '-186 days'), date('now', '-190 days'), 0),
    (2, 4, date('now', '-170 days'), date('now', '-156 days'), date('now', '-150 days'), 12.0),
    (1, 5, date('now', '-160 days'), date('now', '-146 days'), date('now', '-149 days'), 0),
    (7, 5, date('now', '-140 days'), date('now', '-126 days'), date('now', '-126 days'), 0),
    (3, 6, date('now', '-120 days'), date('now', '-106 days'), date('now', '-100 days'), 12.0),
    (6, 4, date('now',  '-90 days'), date('now',  '-76 days'), date('now',  '-80 days'), 0),
    (2, 5, date('now',  '-70 days'), date('now',  '-56 days'), date('now',  '-60 days'), 0),
    (11, 6, date('now', '-60 days'), date('now',  '-46 days'), date('now',  '-44 days'), 4.0),
    (13, 7, date('now', '-45 days'), date('now',  '-31 days'), date('now',  '-35 days'), 0),
    (1, 6, date('now',  '-30 days'), date('now',  '-16 days'), date('now',  '-20 days'), 0),
    -- Overdue, still out
    (2, 6, date('now',  '-40 days'), date('now',  '-26 days'), NULL, 0),
    (7, 4, date('now',  '-30 days'), date('now',  '-16 days'), NULL, 0),
    (9, 7, date('now',  '-24 days'), date('now',  '-10 days'), NULL, 0),
    -- Currently out, within the loan period
    (5, 5, date('now',   '-8 days'), date('now',   '+6 days'), NULL, 0),
    (14, 4, date('now',  '-5 days'), date('now',   '+9 days'), NULL, 0),
    (15, 6, date('now',  '-3 days'), date('now',  '+11 days'), NULL, 0),
    (4, 7, date('now',  '-12 days'), date('now',   '+2 days'), NULL, 0),
    (12, 5, date('now',  '-1 days'), date('now',  '+13 days'), NULL, 0);
