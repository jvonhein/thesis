#!/bin/bash

set -x
filename=/import/indexes_tpch_sf$1.sql
ehco "ALTER TABLE mdb_sf$1_part
    ADD CONSTRAINT part_kpey
        PRIMARY KEY (p_partkey);

ALTER TABLE mdb_sf$1_supplier
    ADD CONSTRAINT supplier_pkey
        PRIMARY KEY (s_suppkey);

ALTER TABLE mdb_sf$1_partsupp
    ADD CONSTRAINT partsupp_pkey
        PRIMARY KEY (ps_partkey, ps_suppkey);

ALTER TABLE mdb_sf$1_customer
    ADD CONSTRAINT customer_pkey
        PRIMARY KEY (c_custkey);

ALTER TABLE mdb_sf$1_orders
    ADD CONSTRAINT orders_pkey
        PRIMARY KEY (o_orderkey);

ALTER TABLE mdb_sf$1_lineitem
    ADD CONSTRAINT lineitem_pkey
        PRIMARY KEY (l_orderkey, l_linenumber);

ALTER TABLE mdb_sf$1_nation
    ADD CONSTRAINT nation_pkey
        PRIMARY KEY (n_nationkey);

ALTER TABLE mdb_sf$1_region
    ADD CONSTRAINT region_pkey
        PRIMARY KEY (r_regionkey);" >> filename

bash "mysql mdb < $filename -u mariadb -p123456"