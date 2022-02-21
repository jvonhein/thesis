package com.agora.joscha;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvTableFactory
{
    public static void main(String[] args){
        int numCrimes = 100000;
        int numCriminals = 70000;
        String folder = "/data/Joscha/tables/";
        // String folder = "src/main/resources/tables/";

        TreeMap<Integer, Land> landProbability = new TreeMap<>();
        landProbability.put(17925570, Land.NORD_RHEIN_WESTFALEN);
        landProbability.put(31065753, Land.BAYERN);
        landProbability.put(42168796, Land.BADEN_WUERTEMBERG);
        landProbability.put(50172217, Land.NIEDERSACHSEN);
        landProbability.put(56465371, Land.HESSEN);
        landProbability.put(60563762, Land.RHEINLAND_PFALZ);
        landProbability.put(64620703, Land.SACHSEN_ANHALT);
        landProbability.put(68284791, Land.BERLIN);
        landProbability.put(71195666, Land.SCHLESWIG_HOLSTEIN);
        landProbability.put(73726737, Land.BRANDENBURG);
        landProbability.put(75907421, Land.SACHSEN);
        landProbability.put(78027658, Land.THUERINGEN);
        landProbability.put(79880136, Land.HAMBURG);
        landProbability.put(81490910, Land.MECKLENBURG_VORPOMMERN);
        landProbability.put(82474901, Land.SAARLAND);
        landProbability.put(83155031, Land.BREMEN);


        // 10-7 ratio of crime to criminals
        createCimeTable(numCrimes, numCriminals, landProbability, folder);
        createCriminalTable(numCriminals, numCrimes, landProbability, folder);
        createVaccineDataTable(120000000, landProbability, folder);
    }

    private static void createVaccineDataTable(int numRows, TreeMap<Integer, Land> landProbability, String folder){
        try {
            String filename = folder+"vaccine_data.csv";
            File file = new File(filename);
            file.createNewFile();
            final Random RANDOM = new Random();
            PrimitiveIterator.OfLong longs = RANDOM.longs(1609431773893l, 1637770973940l).iterator(); // Date between 2020-01-01 and 2020-11-24 as long
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // header
            bufferedWriter.write("batch_number;shot_number;date;dummy_data;bundesland\n");

            for (int i = 0; i < numRows; i++) {
                int caseNumber = i;
                Land land = landProbability.ceilingEntry(RANDOM.nextInt(83155031)).getValue();
                Long dateAsLong = longs.next();
                Instant instance = java.time.Instant.ofEpochMilli(dateAsLong);
                final LocalDateTime localDateTime = LocalDateTime.ofInstant(instance, ZoneId.systemDefault());
                final String formattedDate = localDateTime.format(formatter);
                String details = "Lirum Larium Ipsum";
                int shotnumber = RANDOM.nextInt(2) + 1;
                bufferedWriter.write("batch"+caseNumber+";"+shotnumber+";"+formattedDate+";"+details+";"+land+"\n");
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createCimeTable(int numCrimes, int numCriminals, TreeMap<Integer, Land> landProbability, String folder){
        try {
        String filename = folder+"crime.csv";
        File file = new File(filename);
        file.createNewFile();
        final Random RANDOM = new Random();
        PrimitiveIterator.OfLong longs = RANDOM.longs(1609431773893l, 1637770973940l).iterator(); // Date between 2020-01-01 and 2020-11-24 as long
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        // header
        bufferedWriter.write("case-number;bundesland;category;date;criminal-id;victim-id;details\n");

        for (int i = 0; i < numCrimes; i++) {
            int caseNumber = i;
            Land land = landProbability.ceilingEntry(RANDOM.nextInt(83155031)).getValue();
            CrimeCategory category = CrimeCategory.randomCategory();
            Long dateAsLong = longs.next();
            Instant instance = java.time.Instant.ofEpochMilli(dateAsLong);
            final LocalDateTime localDateTime = LocalDateTime.ofInstant(instance, ZoneId.systemDefault());
            final String formattedDate = localDateTime.format(formatter);
            int criminalId = RANDOM.nextInt(numCriminals);
            int victimId = RANDOM.nextInt(numCrimes);
            String details = "Lirum Larium Ipsum";
            bufferedWriter.write(caseNumber+";"+land+";"+category+";"+formattedDate+";"+criminalId+";"+victimId+";"+details+"\n");
        }

        bufferedWriter.flush();
        bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createCriminalTable(int numCriminals, int numCrimes, TreeMap<Integer, Land> landProbability, String folder){
        try {
            String filename = folder+"criminals.csv";
            File file = new File(filename);
            file.createNewFile();
            final Random RANDOM = new Random();
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // header
            bufferedWriter.write("id;gender;age;address;vaccine_status;details\n");
            String details = "Larum Ipsum Lorum";
            for (int i = 0; i < numCriminals; i++) {
                int id = i;
                String gender = RANDOM.nextBoolean() ? "m" : "f";
                int age = RANDOM.nextInt(100);
                String adress = "random adress "+ i;
                int vaccineStatus = RANDOM.nextBoolean() ? 0 : 1;

                bufferedWriter.write(id+";"+gender+";"+age+";"+adress+";"+vaccineStatus+";"+details+"\n");
            }

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum CrimeCategory{
        murder, rape, homicide, robbery, theft, assault, fraud, vandalism, slander;

        private static final List<CrimeCategory> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();
        private static CrimeCategory randomCategory(){
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    public enum Land{
        BADEN_WUERTEMBERG,
        BAYERN,
        RHEINLAND_PFALZ,
        SAARLAND,
        HESSEN,
        NORD_RHEIN_WESTFALEN,
        THUERINGEN,
        SACHSEN,
        SACHSEN_ANHALT,
        BRANDENBURG,
        NIEDERSACHSEN,
        BERLIN,
        MECKLENBURG_VORPOMMERN,
        BREMEN,
        HAMBURG,
        SCHLESWIG_HOLSTEIN;

        private static final List<Land> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();
        private static Land randomLand(){
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }
}
