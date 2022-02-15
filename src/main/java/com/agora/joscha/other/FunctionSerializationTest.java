package com.agora.joscha.other;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionSerializationTest {
    public static void main(String[] args){

        // sqlString.split(",|AS\s`\$f\d+`", outColNames.size());
        Pattern p = Pattern.compile(",|AS `\\$f[0-9]+`");
        Matcher m = p.matcher("AS `$f2`");
        Matcher n = p.matcher(",");
        System.out.println(m.matches());
        System.out.println(n.matches());
    }
}
