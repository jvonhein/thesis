FROM ubuntu:latest

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        gcc \
        libc-dev \
        make \
        cmake \
        ca-certificates \
        git \
        postgresql-12 \
        postgresql-server-dev-12 \
        odbc-postgresql \
        unixodbc-dev \
        libssl-dev \
        alien \
        curl \
        nano \
        openjdk-8-jdk \
    && rm -rf /var/lib/apt/lists/*

RUN git clone https://github.com/CartoDB/odbc_fdw.git && cd odbc_fdw && make && make install

RUN git clone https://github.com/MariaDB/mariadb-connector-odbc.git
RUN mkdir build && cd build && cmake ../mariadb-connector-odbc/ -DCMAKE_BUILD_TYPE=RelWithDebInfo -DCONC_WITH_UNIT_TESTS=Off -DCMAKE_INSTALL_PREFIX=/usr/local -DWITH_SSL=OPENSSL \
    && cmake --build . --config RelWithDebInfo \
    && make install

COPY config/mariadb.ini /
RUN cat mariadb.ini >> /etc/odbcinst.ini

#RUN curl -O https://downloads.cloudera.com/connectors/hive_odbc_2.6.1.1001/Linux/ClouderaHiveODBC-2.6.1.1001-1.x86_64.rpm
#RUN alien -i ClouderaHiveODBC-2.6.1.1001-1.x86_64.rpm
#COPY config/hive.ini /
#RUN cat hive.ini >> /etc/odbcinst.ini

RUN ln -s /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/server/libjvm.so /usr/lib/libjvm.so
RUN git clone https://github.com/atris/JDBC_FDW.git \
 && cd JDBC_FDW \
 && make install USE_PGXS=1

COPY tables/health.csv /tables/

USER postgres

# change password and create table
RUN    /etc/init.d/postgresql start &&\
    psql --command "ALTER USER postgres WITH PASSWORD '123456';" &&\
    createdb -O postgres db1 &&\
    psql -d db1 --command "CREATE TABLE health( \
        bundesland VARCHAR(255), \
        population_total INTEGER, \
        vaccinated_firstshot INTEGER, \
        vaccinated_secondshot INTEGER, \
        percent_vaccinated_firstshot NUMERIC(4,3), \
        percent_vaccinated_secondshot NUMERIC(4,3));" &&\
   psql -d db1 --command "COPY health(bundesland, population_total, vaccinated_firstshot, vaccinated_secondshot, percent_vaccinated_firstshot, percent_vaccinated_secondshot) \
        FROM '/tables/health.csv' \
        DELIMITER ';' \
        CSV HEADER;"

RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/12/main/pg_hba.conf

RUN echo "listen_addresses='*'" >> /etc/postgresql/12/main/postgresql.conf

EXPOSE 5432

#COPY import/create_tpch.sql /create_tpch.sql
#COPY import/index_tpch.sql /index_tpch.sql
#COPY import/import_tpch.sh /import_tpch.sh
#COPY import/create_indexes.sh /create_indexes.sh

#VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]

CMD ["/usr/lib/postgresql/12/bin/postgres", "-D", "/var/lib/postgresql/12/main", "-c", "config_file=/etc/postgresql/12/main/postgresql.conf"]
