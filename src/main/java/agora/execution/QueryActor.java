package agora.execution;

import agora.JsonSerializable;
import agora.iqr.RelFactory;
import agora.iqr.TestSchema;
import akka.actor.Status;
import akka.actor.Status.Success;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rel2sql.SqlImplementor;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Temporary Actor which is responsible for the execution of a Query for a single execution engine.
 * It's responsibilities are to translate the query, send relevant parts of the query to 'upstream' NodeExecutors, wait for the response of
 * these actors (if those are a prerequisite to perform the query), setup the query if necessary (like adding a foreign table), execute the query
 * on the provided execution engine and inform downsteram NodeExecutors when successfull if necessary.
 */
public class QueryActor extends AbstractBehavior<QueryActor.QueryMessage> {
    public interface QueryMessage{}

    public static class RemoteExecutionFinished implements QueryMessage, NodeExecutor.ExecutorMessage, JsonSerializable {
        public final String status;
        public final int queryId;
        public final int workloadId;
        public final int localExecutionPlanIndex;
        public final ActorRef<QueryMessage> senderQueryActor;
        public String hostname;

        public RemoteExecutionFinished(String status, int queryId, int workloadId, int localExecutionPlanIndex, ActorRef<QueryMessage> senderQueryActor, String hostname) {
            this.status = status;
            this.queryId = queryId;
            this.workloadId = workloadId;
            this.localExecutionPlanIndex = localExecutionPlanIndex;
            this.senderQueryActor = senderQueryActor;
            this.hostname=hostname;
        }
    }

    public static class Requirement{
        final int workloadId;
        final int localExecutionPlanIndex;

        public Requirement(int workloadId, int localExecutionPlanIndex) {
            this.workloadId = workloadId;
            this.localExecutionPlanIndex = localExecutionPlanIndex;
        }
    }

    private Connection conn; // jdbc connection to execution engine / database
    private final ExecutionEngine engine;
    String hostname=System.getenv("EEHOSTNAME");
    private final ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef; // need to know which node Executor this Query belongs to
    private TestSchema schema = new TestSchema();
    private int workloadId = -1;
    private JsonNode workload;
    private final int queryID;

    ArrayList<Requirement>[] requirements;
    String[] hostnamesByWorkload;
    ActorRef<NodeExecutor.ExecutorMessage>[] nodeExecutorsByWorkload;
    private ActorRef<QueryMessage>[] queryActorRefsByWorkload;



    private QueryActor(int id, ActorContext<QueryMessage> context, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors, String iqr, Connection conn, ExecutionEngine engine,
                       ActorRef<QueryMessage>[] queryActorRefs, ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef) {
        super(context);
        this.conn = conn;
        this.engine = engine;
        this.queryActorRefsByWorkload = queryActorRefs;
        this.nodeExecutorActorRef = nodeExecutorActorRef;
        this.queryID = id;

        try {
            processIqr(iqr, registeredNodeExecutors);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static Behavior<QueryMessage> create(int id, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> nodeExecutors, String iqr, Connection conn, ExecutionEngine engine,
                                                ActorRef<QueryMessage>[] queryActorRefs, ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef){
        return Behaviors.setup(context -> new QueryActor(id, context, nodeExecutors, iqr, conn, engine, queryActorRefs, nodeExecutorActorRef));
    }

    public static Behavior<QueryMessage> create(int id, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> nodeExecutors, String iqr, Connection conn, ExecutionEngine engine, ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef){
        return Behaviors.setup(context -> new QueryActor(id, context, nodeExecutors, iqr, conn, engine, null, nodeExecutorActorRef));
    }

    private void processIqr(String iqr, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(iqr);
        int numWorkloads = node.get("workload").size();
        hostnamesByWorkload = new String[numWorkloads];
        String[] executorActorPathsByWorkload = new String[numWorkloads];
        nodeExecutorsByWorkload = new ActorRef[numWorkloads];
        if (queryActorRefsByWorkload==null)
            queryActorRefsByWorkload = new ActorRef[numWorkloads];
        // find workload this query is responsible for
        for (int i = 0; i < numWorkloads; i++) {
            executorActorPathsByWorkload[i] = node.get("workload").get(i).get("executor-actorRef").asText();
            if (node.get("workload").get(i).get("executor-actorRef").asText().equals(nodeExecutorActorRef)){
                this.workload = node.get("workload").get(i);
                this.workloadId = workload.get("id").asInt();
                hostnamesByWorkload[i] = hostname;
                queryActorRefsByWorkload[i]=getContext().getSelf();
            }
        }
        nodeExecutorsByWorkload = findActorRefsByPath(executorActorPathsByWorkload, registeredNodeExecutors);
        if (workloadId==-1){
            getContext().getLog().error("QueryActor received IQR with no workload for it's Node Executor");
        }else {
            int numLocalExecutionPlans = workload.get("local-execution-plan").size();
            requirements = new ArrayList[numLocalExecutionPlans];
            for (int i = 0; i < numLocalExecutionPlans; i++) {
                JsonNode localExecutionPlan = workload.get("local-execution-plan").get(i);
                requirements[i]=new ArrayList<>();
                // check requirements
                if (localExecutionPlan.has("start-condition") && localExecutionPlan.get("start-condition").has("requirements")){
                    localExecutionPlan.get("start-condition").get("requirements").forEach(requirement -> {
                        int reqWorkloadId = requirement.get("message").get("workload-id").asInt();
                        int reqExIndex = requirement.get("message").get("local-execution-plan-index").asInt();
                        Requirement r = new Requirement(reqWorkloadId, reqExIndex);
                        this.requirements[localExecutionPlan.get("id").asInt()].add(r);
                    });
                } else {
                    // start query immediately
                    executePlan(localExecutionPlan);
                }
            }
        }
    }



    private void executePlan(JsonNode localExecutionPlan){

        // preparation
        if(localExecutionPlan.has("input") && localExecutionPlan.get("input").size() > 0){
            gatherInput(localExecutionPlan.get("input"));
        }
        // translate query
        final SchemaPlus rootSchema = Frameworks.createRootSchema(true).add("vaccine_crime", schema);
        final Frameworks.ConfigBuilder configBuilder = Frameworks.newConfigBuilder();
        final FrameworkConfig config = configBuilder
                .defaultSchema(rootSchema)
                .build();
        final RelBuilder relBuilder = RelBuilder.create(config);
        final SqlDialect dialect;
        switch (engine){
            case MARIADB:
                dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
                break;
            case POSTGRES:
                dialect = SqlDialect.DatabaseProduct.POSTGRESQL.getDialect();
                break;
            default:
                dialect = SqlDialect.DatabaseProduct.CALCITE.getDialect();
            // can add more dialects in the future
        }
        final RelToSqlConverter relToSqlConverter = new RelToSqlConverter(dialect);
        try {
            final RelNode root = RelFactory.buildRelNodeRecursively(localExecutionPlan.get("operators"), 0, relBuilder);
            String sqlString = relToSqlConverter.visitRoot(root).toString();
            final Statement statement = conn.createStatement();
            JsonNode output = localExecutionPlan.get("output").get(0);
            // check output to see what to do with the query - in example either save to file or save as view (which changes the statement slightly)
            switch (output.get("type").asText()){
                case "file":
                    ResultSet rs = statement.executeQuery(sqlString);
                    String path = output.get("path").asText();
                    FileWriter fileWriter = new FileWriter(new File(path));
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    int numColumns = output.get("columnTypes").size();
                    String[] columnTypes = new String[numColumns];
                    for (int i = 0; i < numColumns; i++) {
                        // write header
                        if (output.has("columnNames")){
                            bufferedWriter.write(output.get("columnNames").get(i).asText());
                            if (i < numColumns-1)
                                bufferedWriter.write(" | ");
                            else
                                bufferedWriter.newLine();
                        }
                        columnTypes[i]=output.get("columnTypes").get(i).asText();
                    }

                    while (rs.next()){
                        for (int i = 0; i < numColumns; i++) {
                            bufferedWriter.write(getColumn(i, columnTypes, rs));
                            if (i < numColumns-1)
                                bufferedWriter.write(" | ");
                            else
                                bufferedWriter.newLine();
                        }
                    }
                    bufferedWriter.flush();
                    bufferedWriter.close();
                    break;
                case "view":
                    String viewName = localExecutionPlan.get("output").get("name").asText();
                    sqlString = "CREATE VIEW "+viewName+" AS "+sqlString;
                    statement.executeQuery(sqlString);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // possibly inform downstream NodeExecutors
        if(localExecutionPlan.has("on-success") && localExecutionPlan.get("on-success").size() > 0){
            localExecutionPlan.get("on-success").forEach(action -> {
                if (action.has("message")){
                    int to = action.get("message").get("to").asInt();
                    RemoteExecutionFinished msg = new RemoteExecutionFinished("success", this.queryID, workloadId, localExecutionPlan.get("id").asInt(), getContext().getSelf(), hostname);
                    if (queryActorRefsByWorkload[to] != null){
                        queryActorRefsByWorkload[to].tell(msg);
                    } else {
                        nodeExecutorsByWorkload[to].tell(msg);
                    }
                }
            });
        }

    }

    private String getColumn(int i, String[] columnTypes, ResultSet rs) throws SQLException {
        String result = "";
        switch (columnTypes[i]){
            case "VARCHAR": result = rs.getString(i);
                break;
            case "INTEGER": result = ""+rs.getInt(i);
                break;
            case "DOUBLE": result = ""+rs.getDouble(i);
                break;
            case "DATE": result = rs.getDate(i).toString();
                break;
            default:break;
        }
        return result;
    }

    // make sure the query can actually run. creates foreign tables for instance
    private void gatherInput(JsonNode inputs){
        // adds the remote view to schema so calcite can actually translate the operators into a sql-query which can be run on execution engine
        inputs.forEach(input -> {
            if (input.get("type").asText().equals("remoteView")){
                JsonNode view = input.get("view");
                String localNewName = view.get("local-new-name").asText();
                schema.addTableFromColumns(view.get("columnTypes"), localNewName);

                String remoteViewName = input.get("remote-view-name").asText();
                String hostname = hostnamesByWorkload[view.get("workload-id").asInt()];
                String database = view.get("database").asText();
                String sqlStatement = "";

                switch (engine){
                    case MARIADB:
                        // uid and pwd hardcoded for now
                        String remoteEngine = view.get("remote-execution-engine").asText();
                        if (remoteEngine.equals("mariadb")){
                            sqlStatement = "CREATE TABLE "+localNewName+" ENGINE=CONNECT DEFAULT CHARSET=utf8mb4 CONNECTION='mysql://mariadb:123456@"+hostname+"/"+database+"/"+remoteViewName+"' TABLE_TYPE=MYSQL;";
                        } else if (remoteEngine.equals("postgres")){
                            sqlStatement = "CREATE TABLE "+localNewName+" engine=connect table_type=ODBC block_size=10 tabname='"+remoteViewName+"' CONNECTION='DRIVER={PostgreSQL Unicode};" +
                                    "Server="+hostname+";UID=odbc_user;PWD=password;Database="+database+"';";
                        }

                        break;
                    case POSTGRES:
                        sqlStatement = "IMPORT FOREIGN SCHEMA test FROM SERVER "+hostname+ "INTO PUBLIC OPTIONS (" +
                                "odbc_DATABASE" + database + ", table '"+localNewName+"', sql_query 'select * from "+remoteViewName+"' );";

                }

                try {
                    final Statement statement = conn.createStatement();
                    statement.executeQuery(sqlStatement);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // primitive brute force way to match the actor path with the respective ActorRef. Should probably be made more professional and efficient with Comparator of ActorRef by Path
    private static ActorRef<NodeExecutor.ExecutorMessage>[] findActorRefsByPath(String[] actorPaths, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> possibleRefs){
        ActorRef<NodeExecutor.ExecutorMessage>[] result = new ActorRef[actorPaths.length];
        for (int i = 0; i < actorPaths.length; i++) {
            String path = actorPaths[i];
            final Optional<ActorRef<NodeExecutor.ExecutorMessage>> match = possibleRefs.stream().filter(actorRef -> actorRef.path().equals(path)).findFirst();
            if (match.isPresent())
                result[i]=match.get();
        }
        return result;
    }

    @Override
    public Receive<QueryMessage> createReceive() {
        return null;
    }
}
