# --- !Ups

CREATE TABLE employees (
  id INT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  mobile_number VARCHAR(20) NOT NULL,
  address VARCHAR(255)
);

CREATE TABLE contracts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  contract_type ENUM('Permanent', 'Contract') NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  full_time BOOLEAN NOT NULL DEFAULT true,
  hours_per_week INT,
  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

# --- !Downs

DROP TABLE contracts;
DROP TABLE employees;

