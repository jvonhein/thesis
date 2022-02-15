package com.agora.joscha.iqr;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rel2sql.SqlImplementor;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

public class App {
    public static void main(String[] args){

        final TestSchema testSchema = new TestSchema();

        // System.out.println(testSchema.getTableMap().get("health").getRowType(new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT)));

        final SchemaPlus rootSchema = Frameworks.createRootSchema(true).add("vaccine_crime", testSchema);
        final Frameworks.ConfigBuilder configBuilder = Frameworks.newConfigBuilder();
        final FrameworkConfig config = configBuilder
                .defaultSchema(rootSchema)
                .build();
        final RelBuilder builder = RelBuilder.create(config);

        // manual plan for reference
        final RelNode left = builder
                .scan("health")
                .project(builder.field(0), builder.field(5))
                .build();

        final RelNode right = builder
                .scan("crime")
                .filter(builder.call(
                        SqlStdOperatorTable.GREATER_THAN,
                        builder.field(3),
                        builder.literal("2021-07-00")
                ), builder.call(
                        SqlStdOperatorTable.EQUALS,
                        builder.field(2),
                        builder.literal("bodily_harm")
                ))
                .project(builder.field(1), builder.field(4))
                .scan("criminals")
                .project(builder.field(0), builder.field(4))
                .join(JoinRelType.INNER,
                        builder.call(
                                SqlStdOperatorTable.EQUALS,
                                builder.field(2, 0, 1),
                                builder.field(2, 1, 0)
                        ))
                .project(builder.field(0), builder.call(
                        SqlStdOperatorTable.CASE,
                        builder.call(
                                SqlStdOperatorTable.EQUALS,
                                builder.field(3),
                                builder.literal(1)
                        ),
                        builder.literal(1),
                        builder.literal(null)
                        ))
                .aggregate(builder.groupKey(0),
                        builder.count(),
                        builder.count(builder.field(1)))
                /*
                .aggregate(builder.groupKey(0),
                        builder.count().filter(builder.call(
                                SqlStdOperatorTable.EQUALS,
                                builder.field(1),
                                builder.literal(1)
                        )),
                        builder.count())

                 */
                .build();

                /*

        final RelNode relNode = builder
                .push(left)
                .push(right)
                .join(JoinRelType.INNER,
                        builder.call(
                                SqlStdOperatorTable.EQUALS,
                                builder.field(2, 0, 0),
                                builder.field(2, 1, 0)
                        ))
                .build();

                 */


        System.out.println(RelOptUtil.toString(right));


        final SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        final RelToSqlConverter relToSqlConverter = new RelToSqlConverter(dialect);
        SqlImplementor.Result rs = relToSqlConverter.visitRoot(right);


        System.out.println(rs.asStatement().toString());
    }
}
