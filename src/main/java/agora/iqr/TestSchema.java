package agora.iqr;


import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.HashMap;
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

        HashMap<String, Table> tableMap = new HashMap<>();
        tableMap.put("health", health);
        tableMap.put("crime", crime);
        tableMap.put("criminals", criminals);

        this.tableMap = tableMap;
    }


    public TestSchema(Map<String, Table> tableMap) {
        this.tableMap = tableMap;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }
}
