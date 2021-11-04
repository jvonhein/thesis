package agora.iqr;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;

import java.util.function.Function;

public class TestTable extends AbstractTable {

    private final Function<RelDataTypeFactory, RelDataType> typeBuilder;

    public TestTable(Function<RelDataTypeFactory, RelDataType> typeBuilder) {
        this.typeBuilder = typeBuilder;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        return typeBuilder.apply(typeFactory);
    }
}
