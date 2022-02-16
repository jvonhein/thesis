CREATE EXTENSION odbc_fdw;
CREATE SERVER mdb_criminals FOREIGN DATA WRAPPER odbc_fdw OPTIONS (dsn 'mdb_criminals');
CREATE SERVER mdb_crime FOREIGN DATA WRAPPER odbc_fdw OPTIONS (dsn 'mdb_crime');
CREATE ROLE odbc_user SUPERUSER LOGIN PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE db1 TO odbc_user;
CREATE USER MAPPING FOR odbc_user SERVER mdb_criminals OPTIONS (odbc_UID 'root', odbc_PWD '');
CREATE USER MAPPING FOR odbc_user SERVER mdb_crime OPTIONS (odbc_UID 'root', odbc_PWD '');
CREATE USER MAPPING FOR postgres SERVER mdb_criminals OPTIONS (odbc_UID 'root', odbc_PWD '');
CREATE USER MAPPING FOR postgres SERVER mdb_crime OPTIONS (odbc_UID 'root', odbc_PWD '');

CREATE TABLE country_stats (
bundesland VARCHAR(255),
population INTEGER,
size_in_km INTEGER,
dummy_data VARCHAR(255)
);

COPY country_stats(bundesland, population, size_in_km, dummy_data)
FROM '/tables/country_stats.csv'
DELIMITER ';'
CSV HEADER;

CREATE TABLE vaccine_data (
batch_number VARCHAR(255) PRIMARY KEY,
shot_number INTEGER,
date DATE,
dummy_data VARCHAR(255),
bundesland VARCHAR(255)
);



