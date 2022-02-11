package agora.execution;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class Demo {

    public static void main(String[] args) {
        if(args.length == 1){
            // Override the configuration of the port
            Map<String, Object> overrides = new HashMap<>();
            overrides.put("akka.remote.artery.canonical.port", 1603);
            Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());
            //Config config = ConfigFactory.load();
            String pathToIqr = args[0];
            try {
                BufferedReader reader = new BufferedReader(new FileReader(pathToIqr));
                StringBuilder resultStringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
                }
                reader.close();
                String iqr = resultStringBuilder.toString();

                ActorSystem<Void> system = ActorSystem.create(RootBehaviorEM.create(iqr), "clustering-cluster", config);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        if (args.length == 0) {

            // in docker environment
            if (System.getenv("CLUSTER_PORT")!=null){
                Config config = ConfigFactory.load();
                ActorSystem<Void> system = ActorSystem.create(RootBehaviorNE.create(), "clustering-cluster", config);
            } else {
                // only for test in local machine
                startupNodeExecutor(1600);
                startupNodeExecutor(1601);
                startupNodeExecutor(1602);
            }
        }
    }

    private static class RootBehaviorEM {
        static Behavior<Void> create(String iqr) {
            return Behaviors.setup(context -> {
                context.spawn(ExecutionManager.create(iqr), "ExecutionManager");
                return Behaviors.empty();
            });
        }
    }

    private static class RootBehaviorNE {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {

                final String jdbcUrl = System.getenv("JDBC_URL") != null ? System.getenv("JDBC_URL") : "testurl";
                final String jdbcUser = System.getenv("JDBC_USER") != null ? System.getenv("JDBC_USER") : "testuser";
                final String jdbcPw = System.getenv("JDBC_PW") != null ? System.getenv("JDBC_PW") : "testpw";
                final ExecutionEngine engine = System.getenv("ENGINE") != null ? getEngine(System.getenv("JDBC_URL")) : ExecutionEngine.MARIADB;

                context.spawn(NodeExecutor.create(jdbcUrl, jdbcUser, jdbcPw, engine), "Node-Executor_for_"+jdbcUser);

                return Behaviors.empty();
            });
        }
    }

    private static void startupNodeExecutor(int port) {

        // Override the configuration of the port
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", port);
        Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());

        ActorSystem<Void> system = ActorSystem.create(RootBehaviorNE.create(), "clustering-cluster", config);
    }

    private static ExecutionEngine getEngine(String s){
        switch (s){
            case "postgres": return ExecutionEngine.POSTGRES;
            case "mariadb": return ExecutionEngine.MARIADB;
            default: return null;
        }
    }
}
