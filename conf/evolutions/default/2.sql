# --- !Ups

-- unique emails never to be used by updates or new employees
CREATE TABLE emails (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_id BIGINT NULL,
  address VARCHAR(255) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deactivated_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY ux_emails_address (address),
  INDEX ix_emails_employee_id (employee_id),
  CONSTRAINT fk_emails_employee
    FOREIGN KEY (employee_id) REFERENCES employees(id)
    ON DELETE SET NULL
);

-- link to employees
ALTER TABLE employees
  ADD COLUMN email_id BIGINT NULL,
  ADD INDEX ix_employees_email_id (email_id),
  ADD CONSTRAINT fk_employees_email
    FOREIGN KEY (email_id) REFERENCES emails(id)
    ON DELETE SET NULL;

# --- !Downs

ALTER TABLE employees DROP FOREIGN KEY fk_employees_email;
ALTER TABLE employees DROP COLUMN email_id;
DROP TABLE emails;
