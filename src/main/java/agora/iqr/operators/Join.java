package agora.iqr.operators;

import agora.iqr.AgoraOperator;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.RexNode;

public class Join extends AgoraOperator {

    JoinRelType type;
    // left and right column indices have to be in the same order!
    int[] leftColumnJoinIndices;
    int[] rightColumnJoinIndices;
}
