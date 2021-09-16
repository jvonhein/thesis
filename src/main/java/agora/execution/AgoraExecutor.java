package agora.execution;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.cluster.typed.Cluster;
import scala.collection.Iterator;
import scala.collection.immutable.Set;

/**
 * Global Optimizer
 * Should be a Singleton Actor and entry point for user queries.
 * Needs to be subscribed to ClusterEvents and keep tabs on all available execution engines & assets within the Agora Ecosystem.
 *
 * Should spawn a separate Actor per user query
 */
public class AgoraExecutor extends AbstractBehavior<AgoraExecutor.Query> {

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
    // Set of LocalExecutors in the System
    // Map of Assets to LocalExecutors (in order to be able be able to perform the Optimization process and map operations to localExecutors)


    // Create & Constructor
    private AgoraExecutor(ActorContext<AgoraExecutor.Query> context) {
        super(context);
        this.context = context;
        this.listingResponseAdapter = context.messageAdapter(Receptionist.Listing.class, ListingResponse::new);
        Cluster cluster = Cluster.get(context.getSystem());

        context.getSystem()
                .receptionist()
                .tell(Receptionist.subscribe(ExecutionProvider.localExecutorServiceKey, listingResponseAdapter));
    }

    public static Behavior<Query> create() {
        return Behaviors.setup(AgoraExecutor::new);
    }

    // Behavior
    private Behavior<Query> onQuery(Query msg){
        context.getLog().info("Message received");
        return this;
    }

    private Behavior<Query> onListingResponse(ListingResponse msg){
        context.getLog().info("\n\nListing Response received:\n");
        final Set<ActorRef<ExecutionProvider.ExecutorMessage>> actorRefSet = msg.listing.allServiceInstances(ExecutionProvider.localExecutorServiceKey);
        final Iterator<ActorRef<ExecutionProvider.ExecutorMessage>> iterator = actorRefSet.iterator();
        while (iterator.hasNext()){
            context.getLog().info("{}", iterator.next().toString());
        }
        context.getLog().info("\n\n");
        return this;
    }

    @Override
    public Receive<AgoraExecutor.Query> createReceive() {

        return newReceiveBuilder()
                .onMessage(ListingResponse.class, this::onListingResponse)
                .build();
    }
}
