package other;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FunctionSerializationTest {
    public static void main(String[] args){

        RandomFunction function = new RandomFunction();

        ObjectMapper mapper = new ObjectMapper();
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("target/serialized_function.json"));
            mapper.writeValue(bufferedWriter, function);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
