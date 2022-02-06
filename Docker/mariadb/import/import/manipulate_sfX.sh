#!/bin/bash

set +x
filename=/import/manipulate_sf$1.sql
for pct in {20..100..20}
do
  echo  "SELECT 'Selectivity: $pct' as '';
  DROP TABLE IF EXISTS mdb_sf$1_supplier_$pct;
  DROP TABLE IF EXISTS mdb_sf$1_part_$pct;
  DROP TABLE IF EXISTS mdb_sf$1_lineitem_$pct;
  DROP TABLE IF EXISTS mdb_sf$1_partsupp_$pct;
  DROP TABLE IF EXISTS mdb_sf$1_customer_$pct;
  DROP TABLE IF EXISTS mdb_sf$1_orders_$pct;

  CREATE TABLE mdb_sf$1_supplier_$pct AS SELECT * FROM mdb_sf$1_supplier;
  CREATE TABLE mdb_sf$1_part_$pct AS SELECT * FROM mdb_sf$1_part;
  CREATE TABLE mdb_sf$1_lineitem_$pct AS SELECT * FROM mdb_sf$1_lineitem;
  CREATE TABLE mdb_sf$1_partsupp_$pct AS SELECT * FROM mdb_sf$1_partsupp;
  CREATE TABLE mdb_sf$1_customer_$pct AS SELECT * FROM mdb_sf$1_customer;
  CREATE TABLE mdb_sf$1_orders_$pct AS SELECT * FROM mdb_sf$1_orders;
  
  ALTER TABLE mdb_sf$1_supplier_$pct ADD PRIMARY KEY(s_suppkey);
  ALTER TABLE mdb_sf$1_part_$pct ADD PRIMARY KEY(p_partkey);
  ALTER TABLE mdb_sf$1_customer_$pct ADD PRIMARY KEY(c_custkey);
  ALTER TABLE mdb_sf$1_orders_$pct ADD PRIMARY KEY(o_orderkey);

  SELECT 'nationkey' as '';
  UPDATE mdb_sf$1_supplier_$pct SET s_nationkey=s_nationkey+(SELECT ROUND(max(n_nationkey)*(1-$pct./100)) FROM mdb_sf$1_nation);
  UPDATE mdb_sf$1_customer_$pct SET c_nationkey=c_nationkey+(SELECT ROUND(max(n_nationkey)*(1-$pct./100)) FROM mdb_sf$1_nation);

  CREATE INDEX s_nationidx_sf$1_$pct ON mdb_sf$1_supplier_$pct(s_nationkey);
  CREATE INDEX c_nationidx_sf$1_$pct ON mdb_sf$1_customer_$pct(c_nationkey);

  SELECT 'partkey' as '';
  UPDATE mdb_sf$1_partsupp_$pct SET ps_partkey=ps_partkey+(SELECT ROUND(max(p_partkey)*(1-$pct./100)) FROM mdb_sf$1_part);
  UPDATE mdb_sf$1_lineitem_$pct SET l_partkey=l_partkey+(SELECT ROUND(max(p_partkey)*(1-$pct./100)) FROM mdb_sf$1_part);
  
  CREATE INDEX ps_partidx_sf$1_$pct ON mdb_sf$1_partsupp_$pct(ps_partkey);
  CREATE INDEX l_partidx_sf$1_$pct ON mdb_sf$1_lineitem_$pct(l_partkey);

  SELECT 'suppkey' as '';
  UPDATE mdb_sf$1_partsupp_$pct SET ps_suppkey=ps_suppkey+(SELECT ROUND(max(s_suppkey)*(1-$pct./100)) FROM mdb_sf$1_supplier);
  UPDATE mdb_sf$1_lineitem_$pct SET l_suppkey=l_suppkey+(SELECT ROUND(max(s_suppkey)*(1-$pct./100)) FROM mdb_sf$1_supplier);
  
  CREATE INDEX ps_suppidx_sf$1_$pct ON mdb_sf$1_partsupp_$pct(ps_suppkey);
  CREATE INDEX l_suppidx_sf$1_$pct ON mdb_sf$1_lineitem_$pct(l_suppkey);

  SELECT 'orderkey' as '';
  UPDATE mdb_sf$1_lineitem_$pct SET l_orderkey=l_orderkey+(SELECT ROUND(max(o_orderkey)*(1-$pct./100)) FROM mdb_sf$1_orders);
  
  CREATE INDEX l_orderidx_sf$1_$pct ON mdb_sf$1_lineitem_$pct(l_orderkey);

  SELECT 'custkey' as '';
  UPDATE mdb_sf$1_orders_$pct SET o_custkey = o_custkey+(SELECT ROUND(max(c_custkey)*(1-$pct./100)) FROM mdb_sf$1_customer);
  
  CREATE INDEX o_custidx_sf$1_$pct ON mdb_sf$1_orders_$pct(o_custkey);" >> $filename
mysql mdb < $filename -u mariadb -p123456
done
