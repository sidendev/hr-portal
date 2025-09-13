-- H2 schema for integration tests

CREATE TABLE IF NOT EXISTS employees (
  id INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  mobile_number VARCHAR(255) NOT NULL,
  address VARCHAR(255),
  email_id BIGINT
);

CREATE TABLE IF NOT EXISTS emails (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id INT,
  address VARCHAR(255) NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deactivated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contracts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  employee_id INT NOT NULL,
  contract_type VARCHAR(255) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  full_time BOOLEAN NOT NULL DEFAULT TRUE,
  hours_per_week INT
);

ALTER TABLE contracts ADD CONSTRAINT IF NOT EXISTS fk_contracts_employee 
FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE;

ALTER TABLE emails ADD CONSTRAINT IF NOT EXISTS fk_emails_employee 
FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL;

ALTER TABLE employees ADD CONSTRAINT IF NOT EXISTS fk_employees_email 
FOREIGN KEY (email_id) REFERENCES emails(id) ON DELETE SET NULL;
