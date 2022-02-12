FROM openjdk:latest

RUN mkdir jars
COPY thesis-1.0-SNAPSHOT.jar /jars
COPY mariadb-java-client-2.7.1.jar /jars
COPY postgresql-42.2.18.jar /jars

ENV CLASSPATH=/jars/mariadb-java-client-2.7.1.jar:/jars/postgresql-42.2.18.jar:/thesis-1.0-SNAPSHOT.jar:.

# Run NodeExecutor when container starts - Execution Manager has to be started manually!
CMD ["java agora.execution.NESTart"]