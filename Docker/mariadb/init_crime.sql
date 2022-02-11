GRANT ALL PRIVILEGES ON *.* TO 'mariadb'@'%';
FlUSH PRIVILEGES;
CREATE TABLE IF NOT EXISTS crime (
    case_number INT UNSIGNED NOT NULL,
    land VARCHAR(255) NOT NULL,
    category VARCHAR(255) NULL,
    date DATE NULL,
    criminal_id INT UNSIGNED NOT NULL,
    victim_id INT UNSIGNED NULL,
    details VARCHAR(255) NULL,
    PRIMARY KEY (`case_number`)
);
LOAD DATA INFILE '/tmp/crime.csv' INTO TABLE crime FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' IGNORE 1 ROWS;

LOAD DATA INFILE '/tmp/crime.csv' INTO TABLE crime CHARACTER SET UTF8 FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' IGNORE 1 ROWS;