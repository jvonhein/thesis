#!/bin/bash

set -x
filename=/import/import_tpch_sf$1.sql
echo "
LOAD DATA LOCAL INFILE '/data/sf$1/customer.tbl' into table mdb_sf$1_customer FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/orders.tbl' into table mdb_sf$1_orders FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/lineitem.tbl' into table mdb_sf$1_lineitem FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/nation.tbl' into table mdb_sf$1_nation FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/partsupp.tbl' into table mdb_sf$1_partsupp FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/part.tbl' into table mdb_sf$1_part FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/region.tbl' into table mdb_sf$1_region FIELDS TERMINATED BY '|';
LOAD DATA LOCAL INFILE '/data/sf$1/supplier.tbl' into table mdb_sf$1_supplier FIELDS TERMINATED BY '|';" >> $filename
mysql mdb < $filename -u mariadb -p123456