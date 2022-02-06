package other;

import java.util.function.Function;

public class RandomFunction implements Function<String, Integer> {

    @Override
    public Integer apply(String s) {
        return s.length();
    }
}
