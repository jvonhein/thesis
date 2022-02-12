package agora.execution;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.cluster.typed.Cluster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import scala.collection.Iterator;
import scala.collection.immutable.Set;

import java.util.TreeSet;

import static agora.execution.NodeExecutor.localExecutorServiceKey;

/**
 * Global Optimizer
 * Should be a Singleton Actor and entry point for user queries.
 *
 *
 * Is completely mocked up. Will send the initial iqr (given by constructor) to the node-executor once he has received his ActorRef from the Receptionist
 */
public class ExecutionManager extends AbstractBehavior<ExecutionManager.Query> {

    // Messages
    interface Query {}

    public static class ServiceMessage implements Query {}

    /**
     * necessary Message Adapter so this Actor can actually receive the Responses from the Receptionist
     */
    private static class ListingResponse implements Query {
        final Receptionist.Listing listing;

        private ListingResponse(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    // State
    private final ActorContext<Query> context;
    private final ActorRef<Receptionist.Listing> listingResponseAdapter;
    String nodeExecutorActorPath;
    JsonNode iqr;
    private TreeSet<ActorRef<NodeExecutor.ExecutorMessage>> registeredNodeExecutors = new TreeSet<>();
    // Set of LocalExecutors in the System
    // Map of Assets to LocalExecutors (in order to be able be able to perform the Optimization process and map operations to localExecutors)


    // Create & Constructor
    private ExecutionManager(ActorContext<ExecutionManager.Query> context, String iqr) {
        super(context);
        this.context = context;
        this.listingResponseAdapter = context.messageAdapter(Receptionist.Listing.class, ListingResponse::new);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(iqr);
            this.iqr=node;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        nodeExecutorActorPath = node.get("workload").get(0).path("executor-actorRef").asText();

        context.getSystem()
                .receptionist()
                .tell(Receptionist.subscribe(localExecutorServiceKey, listingResponseAdapter));
    }

    public static Behavior<Query> create(String iqr) {
        return Behaviors.setup(context -> new ExecutionManager(context, iqr));
    }


    private Behavior<Query> onListingResponse(ListingResponse msg){

        final Set<ActorRef<NodeExecutor.ExecutorMessage>> newRegisteredNodeExecutors = msg.listing.allServiceInstances(localExecutorServiceKey);
        newRegisteredNodeExecutors.foreach(actorRef -> {
            if (!this.registeredNodeExecutors.contains(actorRef)){
                this.registeredNodeExecutors.add(actorRef);
                if (actorRef.path().toString().equals(nodeExecutorActorPath)) {
                    actorRef.tell(new NodeExecutor.AgoraQuery(iqr, null, actorRef.path().toString()));
                }
                getContext().getLog().info("\n\n\nnew listing received. all current registered nodeExecutors: {}", this.registeredNodeExecutors);
                context.getLog().info("\n\n");
            }
            return null;
        });

        return this;
    }

    @Override
    public Receive<ExecutionManager.Query> createReceive() {

        return newReceiveBuilder()
                .onMessage(ListingResponse.class, this::onListingResponse)
                .build();
    }
}
