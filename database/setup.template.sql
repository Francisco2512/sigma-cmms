-- Crea la base de datos y un usuario con privilegios solo sobre ella.
-- Ejecutar como root:  mysql -u root -p < database/setup-local.sql
CREATE DATABASE IF NOT EXISTS sigma_cmms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS sigma_cmms_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'sigma'@'localhost' IDENTIFIED BY '{{DB_PASSWORD}}';
ALTER USER 'sigma'@'localhost' IDENTIFIED BY '{{DB_PASSWORD}}';
GRANT ALL PRIVILEGES ON sigma_cmms.* TO 'sigma'@'localhost';
GRANT ALL PRIVILEGES ON sigma_cmms_test.* TO 'sigma'@'localhost';
FLUSH PRIVILEGES;
