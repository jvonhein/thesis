package agora.iqr.operators;

import agora.iqr.AgoraOperator;

import java.util.function.Function;

public class Map<I,O> extends AgoraOperator {

    // TODO: how to persist the function in json
    Function<I,O> transformation;
}
