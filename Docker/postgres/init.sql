#CREATE TABLE health (
#bundesland VARCHAR(255),
#population_total INTEGER,
#vaccinated_firstshot INTEGER,
#vaccinated_secondshot INTEGER,
#percent_vaccinated_firstshot NUMERIC(4,3),
#percent_vaccinated_secondshot NUMERIC(4,3)
#);
#
#
#COPY health(bundesland, population_total, vaccinated_firstshot, vaccinated_secondshot, percent_vaccinated_firstshot, percent_vaccinated_secondshot)
#FROM '/tables/health.csv'
#DELIMITER ';'
#CSV HEADER;

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
