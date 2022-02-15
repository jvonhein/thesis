package com.agora.joscha.iqr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.RelBuilder;


public class RelFactory {

    @Deprecated
    public static RelNode translateJson(String json, String executorActorRef, RelBuilder relBuilder) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        // find the correct query in the iqr-json
        final JsonNode workloads = node.path("workload");
        for (JsonNode workload: workloads){
            // found the subplan of this executor
            if (workload.path("executor-actorRef").asText().equals(executorActorRef)){
                final JsonNode operators = workload.path("local-execution-plan").get(0).get("operators");

                return buildRelNodeRecursively(operators, 0, relBuilder);
            }
        }

        return null;
    }

    public static RelNode buildRelNodeRecursively(JsonNode query, int index, RelBuilder relBuilder) throws Exception {
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
            conditions[i] = relBuilder.call(sqlOperator, relBuilder.field(2, 0, leftOrdinal), relBuilder.field(2, 1, rightOrdinal));
        }

        if (conditions.length == 1){
            return relBuilder.join(joinRelType, conditions[0]).build();
        } else {
            throw new Exception("currently only support for 1 join condition");
        }
    }


    private static RelNode buildProject(JsonNode operator, RelBuilder relBuilder) throws Exception {
        int numOutColumns = operator.path("output").size();
        RexNode[] output = new RexNode[numOutColumns];
        for (int i=0; i< numOutColumns; i++){
            JsonNode column = operator.get("output").get(i);
            // simple field projection
            if (column.get("type").asText().equals("field")){
                output[i] = relBuilder.field(column.get("fieldOrdinal").asInt());
            } else if (column.get("type").asText().equals("expression")){
                if (column.has("CASE")){
                    output[i] = buildCASEExpression(column.get("CASE"), relBuilder);
                }
            }
        }
        return relBuilder.project(output).build();
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

            RexNode[] fields = new RexNode[aggCallNode.path("indices").size()];
            for (int j = 0; j < fields.length; j++) {
                fields[j] = relBuilder.field(aggCallNode.path("indices").path(j).asInt());
            }

            switch (aggCallNode.path("type").asText()){
                case "Count":
                    aggCalls[i] = relBuilder.count(distinct, alias, fields);
                    break;
                case "AVG":
                    aggCalls[i] = relBuilder.avg(distinct, alias, fields[0]);
                    break;
                case "MIN":
                    aggCalls[i] = relBuilder.min(alias, fields[0]);
                case "MAX":
                    aggCalls[i] = relBuilder.max(alias, fields[0]);
                // TODO: implement more aggregations
            }

        }

        return relBuilder
                .aggregate(relBuilder.groupKey(groupKeys), aggCalls)
                .build();
    }

    // builds a SQL CASE expression from jsonNode
    private static RexNode buildCASEExpression(JsonNode node, RelBuilder relBuilder) throws Exception {
        //sanity check
        if (node.has("when") && node.has("then") && node.has("else") && node.get("when").size()==node.get("then").size()){
            RexNode[] operands = new RexNode[(node.get("when").size()*2)+1];
            // defines when and then expression
            for (int i = 0; i < node.get("when").size(); i++) {
                SqlOperator whenOperator = extractOperator(node.get("when").get(i).get("operator"));
                RexNode whenOperand0 = extractOperand(node.get("when").get(i).get("operands").get(0), relBuilder);
                RexNode whenOperand1 = extractOperand(node.get("when").get(i).get("operands").get(1), relBuilder);
                operands[i*2]=relBuilder.call(whenOperator, whenOperand0, whenOperand1);
                RexNode then = extractOperand(node.get("then").get(i), relBuilder);
                operands[(i*2)+1]=then;
            }
            // define else expression
            operands[operands.length-1] = extractOperand(node.get("else"), relBuilder);

            return relBuilder.call(SqlStdOperatorTable.CASE, operands);
        } else {
            throw new Exception("sanity check for CASE expression failed!");
        }
    }


    // helper method to correctly extract the operand correctly from a json-node
    private static RexNode extractOperand(JsonNode node, RelBuilder relBuilder) throws Exception {
        if (node.path("type").asText().equals("field")){
            final int field = node.path("fieldOrdinal").asInt();
            return relBuilder.field(field);
        } else if (node.path("type").asText().equals("literal")){
            if (node.path("value").isNull() || node.path("dataType").asText().equals("Null")){
                return relBuilder.literal(null);
            } else if (node.path("dataType").asText().equals("Int")){
                return relBuilder.literal(node.path("value").asInt());
            } else if (node.path("dataType").asText().equals("String")){
                return relBuilder.literal(node.path("value").asText());
            }
        } else {
            throw new Exception("not yet implemented. So far only support for fields and String or Number literals");
        }
        return null;
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
