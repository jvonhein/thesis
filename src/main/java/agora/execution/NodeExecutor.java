package agora.execution;

import agora.JsonSerializable;
import agora.iqr.RelFactory;
import agora.iqr.TestSchema;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import scala.collection.immutable.HashSet;
import scala.collection.immutable.Set;

import java.util.TreeSet;

/**
 * Local Executor Actor.
 * Manages incoming Queries and the local execution environment.
 * Queries received should be in the form of logical query plans. The executor is then responsible to optimize those
 * and transform them into physical query plans or something similar the specific execution engine is able to understand.
 *
 * Parts of the query which can or should not be executed locally will be relayed to their respective remote LocalExecutors.
 *
 * All of this behavior should most likely be split up in multiple different actors, be it per query or otherwise - with this actor acting as supervisor (local rootActor).
 *
 * This Actor needs to register itself at the Receptionist to make itself known to the Ecosystem.
 * Additionally it needs to subscribe to all other LocalExecutors coming online at the receptionist so it knows how to contact them.
 */
public class NodeExecutor extends AbstractBehavior<NodeExecutor.ExecutorMessage> {

    // Messages
    interface ExecutorMessage {}

    public static class AgoraQuery implements ExecutorMessage, JsonSerializable {
        final public String iqr;
        final public ActorRef sender;

        @JsonCreator
        public AgoraQuery(String iqr, ActorRef sender) {
            this.iqr = iqr;
            this.sender = sender;
        }
    }

    private static class ListingResponse implements ExecutorMessage {
        final Receptionist.Listing listing;

        public ListingResponse(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    /**
     * ServiceKey for this Type of Actor
     */
    public static final ServiceKey<ExecutorMessage> localExecutorServiceKey =
            ServiceKey.create(ExecutorMessage.class, "localExecutor");

    // State
    private final ActorRef<Receptionist.Listing> listingResponseAdapter;
    private TreeSet<ActorRef<ExecutorMessage>> registeredNodeExecutors = new TreeSet<>();
    private String currentQuery;
    private final String jdbcUrl = System.getenv("JDBC_URL");
    private final String jdbcUser = System.getenv("JDBC_USER");
    private final String jdbcPw = System.getenv("JDBC_PW");
    private final TestSchema vaccineCrimeSchema = new TestSchema(); // schema of the local execution engine


    private NodeExecutor(ActorContext<ExecutorMessage> context) {
        super(context);
        this.listingResponseAdapter = context.messageAdapter(Receptionist.Listing.class, ListingResponse::new);
        Cluster cluster = Cluster.get(context.getSystem());

        context.getSystem().receptionist().tell(
                Receptionist.subscribe(localExecutorServiceKey, listingResponseAdapter)
        );
    }

    public static Behavior<ExecutorMessage> create(){
        return Behaviors.setup(
                context -> {
                    context.getSystem()
                            .receptionist()
                            .tell(Receptionist.register(localExecutorServiceKey, context.getSelf()));


                    return new NodeExecutor(context);
        });
    }

    private Behavior<ExecutorMessage> receiveIQR(AgoraQuery msg){
        return this;
    }

    // makes sure this actor always has a list of all node-executors in the ecosystem
    private Behavior<ExecutorMessage> onListing(Receptionist.Listing listing){
        final Set<ActorRef<ExecutorMessage>> newRegisteredNodeExecutors = listing.allServiceInstances(localExecutorServiceKey);
        newRegisteredNodeExecutors.foreach(this.registeredNodeExecutors::add);
        if (newRegisteredNodeExecutors.size() > 0)
            getContext().getLog().info("new listing received. all current registered nodeExecutors: {}", this.registeredNodeExecutors);
        return this;
    }

    @Override
    public Receive<ExecutorMessage> createReceive() {
        return null;
    }
}
