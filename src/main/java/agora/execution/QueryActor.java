package agora.execution;

import agora.JsonSerializable;
import agora.iqr.TestSchema;
import akka.actor.Status;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Temporary Actor which is responsible for the execution of a Query for a single execution engine.
 * It's responsibilities are to translate the query, send relevant parts of the query to 'upstream' NodeExecutors, wait for the response of
 * these actors (if those are a prerequisite to perform the query), setup the query if necessary (like adding a foreign table), execute the query
 * on the provided execution engine and inform downsteram NodeExecutors when successfull if necessary.
 */
public class QueryActor extends AbstractBehavior<QueryActor.QueryMessage> {
    public interface QueryMessage{}

    public static class RemoteExecutionFinished implements QueryMessage, JsonSerializable {
        public final Status status;
        public final int queryId;
        public final int workloadId;
        public final int localExecutionPlanIndex;
        public final ActorRef<QueryMessage> senderQueryActor;

        public RemoteExecutionFinished(Status status, int queryId, int workloadId, int localExecutionPlanIndex, ActorRef<QueryMessage> senderQueryActor) {
            this.status = status;
            this.queryId = queryId;
            this.workloadId = workloadId;
            this.localExecutionPlanIndex = localExecutionPlanIndex;
            this.senderQueryActor = senderQueryActor;
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

    private final TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors;
    private final String iqr;
    private Connection conn; // jdbc connection to execution engine / database
    private final ExecutionEngine engine;
    private final HashMap<Integer, ActorRef<QueryMessage>> workloadIdToQueryActorMap; // Utility Map to be able to send message to other QueryActors directly (without going through NodeExecutor actor first)
    private final ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef; // need to know which node Executor this Query belongs to
    private TestSchema schema = new TestSchema();
    private int workloadId = -1;
    private JsonNode workload;
    private HashMap<Integer, Requirement> requirements; //<this.localEcecutionPlanIndex, Requirement>
    private HashMap<Integer, String> workloadToHostnameMap = new HashMap<>();
    String hostname=System.


    private QueryActor(ActorContext<QueryMessage> context, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors, String iqr, Connection conn, ExecutionEngine engine, HashMap<Integer, ActorRef<QueryMessage>> workloadIdToQueryActorMap, ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef) {
        super(context);
        this.registeredNodeExecutors = registeredNodeExecutors;
        this.iqr = iqr;
        this.conn = conn;
        this.engine = engine;
        this.workloadIdToQueryActorMap = workloadIdToQueryActorMap;
        this.nodeExecutorActorRef = nodeExecutorActorRef;
    }

    public static Behavior<QueryMessage> create(TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> nodeExecutors, String iqr, Connection conn, ExecutionEngine engine, HashMap<Integer, ActorRef<QueryMessage>> workloadIdToQueryActorMap, ActorRef<NodeExecutor.ExecutorMessage> nodeExecutorActorRef){
        return Behaviors.setup(context -> new QueryActor(context, nodeExecutors, iqr, conn, engine, workloadIdToQueryActorMap, nodeExecutorActorRef));
    }

    private void processIqr() throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(iqr);
        int numWorkloads = node.get("workload").size();
        // find workload this query is responsible for
        for (int i = 0; i < numWorkloads; i++) {
            if (node.get("workload").get(i).get("executor-actorRef").asText().equals(nodeExecutorActorRef)){
                this.workload = node.get("workload").get(i);
                this.workloadId = workload.get("id").asInt();
            }
        }
        if (workloadId==-1){
            getContext().getLog().error("QueryActor received IQR with no workload for it's Node Executor");
        }else {
            Iterator<JsonNode> executionPlanIterator = workload.get("local-execution-plan").elements();
            while (executionPlanIterator.hasNext()){
                JsonNode localExecutionPlan = executionPlanIterator.next();

                // check requirements
                if (localExecutionPlan.has("start-condition") && localExecutionPlan.get("start-condition").has("requirements")){
                    localExecutionPlan.get("start-condition").get("requirements").forEach(requirement -> {
                        int reqWorkloadId = requirement.get("message").get("workload-id").asInt();
                        int reqExIndex = requirement.get("message").get("local-execution-plan-index").asInt();
                        this.requirements.put(localExecutionPlan.get("id").asInt(), new Requirement(reqWorkloadId, reqExIndex));
                    });
                }
                // if no requirements: start immediately


            }
        }
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
                String hostname = workloadToHostnameMap.get(view.get("workload-id").asInt());
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



    @Override
    public Receive<QueryMessage> createReceive() {
        return null;
    }
}
