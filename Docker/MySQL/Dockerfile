FROM mysql/mysql-server:latest

ENV MYSQL_ROOT_PASSWORD=my-secret-pw

COPY /home/rpoepsel/joscha/tables/criminals.csv /tables/

RUN "${mysql[@]}" -e "CREATE DATABASE thesis"

## CREATE TABLE criminals (id INT NOT NULL, gender CHAR(1) NOT NULL, age INT NULL, adress VARCHAR(255) NULL, vaccine_status tinyint NOT NULL, details VARCHAR(255) NULL);
load data infile '/tmp/criminals.csv' into table criminals fields terminated by ';' lines terminated by '\n' ignore 1 rows;
