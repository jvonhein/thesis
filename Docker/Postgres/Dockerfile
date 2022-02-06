FROM ubuntu:latest

ENV DEBIAN_FRONTEND interactive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        gcc \
        libc-dev \
        make \
        cmake \
        git \
        postgresql-12 \
        postgresql-server-dev-12 \
        odbc-postgresql \
        curl \
    && rm -rf /var/lib/apt/lists/*

RUN echo 'root:Docker!' | chpasswd

COPY src/main/resources/tables/health.csv /tables/

USER postgres

# change pw & create db1 for this image
RUN    /etc/init.d/postgresql start &&\
    psql --command "ALTER USER postgres WITH PASSWORD '123456';" &&\
    createdb -O postgres db1

# import health table from csv
RUN psql -d db1 --command "CREATE TABLE health( \
        bundesland VARCHAR(255), \
        population_total INTEGER, \
        vaccinated_firstshot INTEGER, \
        vaccinated_secondshot INTEGER, \
        percent_vaccinated_firstshot NUMERIC(4,3), \
        percent_vaccinated_secondshot NUMERIC(4,3));" &&\
    psql --command "COPY health(bundesland, population_total, vaccinated_firstshot, vaccinated_secondshot, percent_vaccinated_firstshot, percent_vaccinated_secondshot) \
        FROM '/tables/health.csv' \
        DELIMITER ';' \
        CSV HEADER;"