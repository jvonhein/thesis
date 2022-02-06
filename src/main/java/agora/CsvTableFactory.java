package agora;



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

        // 10-7 ratio of crime to criminals
        createCimeTable(numCrimes, numCriminals);
        createCriminalTable(numCriminals, numCrimes);
    }

    private static void createCimeTable(int numCrimes, int numCriminals){
        try {
        String filename = "src/main/resources/tables/crime.csv";
        File file = new File(filename);
        file.createNewFile();
        final Random RANDOM = new Random();
        PrimitiveIterator.OfLong longs = RANDOM.longs(1609431773893l, 1637770973940l).iterator(); // Date between 2021-01-01 and 2020-11-24 as long
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        // header
        bufferedWriter.write("land;population_total;vaccinated_firstshot;vaccinated_secondshot;percent_vaccinated_firstshot;percent_vaccinated_secondshot\n");

        for (int i = 0; i < numCrimes; i++) {
            int caseNumber = i;
            Land land = Land.randomLand();
            CrimeCategory category = CrimeCategory.randomCategory();
            Long dateAsLong = longs.next();
            Instant instance = java.time.Instant.ofEpochMilli(dateAsLong);
            final LocalDateTime localDateTime = LocalDateTime.ofInstant(instance, ZoneId.systemDefault());
            final String formattedDate = localDateTime.format(formatter);
            int criminalId = RANDOM.nextInt(80000);
            int victimId = RANDOM.nextInt(200000);
            String details = "Lirum Larium Ipsum";
            bufferedWriter.write(caseNumber+";"+land+";"+category+";"+formattedDate+";"+criminalId+";"+victimId+";"+details+"\n");
        }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createCriminalTable(int numCriminals, int numCrimes){
        try {
            String filename = "src/main/resources/tables/criminals.csv";
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum CrimeCategory{
        MORD, VERGEWALTIGUNG, TOTSCHLAG, RAUB, DIEBSTAHL, KÖRPERVERLETZUNG, BETRUG, SACHBESCHÄDIGUNG, BELEIDIGUNG;

        private static final List<CrimeCategory> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();
        private static CrimeCategory randomCategory(){
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    public enum Land{
        BADEN_WÜRTEMBERG,
        BAYERN,
        RHEINLAND_PFALZ,
        SAARLAND,
        HESSEN,
        NORD_RHEIN_WESTFALEN,
        THÜRINGEN,
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
