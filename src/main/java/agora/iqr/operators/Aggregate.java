package agora.iqr.operators;

public class Aggregate {

    AggregateType aggregateType;

    enum AggregateType {
        COUNT, SUM, AVG, MIN, MAX
    }
}
