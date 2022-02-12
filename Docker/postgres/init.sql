CREATE EXTENSION odbc_fdw;
CREATE SERVER mdb_criminals FOREIGN DATA WRAPPER odbc_fdw OPTIONS (dsn 'mdb_criminals');
CREATE USER MAPPING FOR postgres SERVER mdb_criminals OPTIONS (odbc_UID 'root', odbc_PWD '');
CREATE SERVER mdb_crime FOREIGN DATA WRAPPER odbc_fdw OPTIONS (dsn 'mdb_crime');
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

CREATE ROLE odbc_user SUPERUSER LOGIN PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE db1 TO odbc_user;


