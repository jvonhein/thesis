package com.agora.joscha.execution;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class EMRunner {
    public static void main(String[] args){
        String pathToIqr = "/Users/joschavonhein/Workspace/thesis/project/src/main/resources/Vaccine-Crime-Plan-localhost.json";
        if(args.length == 1){
            pathToIqr=args[0];
        }

        // Override the configuration of the port
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", 1603);
        Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());
        //Config config = ConfigFactory.load();

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

    private static class RootBehaviorEM {
        static Behavior<Void> create(String iqr) {
            return Behaviors.setup(context -> {
                context.spawn(ExecutionManager.create(iqr), "ExecutionManager");
                return Behaviors.empty();
            });
        }
    }
}
