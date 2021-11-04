package agora.iqr.operators;

import agora.iqr.AgoraOperator;
import org.apache.calcite.rel.RelFieldCollation;

import java.util.List;

public class Sort extends AgoraOperator {
    // contains indexes and direction of the columns which are to be sorted in priority order (first list-element has highest priority)
    List<RelFieldCollation> ordering;
}
