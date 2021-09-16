package agora.execution;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Demo {

    public static void main(String[] args) {
        if (args.length == 0) {
            startup("executor", 25251);
            startup("executor", 25252);
            startup("executor", 0);
            startup("globalOptimizer", 0);
        } else {
            if (args.length != 2)
                throw new IllegalArgumentException("Usage: role port");
            startup(args[0], Integer.parseInt(args[1]));
        }
    }

    private static class RootBehavior {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {
                Cluster cluster = Cluster.get(context.getSystem());

                if (cluster.selfMember().hasRole("executor")) {
                    context.spawn(ExecutionProvider.create(), "Executor");
                }
                if (cluster.selfMember().hasRole("globalOptimizer")) {
                    context.spawn(AgoraExecutor.create(), "AgoraExecutor");
                }

                return Behaviors.empty();
            });
        }
    }

    private static void startup(String role, int port) {

        // Override the configuration of the port
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", port);
        overrides.put("akka.cluster.roles", Collections.singletonList(role));

        Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load("stats"));

        ActorSystem<Void> system = ActorSystem.create(RootBehavior.create(), "ClusterSystem", config);
    }
}
