// Dice.java
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Dice {
    private final int sides;
    private final Random random;

    public Dice(int sides, Long seed) {
        this.sides = sides;
        this.random = (seed != null) ? new Random(seed) : new Random();
    }

    public int[] roll(int count) {
        int[] results = new int[count];
        for (int i = 0; i < count; i++) {
            results[i] = random.nextInt(sides) + 1;
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        // Простой парсинг аргументов (без библиотек)
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i+1].startsWith("--")) {
                    params.put(key, args[++i]);
                } else {
                    params.put(key, "");
                }
            }
        }

        int roll = Integer.parseInt(params.getOrDefault("roll", "1"));
        int count = Integer.parseInt(params.getOrDefault("count", "1"));
        Long seed = params.containsKey("seed") ? Long.parseLong(params.get("seed")) : null;
        boolean stats = params.containsKey("stats");
        String exportJson = params.get("export-json");
        String exportCsv = params.get("export-csv");

        Dice dice = new Dice(6, seed);
        int[][] results = new int[count][];
        Map<Integer, Integer> statsMap = new HashMap<>();
        int totalSum = 0;

        System.out.println("\u001B[36mБросаем " + roll + " костей, " + count + " раз(а)\u001B[0m");
        for (int i = 0; i < count; i++) {
            int[] vals = dice.roll(roll);
            results[i] = vals;
            int sum = 0;
            for (int v : vals) {
                sum += v;
                statsMap.put(v, statsMap.getOrDefault(v, 0) + 1);
            }
            totalSum += sum;
            System.out.printf("\u001B[32mБросок %d: [%s] сумма = %d\u001B[0m%n", i+1, Arrays.toString(vals), sum);
        }

        if (count > 1) {
            double avg = (double) totalSum / count;
            System.out.printf("\u001B[33mОбщая сумма всех бросков: %d, среднее: %.2f\u001B[0m%n", totalSum, avg);
        }

        if (stats) {
            System.out.println("\u001B[35m\nСтатистика выпадений:\u001B[0m");
            for (int side = 1; side <= 6; side++) {
                System.out.printf("  %d: %d%n", side, statsMap.getOrDefault(side, 0));
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (exportJson != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("timestamp", Instant.now().toString());
            data.put("roll_count", roll);
            data.put("num_rolls", count);
            data.put("results", results);
            data.put("statistics", statsMap);
            data.put("total_sum", totalSum);
            data.put("average", (double) totalSum / count);
            String json = gson.toJson(data);
            Files.write(Paths.get(exportJson), json.getBytes());
            System.out.println("\u001B[34mРезультаты экспортированы в " + exportJson + "\u001B[0m");
        }

        if (exportCsv != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(exportCsv))) {
                pw.println("roll_number,values,sum");
                for (int i = 0; i < count; i++) {
                    int[] vals = results[i];
                    int sum = 0;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < vals.length; j++) {
                        if (j > 0) sb.append(",");
                        sb.append(vals[j]);
                        sum += vals[j];
                    }
                    pw.printf("%d,\"%s\",%d%n", i+1, sb.toString(), sum);
                }
            }
            System.out.println("\u001B[34mРезультаты экспортированы в " + exportCsv + "\u001B[0m");
        }
    }
}
