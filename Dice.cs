// Dice.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

class Dice
{
    private readonly int sides;
    private readonly Random random;

    public Dice(int sides, int? seed = null)
    {
        this.sides = sides;
        this.random = seed.HasValue ? new Random(seed.Value) : new Random();
    }

    public int[] Roll(int count)
    {
        var results = new int[count];
        for (int i = 0; i < count; i++)
            results[i] = random.Next(1, sides + 1);
        return results;
    }

    static void Main(string[] args)
    {
        var opts = ParseArgs(args);
        int roll = opts.GetValueOrDefault("roll", 1);
        int count = opts.GetValueOrDefault("count", 1);
        int? seed = opts.ContainsKey("seed") ? int.Parse(opts["seed"]) : (int?)null;
        bool stats = opts.ContainsKey("stats");
        string exportJson = opts.GetValueOrDefault("export-json");
        string exportCsv = opts.GetValueOrDefault("export-csv");

        var dice = new Dice(6, seed);
        var results = new List<int[]>();
        var statsDict = new Dictionary<int, int>();
        int totalSum = 0;

        Console.WriteLine($"\u001B[36mБросаем {roll} костей, {count} раз(а)\u001B[0m");
        for (int i = 0; i < count; i++)
        {
            var vals = dice.Roll(roll);
            results.Add(vals);
            int sum = vals.Sum();
            totalSum += sum;
            foreach (var v in vals)
            {
                if (!statsDict.ContainsKey(v)) statsDict[v] = 0;
                statsDict[v]++;
            }
            Console.WriteLine($"\u001B[32mБросок {i+1}: [{string.Join(", ", vals)}] сумма = {sum}\u001B[0m");
        }

        if (count > 1)
        {
            double avg = (double)totalSum / count;
            Console.WriteLine($"\u001B[33mОбщая сумма всех бросков: {totalSum}, среднее: {avg:F2}\u001B[0m");
        }

        if (stats)
        {
            Console.WriteLine("\u001B[35m\nСтатистика выпадений:\u001B[0m");
            for (int side = 1; side <= 6; side++)
                Console.WriteLine($"  {side}: {statsDict.GetValueOrDefault(side, 0)}");
        }

        if (exportJson != null)
        {
            var data = new
            {
                timestamp = DateTime.UtcNow.ToString("o"),
                roll_count = roll,
                num_rolls = count,
                results = results,
                statistics = statsDict,
                total_sum = totalSum,
                average = (double)totalSum / count
            };
            string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(exportJson, json);
            Console.WriteLine($"\u001B[34mРезультаты экспортированы в {exportJson}\u001B[0m");
        }

        if (exportCsv != null)
        {
            using var sw = new StreamWriter(exportCsv);
            sw.WriteLine("roll_number,values,sum");
            for (int i = 0; i < results.Count; i++)
            {
                var vals = results[i];
                int sum = vals.Sum();
                sw.WriteLine($"{i+1},\"{string.Join(",", vals)}\",{sum}");
            }
            Console.WriteLine($"\u001B[34mРезультаты экспортированы в {exportCsv}\u001B[0m");
        }
    }

    static Dictionary<string, string> ParseArgs(string[] args)
    {
        var dict = new Dictionary<string, string>();
        for (int i = 0; i < args.Length; i++)
        {
            if (args[i].StartsWith("--"))
            {
                string key = args[i].Substring(2);
                if (i + 1 < args.Length && !args[i+1].StartsWith("--"))
                    dict[key] = args[++i];
                else
                    dict[key] = "";
            }
        }
        return dict;
    }
}
