// Dice.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import java.io.File
import java.time.Instant
import kotlin.random.Random

class Dice(private val sides: Int, seed: Long? = null) {
    private val random = if (seed != null) Random(seed) else Random(System.currentTimeMillis())

    fun roll(count: Int): List<Int> {
        return (1..count).map { random.nextInt(1, sides + 1) }
    }
}

class Options {
    @Parameter(names = ["--roll"], description = "Количество костей в броске")
    var roll: Int = 1

    @Parameter(names = ["--count"], description = "Количество бросков")
    var count: Int = 1

    @Parameter(names = ["--seed"], description = "Seed для генератора")
    var seed: Long? = null

    @Parameter(names = ["--stats"], description = "Показать статистику")
    var stats: Boolean = false

    @Parameter(names = ["--export-json"], description = "Экспорт в JSON")
    var exportJson: String? = null

    @Parameter(names = ["--export-csv"], description = "Экспорт в CSV")
    var exportCsv: String? = null
}

fun main(args: Array<String>) {
    val options = Options()
    JCommander.newBuilder().addObject(options).build().parse(*args)

    val dice = Dice(6, options.seed)
    val results = mutableListOf<List<Int>>()
    val stats = mutableMapOf<Int, Int>()
    var totalSum = 0

    println("\u001B[36mБросаем ${options.roll} костей, ${options.count} раз(а)\u001B[0m")
    for (i in 0 until options.count) {
        val vals = dice.roll(options.roll)
        results.add(vals)
        val sum = vals.sum()
        totalSum += sum
        for (v in vals) {
            stats[v] = stats.getOrDefault(v, 0) + 1
        }
        println("\u001B[32mБросок ${i+1}: [${vals.joinToString(", ")}] сумма = $sum\u001B[0m")
    }

    if (options.count > 1) {
        val avg = totalSum.toDouble() / options.count
        println("\u001B[33mОбщая сумма всех бросков: $totalSum, среднее: ${"%.2f".format(avg)}\u001B[0m")
    }

    if (options.stats) {
        println("\u001B[35m\nСтатистика выпадений:\u001B[0m")
        for (side in 1..6) {
            println("  $side: ${stats[side] ?: 0}")
        }
    }

    options.exportJson?.let { file ->
        val data = mapOf(
            "timestamp" to Instant.now().toString(),
            "roll_count" to options.roll,
            "num_rolls" to options.count,
            "results" to results,
            "statistics" to stats,
            "total_sum" to totalSum,
            "average" to totalSum.toDouble() / options.count
        )
        val gson = GsonBuilder().setPrettyPrinting().create()
        File(file).writeText(gson.toJson(data))
        println("\u001B[34mРезультаты экспортированы в $file\u001B[0m")
    }

    options.exportCsv?.let { file ->
        File(file).printWriter().use { pw ->
            pw.println("roll_number,values,sum")
            results.forEachIndexed { idx, vals ->
                val sum = vals.sum()
                pw.println("${idx+1},\"${vals.joinToString(",")}\",$sum")
            }
        }
        println("\u001B[34mРезультаты экспортированы в $file\u001B[0m")
    }
}
