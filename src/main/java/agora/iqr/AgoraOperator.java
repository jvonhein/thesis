package agora.iqr;



import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.sql.type.SqlTypeName;


import java.util.List;

/**
 * general building block of the of a subquery
 */
public abstract class AgoraOperator {

    // identifier, used to define a position within the subplan
    public int id;
    public int rowcount;
    public int cumulativeCost;
    // defines ordering of the table
    public List<RelCollation> ordering;
    // columns are interacted with their index in the column-array
    public SqlTypeName[] columnTypes;
    public List<Integer> input;
}
