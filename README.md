This project contains the code for a prototype NodeExecutor capable of processing relational algebra workloads.

Additionally, all the components to recreate the Example/Experiment presented in my thesis are also part of the project.

## Running the Example using Docker
In order to run the experiment in the Docker environment, the csv-files containing data for the tables 'crimes' and 'criminals' have to be added to the /Docker/MariaDB/tables folder.

In the docker-compose file the volume needs to point to the folder where the vaccine_data.csv file is located.

The jar-file of this project, (created by 'mvn clean package' command) has to be moved to the /Docker/NodeExecutor folder.

Run the docker environment by calling 'docker-compose up'.

Make sure all tables are created and data is imported from the csv files in the database-containers & odbc is configured correctly.

NodeExecutors should already be only, in order to run a job / experiment, use 'docker exec -it ne1 bash' to have terminal access to the ne1 container. Run the following command to execute a given job

    java -cp /jars/thesis-1.0-SNAPSHOT.jar com.agora.joscha.execution.EMRunner /plan-docker.json


