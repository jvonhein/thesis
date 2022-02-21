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

COPY tables/country_stats.csv /tables/
COPY init.sql /
COPY vaccine_import.sql /

RUN git clone https://github.com/CartoDB/odbc_fdw.git && cd odbc_fdw && make && make install
RUN git clone https://github.com/MariaDB/mariadb-connector-odbc.git
RUN mkdir build && cd build && cmake ../mariadb-connector-odbc/ -DCMAKE_BUILD_TYPE=RelWithDebInfo -DCONC_WITH_UNIT_TESTS=Off -DCMAKE_INSTALL_PREFIX=/usr/local -DWITH_SSL=OPENSSL \
    && cmake --build . --config RelWithDebInfo \
    && make install \
    && cd /

COPY config /config 
RUN cat /config/mariadb_driver.ini >> /etc/odbcinst.ini &&\
    cat /config/data_sources.ini >> /etc/odbc.ini


RUN ln -s /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/server/libjvm.so /usr/lib/libjvm.so
RUN git clone https://github.com/atris/JDBC_FDW.git \
 && cd JDBC_FDW \
 && make install USE_PGXS=1


USER postgres

# change password and create table
RUN    /etc/init.d/postgresql start &&\
    psql --command "ALTER USER postgres WITH PASSWORD '123456';" &&\
    createdb -O postgres db1 &&\
    psql -d db1 -f /init.sql


RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/12/main/pg_hba.conf

RUN echo "listen_addresses='*'" >> /etc/postgresql/12/main/postgresql.conf

EXPOSE 5432

VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"] 

CMD ["/usr/lib/postgresql/12/bin/postgres", "-D", "/var/lib/postgresql/12/main", "-c", "config_file=/etc/postgresql/12/main/postgresql.conf"]
