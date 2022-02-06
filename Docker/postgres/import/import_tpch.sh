#!/bin/bash
#$1: postgres db, $2: scale factor
set -x

sed "s\CREATE TABLE pg_\CREATE TABLE pg$1_\g" create_tpch.sql >/tmp/create1.sql
sed "s\TABLE pg$1_sf_\TABLE pg$1_sf$2_\g" /tmp/create1.sql >/tmp/create2.sql
psql -f /tmp/create2.sql db1

psql -d db1 -c "\copy pg$1_sf$2_lineitem FROM /data/sf$2/lineitem.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_supplier FROM /data/sf$2/supplier.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_region FROM /data/sf$2/region.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_nation FROM /data/sf$2/nation.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_orders FROM /data/sf$2/orders.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_customer FROM /data/sf$2/customer.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_partsupp FROM /data/sf$2/partsupp.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
psql -d db1 -c "\copy pg$1_sf$2_part FROM /data/sf$2/part.tbl with CSV DELIMITER '|' QUOTE '\"' ESCAPE '\';"
