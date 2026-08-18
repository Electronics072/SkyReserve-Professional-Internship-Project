CREATE DATABASE IF NOT EXISTS skyreserve_db;
USE skyreserve_db;
-- Tables are created/updated by Spring Data JPA.
-- For a clean installation, create the database and run the application with the MySQL profile.

-- Professional build: payment simulation fields are added automatically by Hibernate (ddl-auto=update).
-- If maintaining an existing manual schema, add: payment_method VARCHAR(80) NOT NULL DEFAULT 'Demo UPI', payment_status VARCHAR(40) NOT NULL DEFAULT 'DEMO_PAID';
