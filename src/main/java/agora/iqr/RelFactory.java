package agora.iqr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.RelBuilder;

import java.io.BufferedReader;
import java.io.FileReader;

public class RelFactory {

    public static RelNode translateJson(String json, String executorActorRef, RelBuilder relBuilder) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        // find the correct query in the iqr-json
        final JsonNode subplans = node.path("subplans");
        for (JsonNode subplan: subplans){
            // found the subplan of this executor
            if (subplan.path("executor-actorRef").asText().equals(executorActorRef)){
                final JsonNode query = subplan.path("query");

                return buildRelNodeRecursively(query, 0, relBuilder);
            }
        }

        return null;
    }

    private static RelNode buildRelNodeRecursively(JsonNode query, int index, RelBuilder relBuilder) throws Exception {
        relBuilder.clear();

        JsonNode jsonOperator = query.get(index);
        JsonNode inputs = jsonOperator.path("input");
        RelNode operator = null;
        if (inputs.size() == 1){
            RelNode child = buildRelNodeRecursively(query, inputs.get(0).asInt(), relBuilder);
            relBuilder.push(child);
            String type = jsonOperator.path("type").asText();
            switch (type){
                case "project": operator = buildProject(jsonOperator, relBuilder); break;
                case "filter": operator = buildFilter(jsonOperator, relBuilder); break;
                case "aggregate": operator = buildAggregate(jsonOperator, relBuilder); break;
            }
        } else if (inputs.size() == 2){
            RelNode leftChild = buildRelNodeRecursively(query, inputs.get(0).asInt(), relBuilder);
            RelNode rightChild = buildRelNodeRecursively(query, inputs.get(1).asInt(), relBuilder);
            relBuilder
                    .push(leftChild)
                    .push(rightChild);
            operator = buildJoin(jsonOperator, relBuilder);

        } else if (inputs.size() == 0){
            // no childs
            operator = buildScan(jsonOperator, relBuilder);
        } else {
            throw new IllegalStateException("Operators can only have 0-2 inputs");
        }

        // System.out.println(jsonOperator.path("id").asInt());

        return operator;
    }

    private static RelNode buildJoin(JsonNode operator, RelBuilder relBuilder) throws Exception {
        JoinRelType joinRelType = null;
        switch (operator.path("jointype").asText()){
            case "LEFT": joinRelType = JoinRelType.LEFT; break;
            case "RIGHT": joinRelType = JoinRelType.RIGHT; break;
            case "SEMI": joinRelType = JoinRelType.SEMI; break;
            case "FULL": joinRelType = JoinRelType.FULL; break;
            case "ANTI": joinRelType = JoinRelType.ANTI; break;
            case "INNER": joinRelType = JoinRelType.INNER;
            default: break; // use inner as default
        }
        RexNode[] conditions = new RexNode[operator.path("conditions").size()];
        for (int i = 0; i < conditions.length; i++) {
            final SqlOperator sqlOperator = extractOperator(operator.path("conditions").path(i).path("operator"));
            int leftOrdinal = operator.path("conditions").path(i).path("leftOrdinal").asInt();
            int rightOrdinal = operator.path("conditions").path(i).path("rightOrdinal").asInt();
            conditions[i] = relBuilder.call(sqlOperator, relBuilder.field(leftOrdinal), relBuilder.field(rightOrdinal));
        }

        if (conditions.length == 1){
            return relBuilder.join(joinRelType, conditions[0]).build();
        } else {
            throw new Exception("currently only support for 1 join condition");
        }
    }


    private static RelNode buildProject(JsonNode operator, RelBuilder relBuilder){
        RexInputRef[] ordinals = new RexInputRef[operator.path("fieldOrdinals").size()];
        for(int i=0; i < operator.path("fieldOrdinals").size(); i++){
            int ordinal = operator.path("fieldOrdinals").get(i).asInt();
            ordinals[i] = relBuilder.field(ordinal);
        }
        return relBuilder.project(ordinals).build();
    }

    private static RelNode buildScan(JsonNode operator, RelBuilder relBuilder){
        String tableName = operator.path("tableName").asText();
        return relBuilder
                .scan(tableName)
                .build();
    }

    private static RelNode buildFilter(JsonNode operator, RelBuilder relBuilder) throws Exception {
        RexNode[] predicates = new RexNode[operator.path("predicates").size()];

        for (int i=0; i < operator.path("predicates").size(); i++){
            JsonNode predicate = operator.path("predicates").get(i);

            SqlOperator sqlOperator = extractOperator(predicate.path("operator"));
            RexNode operand0 = extractOperand(predicate.path("operands").path(0), relBuilder);
            RexNode operand1 = extractOperand(predicate.path("operands").path(1), relBuilder);

            predicates[i] = relBuilder.call(sqlOperator, operand0, operand1);
        }

        return relBuilder
                .filter(predicates)
                .build();
    }

    private static RelNode buildAggregate(JsonNode operator, RelBuilder relBuilder) throws Exception {
        int[] groupKeys = new int[operator.path("GroupingIndices").size()];
        for (int i = 0; i < operator.path("GroupingIndices").size(); i++) {
            groupKeys[i] = operator.path("GroupingIndices").path(i).asInt();
        }
        RelBuilder.AggCall[] aggCalls = new RelBuilder.AggCall[operator.path("aggCalls").size()];
        for (int i = 0; i < operator.path("aggCalls").size(); i++) {

            JsonNode aggCallNode = operator.path("aggCalls").path(i);
            String alias = aggCallNode.has("alias") ? aggCallNode.path("alias").asText() : null;
            boolean distinct = aggCallNode.has("distinct") && aggCallNode.path("distinct").asText().equals("true");

            // optional conditional clause
            boolean predicate = aggCallNode.has("predicate");
            RexNode condition = null;
            if (aggCallNode.has("predicate")){
                final SqlOperator sqlOperator = extractOperator(aggCallNode.path("predicate").path("operator"));
                final RexNode operand0 = extractOperand(aggCallNode.path("predicate").path("operands").path(0), relBuilder);
                final RexNode operand1 = extractOperand(aggCallNode.path("predicate").path("operands").path(1), relBuilder);
                condition = relBuilder.call(sqlOperator, operand0, operand1);
            }

            switch (aggCallNode.path("type").asText()){
                case "Count":
                    RexNode[] fields = new RexNode[aggCallNode.path("indices").size()];
                    for (int j = 0; j < fields.length; j++) {
                        fields[i] = relBuilder.field(aggCallNode.path("indices").path(i).asInt());
                    }
                    aggCalls[i] = predicate ? relBuilder.count(distinct, alias, fields).filter(condition) : relBuilder.count(distinct, alias, fields);
                    break;

                // TODO: implement more aggregations
            }

        }

        return relBuilder
                .aggregate(relBuilder.groupKey(groupKeys), aggCalls)
                .build();
    }


    // helper method to correctly extract the operand correctly from a json-node
    private static RexNode extractOperand(JsonNode node, RelBuilder relBuilder) throws Exception {
        RexNode operand = null;
        if (node.path("type").asText().equals("field")){
            operand = relBuilder.field(node.path("field").asInt());
        } else if (node.path("type").asText().equals("literal")){
            if (node.path("dataType").asText().equals("Int")){
                operand = relBuilder.literal(node.path("value").asInt());
            } else if (node.path("dataType").asText().equals("String")){
                operand = relBuilder.literal(node.path("value").asText());
            }
        } else {
            throw new Exception("not yet implemented. So far only support for fields and String or Number literals");
        }
        return operand;
    }

    private  static SqlOperator extractOperator(JsonNode node){
        SqlOperator sqlOperator;
        switch (node.asText()){
            case "GREATER_THAN": sqlOperator = SqlStdOperatorTable.GREATER_THAN; break;
            case "GREATER_THAN_OR_EQUAL": sqlOperator = SqlStdOperatorTable.GREATER_THAN_OR_EQUAL; break;
            case "EQUALS": sqlOperator = SqlStdOperatorTable.EQUALS; break;
            case "LESS_THAN": sqlOperator = SqlStdOperatorTable.LESS_THAN; break;
            case "LESS_THAN_OR_EQUAL": sqlOperator = SqlStdOperatorTable.LESS_THAN_OR_EQUAL; break;
            case "NOT_EQUALS": sqlOperator = SqlStdOperatorTable.NOT_EQUALS; break;
            default: throw new IllegalArgumentException("operator not yet implemented");
        }

        return sqlOperator;
    }
}
