// dice.rs
use clap::{App, Arg};
use colored::*;
use rand::prelude::*;
use rand::SeedableRng;
use rand::rngs::StdRng;
use serde::{Deserialize, Serialize};
use std::fs;
use std::collections::HashMap;
use chrono::Utc;

#[derive(Serialize, Deserialize)]
struct DiceResult {
    timestamp: String,
    roll_count: usize,
    num_rolls: usize,
    results: Vec<Vec<usize>>,
    statistics: HashMap<usize, usize>,
    total_sum: usize,
    average: f64,
}

struct Dice {
    sides: usize,
    rng: StdRng,
}

impl Dice {
    fn new(sides: usize, seed: Option<u64>) -> Self {
        let rng = if let Some(s) = seed {
            StdRng::seed_from_u64(s)
        } else {
            StdRng::from_entropy()
        };
        Dice { sides, rng }
    }

    fn roll(&mut self, count: usize) -> Vec<usize> {
        let mut results = Vec::with_capacity(count);
        for _ in 0..count {
            results.push(self.rng.gen_range(1..=self.sides));
        }
        results
    }
}

fn main() {
    let matches = App::new("Dice D6")
        .arg(Arg::with_name("roll").long("roll").takes_value(true).default_value("1"))
        .arg(Arg::with_name("count").long("count").takes_value(true).default_value("1"))
        .arg(Arg::with_name("seed").long("seed").takes_value(true))
        .arg(Arg::with_name("stats").long("stats"))
        .arg(Arg::with_name("export-json").long("export-json").takes_value(true))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true))
        .get_matches();

    let roll_count: usize = matches.value_of("roll").unwrap().parse().unwrap();
    let num_rolls: usize = matches.value_of("count").unwrap().parse().unwrap();
    let seed: Option<u64> = matches.value_of("seed").map(|s| s.parse().unwrap());
    let show_stats = matches.is_present("stats");
    let export_json = matches.value_of("export-json");
    let export_csv = matches.value_of("export-csv");

    let mut dice = Dice::new(6, seed);
    let mut results = Vec::with_capacity(num_rolls);
    let mut stats: HashMap<usize, usize> = HashMap::new();
    let mut total_sum = 0;

    println!("{}", format!("Бросаем {} костей, {} раз(а)", roll_count, num_rolls).cyan());
    for i in 0..num_rolls {
        let vals = dice.roll(roll_count);
        let sum: usize = vals.iter().sum();
        results.push(vals.clone());
        total_sum += sum;
        for &v in &vals {
            *stats.entry(v).or_insert(0) += 1;
        }
        println!("{}", format!("Бросок {}: [{:?}] сумма = {}", i+1, vals, sum).green());
    }

    if num_rolls > 1 {
        let avg = total_sum as f64 / num_rolls as f64;
        println!("{}", format!("Общая сумма всех бросков: {}, среднее: {:.2}", total_sum, avg).yellow());
    }

    if show_stats {
        println!("{}", "\nСтатистика выпадений:".magenta());
        for side in 1..=6 {
            println!("  {}: {}", side, stats.get(&side).unwrap_or(&0));
        }
    }

    if let Some(file) = export_json {
        let data = DiceResult {
            timestamp: Utc::now().to_rfc3339(),
            roll_count,
            num_rolls,
            results,
            statistics: stats,
            total_sum,
            average: total_sum as f64 / num_rolls as f64,
        };
        let json = serde_json::to_string_pretty(&data).unwrap();
        fs::write(file, json).unwrap();
        println!("{}", format!("Результаты экспортированы в {}", file).blue());
    }

    if let Some(file) = export_csv {
        let mut wtr = csv::Writer::from_path(file).unwrap();
        wtr.write_record(&["roll_number", "values", "sum"]).unwrap();
        for (i, vals) in results.iter().enumerate() {
            let sum: usize = vals.iter().sum();
            let vals_str = vals.iter().map(|v| v.to_string()).collect::<Vec<_>>().join(",");
            wtr.write_record(&[(i+1).to_string(), vals_str, sum.to_string()]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("Результаты экспортированы в {}", file).blue());
    }
}
