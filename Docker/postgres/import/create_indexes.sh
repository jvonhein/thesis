#!/bin/bash

set -x
#$1: postgres db, $2: scale factor
sed "s\pg_sf\pg${1}_sf\g" index_tpch.sql >/tmp/index1.sql
sed "s\_sf_\_sf${2}_\g" /tmp/index1.sql > /tmp/index2.sql
psql -f /tmp/index2.sql db1