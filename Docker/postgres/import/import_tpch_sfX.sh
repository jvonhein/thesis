#!/bin/bash

set +x

#sed "s\CREATE TABLE pg_\CREATE TABLE pg$1_\g" create_tpch_sf$1.sql >/tmp/create10.sql
#psql -f /tmp/create10.sql db1

psql -d db1 -c "\copy pg_sf$1_lineitem FROM /data/sf$1/lineitem.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_supplier FROM /data/sf$1/supplier.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_region FROM /data/sf$1/region.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_nation FROM /data/sf$1/nation.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_orders FROM /data/sf$1/orders.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_customer FROM /data/sf$1/customer.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_partsupp FROM /data/sf$1/partsupp.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg_sf$1_part FROM /data/sf$1/part.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"