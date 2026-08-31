#!/usr/bin/env node
// dice.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

class Dice {
    constructor(sides = 6) {
        this.sides = sides;
    }

    roll(count = 1) {
        const results = [];
        for (let i = 0; i < count; i++) {
            results.push(Math.floor(Math.random() * this.sides) + 1);
        }
        return results;
    }
}

program
    .option('--roll <number>', 'Количество костей в броске', parseInt, 1)
    .option('--count <number>', 'Количество бросков', parseInt, 1)
    .option('--seed <number>', 'Seed для генератора', parseInt)
    .option('--stats', 'Показать статистику')
    .option('--export-json <file>', 'Экспорт в JSON')
    .option('--export-csv <file>', 'Экспорт в CSV')
    .parse(process.argv);

const opts = program.opts();

if (opts.seed !== undefined) {
    // Простая имитация seed: используем множитель для детерминированности
    // В реальном коде лучше использовать seedrandom
    let s = opts.seed;
    const seedRandom = () => {
        s = (s * 9301 + 49297) % 233280;
        return s / 233280;
    };
    // Заменяем Math.random
    const origRandom = Math.random;
    Math.random = seedRandom;
    // Восстановим после выполнения (но для простоты оставим)
}

const dice = new Dice();
const results = [];
const stats = {};
let totalSum = 0;

console.log(chalk.cyan(`Бросаем ${opts.roll} костей, ${opts.count} раз(а)`));
for (let i = 0; i < opts.count; i++) {
    const values = dice.roll(opts.roll);
    const sum = values.reduce((a, b) => a + b, 0);
    results.push(values);
    totalSum += sum;
    for (const v of values) {
        stats[v] = (stats[v] || 0) + 1;
    }
    console.log(chalk.green(`Бросок ${i+1}: [${values.join(', ')}] сумма = ${sum}`));
}

if (opts.count > 1) {
    console.log(chalk.yellow(`Общая сумма всех бросков: ${totalSum}, среднее: ${(totalSum / opts.count).toFixed(2)}`));
}

if (opts.stats) {
    console.log(chalk.magenta('\nСтатистика выпадений:'));
    for (let side = 1; side <= 6; side++) {
        console.log(`  ${side}: ${stats[side] || 0}`);
    }
}

if (opts.exportJson) {
    const data = {
        timestamp: new Date().toISOString(),
        roll_count: opts.roll,
        num_rolls: opts.count,
        results: results,
        statistics: stats,
        total_sum: totalSum,
        average: opts.count > 0 ? totalSum / opts.count : 0
    };
    fs.writeFileSync(opts.exportJson, JSON.stringify(data, null, 2));
    console.log(chalk.blue(`Результаты экспортированы в ${opts.exportJson}`));
}

if (opts.exportCsv) {
    let csv = 'roll_number,values,sum\n';
    results.forEach((vals, idx) => {
        csv += `${idx+1},"${vals.join(',')}",${vals.reduce((a,b)=>a+b,0)}\n`;
    });
    fs.writeFileSync(opts.exportCsv, csv);
    console.log(chalk.blue(`Результаты экспортированы в ${opts.exportCsv}`));
}
