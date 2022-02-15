package com.agora.joscha.iqr;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rel2sql.SqlImplementor;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

import java.io.BufferedReader;
import java.io.FileReader;

public class TranslationTest {

    public static void main(String[] args) throws Exception {

        String file = "/Users/joschavonhein/Workspace/thesis/project/src/main/resources/Vaccine-Crime-Plan.json";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder resultStringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            resultStringBuilder.append(line).append("\n");
        }
        reader.close();
        String json = resultStringBuilder.toString();

        final TestSchema vaccineCrimeSchema = new TestSchema();

        // add the two intermediate results as tables to the schema so calcite can work with them - manually for now
        final TestTable remote1 = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("land", SqlTypeName.VARCHAR)
                    .add("criminals_vaccinated", SqlTypeName.INTEGER)
                    .add("criminals_total", SqlTypeName.INTEGER)
                    .build();
        });
        final TestTable remote2 = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("criminal_id", SqlTypeName.INTEGER)
                    .add("vaccine_status", SqlTypeName.INTEGER)
                    .build();
        });
        vaccineCrimeSchema.getTableMap().put("remote_view_123_1_0", remote1);
        vaccineCrimeSchema.getTableMap().put("remote_view_123_2_0", remote2);


        final SchemaPlus rootSchema = Frameworks.createRootSchema(true).add("vaccine_crime", vaccineCrimeSchema);
        final Frameworks.ConfigBuilder configBuilder = Frameworks.newConfigBuilder();
        final FrameworkConfig config = configBuilder
                .defaultSchema(rootSchema)
                .build();
        final RelBuilder builder = RelBuilder.create(config);

        final SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        final RelToSqlConverter relToSqlConverter = new RelToSqlConverter(dialect);

        // use factory methods to translate the json & print
        final RelNode first = RelFactory.translateJson(json,"akka://sys@hostA:2552/user/executor" , builder);
        System.out.println(RelOptUtil.toString(first));
        SqlImplementor.Result rs1 = relToSqlConverter.visitRoot(first);
        System.out.println(rs1.asStatement().toString());
        System.out.println();

        final RelNode second = RelFactory.translateJson(json,"akka://sysB@hostB:2552/user/executor" , builder);
        System.out.println(RelOptUtil.toString(second));
        SqlImplementor.Result rs2 = relToSqlConverter.visitRoot(second);
        System.out.println(rs2.asStatement().toString());
        System.out.println();


        final RelNode third = RelFactory.translateJson(json,"akka://sysC@hostC:2552/user/executor" , builder);
        System.out.println(RelOptUtil.toString(third));
        SqlImplementor.Result rs3= relToSqlConverter.visitRoot(third);
        System.out.println(rs3.asStatement().toString());
    }


    private static void oldExamples(){
        final HrClusteredSchema schema = new HrClusteredSchema();
        //Class.forName("org.apache.calcite.jdbc.Driver");
        //final Properties info = new Properties();
        SchemaPlus rootSchema = Frameworks.createRootSchema(true).add("hr", schema);

        final Frameworks.ConfigBuilder configBuilder = Frameworks.newConfigBuilder();

        final FrameworkConfig config = configBuilder
                .defaultSchema(rootSchema)
                .build();

        final RelBuilder builder = RelBuilder.create(config);


        // example0(builder);
        example1(builder);
    }

    private static void example0(RelBuilder builder){
        final RelNode node = builder.scan("emps")
                .scan("depts")
                .join(JoinRelType.INNER,
                        builder.call(
                                SqlStdOperatorTable.EQUALS,
                                builder.field(2, 0,1),
                                builder.field(2,1,0)
                        ))
                .project(
                        builder.field(3),
                        builder.field(6)
                )
                .aggregate(builder.groupKey(1),
                        builder.count(false, "departmentSalary", builder.field(0)))
                .build();


        System.out.println(RelOptUtil.toString(node));

        final SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        final RelToSqlConverter relToSqlConverter = new RelToSqlConverter(dialect);
        SqlImplementor.Result rs = relToSqlConverter.visitRoot(node);

        System.out.println(rs.asStatement().toString());
    }

    private static void example1(RelBuilder builder){
        final RelBuilder filter = builder.scan("emps")
                .filter(
                        builder.call(
                                SqlStdOperatorTable.EQUALS,
                                builder.field(1),
                                builder.literal(10)),
                        builder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                builder.field("salary"),
                                builder.literal(8000)
                        ));
        final RelNode node = builder
                .project(
                        builder.field(1, 0, "name"),
                        builder.field(1, 0, "salary"),
                        builder.field(1, 0, "deptno")
                        //builder.field(2,1,"deptno")
                )
                .scan("depts")
                .join(JoinRelType.INNER, "deptno")
                .build();


        System.out.println(RelOptUtil.toString(node));

        final SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        final RelToSqlConverter relToSqlConverter = new RelToSqlConverter(dialect);
        SqlImplementor.Result rs = relToSqlConverter.visitRoot(node);

        System.out.println(rs.asStatement().toString());
    }
}
