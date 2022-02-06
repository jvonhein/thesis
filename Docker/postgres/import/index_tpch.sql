alter table pg_sf_part
    add constraint part_sf_pkey
        primary key (p_partkey);

alter table pg_sf_supplier
    add constraint supplier_sf_pkey
        primary key (s_suppkey);

alter table pg_sf_partsupp
    add constraint partsupp_sf_pkey
        primary key (ps_partkey, ps_suppkey);

alter table pg_sf_customer
    add constraint customer_sf_pkey
        primary key (c_custkey);

alter table pg_sf_orders
    add constraint orders_sf_pkey
        primary key (o_orderkey);

alter table pg_sf_lineitem
    add constraint lineitem_sf_pkey
        primary key (l_orderkey, l_linenumber);

alter table pg_sf_nation
    add constraint nation_sf_pkey
        primary key (n_nationkey);

alter table pg_sf_region
    add constraint region_sf_pkey
        primary key (r_regionkey);


create index idx_sf_supplier_nation_key on pg_sf_supplier (s_nationkey);

create index idx_sf_partsupp_partkey on pg_sf_partsupp (ps_partkey);
create index idx_sf_partsupp_suppkey on pg_sf_partsupp (ps_suppkey);

create index idx_sf_customer_nationkey on pg_sf_customer (c_nationkey);

create index idx_sf_orders_custkey on pg_sf_orders (o_custkey);

create index idx_sf_lineitem_orderkey on pg_sf_lineitem (l_orderkey);
create index idx_sf_lineitem_part_supp on pg_sf_lineitem (l_partkey,l_suppkey);

create index idx_sf_nation_regionkey on pg_sf_nation (n_regionkey);

