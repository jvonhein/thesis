package agora.iqr.operators;

import agora.iqr.AgoraOperator;

import java.util.function.Function;

public class Reduce<T> extends AgoraOperator {

    Function<T, T> reduceFunction;
}
