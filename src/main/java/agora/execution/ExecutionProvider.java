package agora.execution;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;

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
public class ExecutionProvider extends AbstractBehavior<ExecutionProvider.ExecutorMessage> {

    // Messages
    interface ExecutorMessage {}

    public static class IntermediateQuery {
        ActorRef<ExecutorMessage> downstreamActor;

        public IntermediateQuery(ActorRef<ExecutorMessage> downstreamActor) {
            this.downstreamActor = downstreamActor;
        }
    }

    /**
     * ServiceKey for this Type of Actor
     */
    public static final ServiceKey<ExecutorMessage> localExecutorServiceKey =
            ServiceKey.create(ExecutorMessage.class, "localExecutor");

    // State
    private final ActorContext<ExecutorMessage> context;

    private ExecutionProvider(ActorContext<ExecutorMessage> context) {
        super(context);
        this.context = context;
        Cluster cluster = Cluster.get(context.getSystem());
    }

    public static Behavior<ExecutorMessage> create(){
        return Behaviors.setup(
                context -> {
                    context.getSystem()
                            .receptionist()
                            .tell(Receptionist.register(localExecutorServiceKey, context.getSelf()));

                    return new ExecutionProvider(context);
        });
    }

    @Override
    public Receive<ExecutorMessage> createReceive() {
        return null;
    }
}
