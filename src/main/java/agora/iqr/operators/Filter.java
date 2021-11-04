package agora.iqr.operators;


import agora.iqr.AgoraOperator;

import java.util.function.Function;

public class Filter<Input> extends AgoraOperator {

    // TODO: refine further (maybe predicate per indice
    Function<Input, Boolean> predicate;
}
