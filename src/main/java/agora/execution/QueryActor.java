package agora.execution;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.sql.Connection;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * Temporary Actor which is responsible for the execution of a Query for a single execution engine.
 * It's responsibilities are to translate the query, send relevant parts of the query to 'upstream' NodeExecutors, wait for the response of
 * these actors (if those are a prerequisite to perform the query), setup the query if necessary (like adding a foreign table), execute the query
 * on the provided execution engine and inform downsteram NodeExecutors when successfull if necessary.
 */
public class QueryActor extends AbstractBehavior<QueryActor.QueryMessage> {
    public interface QueryMessage{}

    private final TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors;
    private final String iqr;
    private Connection conn; // jdbc connection to execution engine / database
    private final ExecutionEngine engine;
    private final HashMap<Integer, ActorRef<QueryMessage>> workloadIdToQueryActorMap; // Utility Map to be able to send message to other QueryActors directly (without going through NodeExecutor actor first)

    private QueryActor(ActorContext<QueryMessage> context, TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors, String iqr, Connection conn, ExecutionEngine engine, HashMap<Integer, ActorRef<QueryMessage>> workloadIdToQueryActorMap) {
        super(context);
        this.registeredNodeExecutors = registeredNodeExecutors;
        this.iqr = iqr;
        this.conn = conn;
        this.engine = engine;
        this.workloadIdToQueryActorMap = workloadIdToQueryActorMap;
    }

    public static Behavior<QueryMessage> create(TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> nodeExecutors, String iqr, Connection conn, ExecutionEngine engine, HashMap<Integer, ActorRef<QueryMessage>> workloadIdToQueryActorMap){
        return Behaviors.setup(context -> new QueryActor(context, nodeExecutors, iqr, conn, engine, workloadIdToQueryActorMap));
    }

    private void readLocalExecutionPlan() {

    }


    @Override
    public Receive<QueryMessage> createReceive() {
        return null;
    }
}
