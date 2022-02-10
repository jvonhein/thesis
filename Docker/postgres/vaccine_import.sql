CREATE TABLE vaccine_data (
batch_number VARCHAR(255) PRIMARY KEY,
shot_number INTEGER,
date DATE,
dummy_data VARCHAR(255),
bundesland VARCHAR(255)
);

COPY vaccine_data(batch_number, shot_number, date, dummy_data, bundesland)
FROM '/vol_tables/vaccine_data.csv'
DELIMITER ';'
CSV HEADER;
