#!/bin/bash

set +x

for pct in {20..100..20}
do
echo "Selectivity: $pct"
psql -d db1 -c "DROP TABLE IF EXISTS pg_sf$1_supplier_$pct;"
psql -d db1 -c "DROP TABLE IF EXISTS pg_sf$1_part_$pct;"
psql -d db1 -c "DROP TABLE IF EXISTS pg_sf$1_lineitem_$pct;"
psql -d db1 -c "DROP TABLE IF EXISTS pg_sf$1_partsupp_$pct;"
psql -d db1 -c "DROP TABLE IF EXISTS pg_sf$1_customer_$pct;"
psql -d db1 -c "DROP TABLE IF EXISTS pg_sf$1_orders_$pct;"

psql -d db1 -c "CREATE TABLE pg_sf$1_supplier_$pct AS SELECT * FROM pg_sf$1_supplier;"
psql -d db1 -c "CREATE TABLE pg_sf$1_part_$pct AS SELECT * FROM pg_sf$1_part;"
psql -d db1 -c "CREATE TABLE pg_sf$1_lineitem_$pct AS SELECT * FROM pg_sf$1_lineitem;"
psql -d db1 -c "CREATE TABLE pg_sf$1_partsupp_$pct AS SELECT * FROM pg_sf$1_partsupp;"
psql -d db1 -c "CREATE TABLE pg_sf$1_customer_$pct AS SELECT * FROM pg_sf$1_customer;"
psql -d db1 -c "CREATE TABLE pg_sf$1_orders_$pct AS SELECT * FROM pg_sf$1_orders;"

psql -d db1 -c "ALTER TABLE pg_sf$1_supplier_$pct ADD PRIMARY KEY(s_suppkey);"
psql -d db1 -c "ALTER TABLE pg_sf$1_part_$pct ADD PRIMARY KEY(p_partkey);"
psql -d db1 -c "ALTER TABLE pg_sf$1_customer_$pct ADD PRIMARY KEY(c_custkey);"
psql -d db1 -c "ALTER TABLE pg_sf$1_orders_$pct ADD PRIMARY KEY(o_orderkey);"

nk=$(psql -qtAX -d db1 -c "SELECT PERCENTILE_DISC(1-$pct./100) WITHIN GROUP(ORDER BY s_nationkey) FROM pg_sf$1_supplier;")
psql -d db1 -c "UPDATE pg_sf$1_supplier_$pct SET s_nationkey=s_nationkey+($nk)+1;"
psql -d db1 -c "UPDATE pg_sf$1_customer_$pct SET c_nationkey=c_nationkey+($nk)+1;"

psql -d db1 -c "CREATE INDEX s_nationidx_sf$1_$pct ON pg_sf$1_supplier_$pct(s_nationkey);"
psql -d db1 -c "CREATE INDEX c_nationidx_sf$1_$pct ON pg_sf$1_customer_$pct(c_nationkey);"

pk=$(psql -qtAX -d db1 -c "SELECT PERCENTILE_DISC(1-$pct./100) WITHIN GROUP(ORDER BY pg_sf$1_part.p_partkey) FROM pg_sf$1_part;")
psql -d db1 -c "UPDATE pg_sf$1_partsupp_$pct SET ps_partkey=ps_partkey+($pk);"
psql -d db1 -c "UPDATE pg_sf$1_lineitem_$pct SET l_partkey=l_partkey+($pk);"

psql -d db1 -c "CREATE INDEX ps_partidx_sf$1_$pct ON pg_sf$1_partsupp_$pct(ps_partkey);"
psql -d db1 -c "CREATE INDEX l_partidx_sf$1_$pct ON pg_sf$1_lineitem_$pct(l_partkey);"

sk=$(psql -qtAX -d db1 -c "SELECT PERCENTILE_DISC(1-$pct./100) WITHIN GROUP(ORDER BY pg_sf$1_supplier.s_suppkey) FROM pg_sf$1_supplier;")
psql -d db1 -c "UPDATE pg_sf$1_partsupp_$pct SET ps_suppkey=ps_suppkey+($sk);"
psql -d db1 -c "UPDATE pg_sf$1_lineitem_$pct SET l_suppkey=l_suppkey+($sk);"

psql -d db1 -c "CREATE INDEX ps_suppidx_sf$1_$pct ON pg_sf$1_partsupp_$pct(ps_suppkey);"
psql -d db1 -c "CREATE INDEX l_suppidx_sf$1_$pct ON pg_sf$1_lineitem_$pct(l_suppkey);"

ok=$(psql -qtAX -d db1 -c "SELECT PERCENTILE_DISC(1-$pct./100) WITHIN GROUP(ORDER BY pg_sf$1_orders.o_orderkey) FROM pg_sf$1_orders;")
psql -d db1 -c "UPDATE pg_sf$1_lineitem_$pct SET l_orderkey=l_orderkey+($ok);"

psql -d db1 -c "CREATE INDEX l_orderidx_sf$1_$pct ON pg_sf$1_lineitem_$pct(l_orderkey);"

ck=$(psql -qtAX -d db1 -c "SELECT PERCENTILE_DISC(1-$pct./100) WITHIN GROUP(ORDER BY pg_sf$1_customer.c_custkey) FROM pg_sf$1_customer;")
psql -d db1 -c "UPDATE pg_sf$1_orders_$pct SET o_custkey = o_custkey+($ck);"

psql -d db1 -c "CREATE INDEX o_custidx_sf$1_$pct ON pg_sf$1_orders_$pct(o_custkey);"

done
