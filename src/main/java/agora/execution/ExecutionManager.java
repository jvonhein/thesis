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
    String iqr;
    // Set of LocalExecutors in the System
    // Map of Assets to LocalExecutors (in order to be able be able to perform the Optimization process and map operations to localExecutors)


    // Create & Constructor
    private ExecutionManager(ActorContext<ExecutionManager.Query> context, String iqr) {
        super(context);
        this.context = context;
        this.listingResponseAdapter = context.messageAdapter(Receptionist.Listing.class, ListingResponse::new);
        this.iqr = iqr;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(iqr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        nodeExecutorActorPath = node.get("workload").get(0).path("executor-actorRef").asText();

        context.getSystem()
                .receptionist()
                .tell(Receptionist.subscribe(NodeExecutor.localExecutorServiceKey, listingResponseAdapter));
    }

    public static Behavior<Query> create(String iqr) {
        return Behaviors.setup(context -> new ExecutionManager(context, iqr));
    }


    private Behavior<Query> onListingResponse(ListingResponse msg){
        context.getLog().info("\n\nListing Response received:\n");
        final Set<ActorRef<NodeExecutor.ExecutorMessage>> actorRefSet = msg.listing.allServiceInstances(NodeExecutor.localExecutorServiceKey);
        final Iterator<ActorRef<NodeExecutor.ExecutorMessage>> iterator = actorRefSet.iterator();
        while (iterator.hasNext()){
            final ActorRef<NodeExecutor.ExecutorMessage> nodeExecutor = iterator.next();
            if (nodeExecutor.path().equals(nodeExecutorActorPath)){
                nodeExecutor.tell(new NodeExecutor.AgoraQuery(iqr, null));
            }
            context.getLog().info("{}", nodeExecutor.toString());
        }
        context.getLog().info("\n\n");
        return this;
    }

    @Override
    public Receive<ExecutionManager.Query> createReceive() {

        return newReceiveBuilder()
                .onMessage(ListingResponse.class, this::onListingResponse)
                .build();
    }
}
