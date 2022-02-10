package agora.iqr;


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestSchema extends AbstractSchema {

    final Map<String, Table> tableMap;

    // Default Constructor which creates Vaccine-Crime Schema
    public TestSchema(){
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

        final TestTable country_stats = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("bundesland", SqlTypeName.VARCHAR)
                    .add("population", SqlTypeName.INTEGER)
                    .add("size", SqlTypeName.INTEGER)
                    .add("dummy_data", SqlTypeName.VARCHAR)
                    .build();
        });

        final TestTable vaccine_data = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            return builder
                    .add("batch_number", SqlTypeName.VARCHAR)
                    .add("shot_number", SqlTypeName.INTEGER)
                    .add("date", SqlTypeName.DATE)
                    .add("dummy_data", SqlTypeName.VARCHAR)
                    .add("bundesland", SqlTypeName.VARCHAR)
                    .build();
        });

        HashMap<String, Table> tableMap = new HashMap<>();
        tableMap.put("health", health);
        tableMap.put("crime", crime);
        tableMap.put("criminals", criminals);
        tableMap.put("country_stats", country_stats);
        tableMap.put("vaccine_data", vaccine_data);

        this.tableMap = tableMap;
    }

    public TestSchema addTableFromColumns(JsonNode columns, String tableName) {

        final TestTable newTable = new TestTable(relDataTypeFactory -> {
            RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(relDataTypeFactory);
            int numColumns = columns.size();
            for (int i = 0; i < numColumns; i++) {
                String columnType = columns.get(i).asText();
                switch (columnType){
                    case "VARCHAR":
                        builder.add("varchar_"+i, SqlTypeName.VARCHAR);
                        break;
                    case "INTEGER":
                        builder.add("integer_"+i, SqlTypeName.INTEGER);
                        break;
                    case "DATE":
                        builder.add("date"+i, SqlTypeName.DATE);
                        break;
                    case "DOUBLE":
                        builder.add("double"+i, SqlTypeName.DOUBLE);
                        break;
                    default:
                        System.out.println("column Type not identified - not added to Schema!!!");
                        break;
                }
            }
            return builder.build();
        });

        tableMap.put(tableName, newTable);
        return this;
    }

    public TestSchema(Map<String, Table> tableMap) {
        this.tableMap = tableMap;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }
}
