# --- !Ups
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  email VARCHAR(255) UNIQUE
);

# --- !Downs
DROP TABLE users;
