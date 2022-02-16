package com.agora.joscha.old;

import com.agora.joscha.iqr.TestSchema;
import com.agora.joscha.iqr.TestTable;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.*;

import java.util.HashMap;

public class sqlToRelTest {
    public static void main(String[] args) throws SqlParseException, ValidationException, RelConversionException {
        final TestTable health = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("land", SqlTypeName.VARCHAR)
                    .add("population_total", SqlTypeName.INTEGER)
                    .add("vaccinated_firstshot", SqlTypeName.INTEGER)
                    .add("vaccinated_secondshot", SqlTypeName.INTEGER)
                    .add("percent_vaccinated_firstshot", SqlTypeName.DOUBLE)
                    .add("percent_vaccinated_secondshot", SqlTypeName.DOUBLE)
                    .build();
        });

        final TestTable crime = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("case_number", SqlTypeName.INTEGER)
                    .add("land", SqlTypeName.VARCHAR)
                    .add("category", SqlTypeName.VARCHAR)
                    .add("date", SqlTypeName.DATE)
                    .add("criminal_id", SqlTypeName.INTEGER)
                    .add("victim_id", SqlTypeName.INTEGER)
                    .add("details", SqlTypeName.VARCHAR)
                    .build();
        });

        final TestTable criminals = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("id", SqlTypeName.INTEGER)
                    .add("gender", SqlTypeName.CHAR)
                    .add("age", SqlTypeName.INTEGER)
                    .add("address", SqlTypeName.VARCHAR)
                    .add("vaccine_status", SqlTypeName.INTEGER)
                    .add("details", SqlTypeName.VARCHAR)
                    .build();
        });

        HashMap<String, Table> tableMap = new HashMap<>();
        tableMap.put("health", health);
        tableMap.put("crime", crime);
        tableMap.put("criminals", criminals);

        final TestSchema testSchema = new TestSchema(tableMap);

        System.out.println(testSchema.getTableMap().get("health").getRowType(new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT)));

        final SchemaPlus rootSchema = Frameworks.createRootSchema(true).add("VACCINE_CRIME", testSchema);
        final Frameworks.ConfigBuilder configBuilder = Frameworks.newConfigBuilder();
        final FrameworkConfig config = configBuilder
                .defaultSchema(rootSchema)
                .build();

        final Planner planner = Frameworks.getPlanner(config);


        /*
        String query = "SELECT \"health\".\"land\", \"health\".\"percent_vaccinated_secondshot\", " +
                "COUNT(CASE WHEN \"criminal\".\"vaccine_status\" = 1 then 1 ELSE NULL END) AS criminals_vaccinated, COUNT(\"criminal\".\"vaccine_status\") criminals_all" +
                "FROM VACCINE_CRIME.\"health\", VACCINE_CRIME.\"crime\", VACCINE_CRIME.\"criminals\" "+
                "WHERE \"health\".\"land\" = \"crime\".\"land\"" +
                "AND \"crime\".\"date\" > '2021-07-00' " +
                "AND \"crime\".\"category\" = 'bodily_harm' " +
                "GROUP BY \"land\"";


        String working = "SELECT \"health\".\"land\", \"crime\".\"criminal_id\" " +
                "FROM VACCINE_CRIME.\"health\", VACCINE_CRIME.\"crime\",  VACCINE_CRIME.\"criminals\"" +
                "WHERE \"health\".\"land\" = \"crime\".\"land\" " +
                "AND \"crime\".\"criminal_id\" = \"criminals\".\"id\" " +
                "AND \"crime\".\"category\" = 'bodily_harm'";

         */

        String query1 = "SELECT \"health\".\"land\", COUNT(*) AS \"criminals_all\", COUNT(CASE WHEN \"criminals\".\"vaccine_status\" = 1 then 1 ELSE NULL END) AS \"criminals_vaccinated\" " +
                "FROM VACCINE_CRIME.\"health\", VACCINE_CRIME.\"crime\",  VACCINE_CRIME.\"criminals\"" +
                "WHERE \"health\".\"land\" = \"crime\".\"land\" " +
                "AND \"crime\".\"criminal_id\" = \"criminals\".\"id\" " +
                "AND \"crime\".\"category\" = 'bodily_harm' " +
                "GROUP BY \"health\".\"land\"";


        final SqlNode parse = planner.parse(query1);
        final SqlNode validate = planner.validate(parse);
        final RelNode relNode = planner.rel(validate).project();

        System.out.println(RelOptUtil.toString(relNode));
    }
}
