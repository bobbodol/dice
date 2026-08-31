

```python
#!/usr/bin/env python3
# dice.py
import argparse
import json
import csv
import random
import sys
from collections import defaultdict
from datetime import datetime
try:
    from colorama import init, Fore, Style
    init(autoreset=True)
    COLOR = True
except ImportError:
    COLOR = False
    # заглушки
    class Fore:
        GREEN = CYAN = YELLOW = RED = BLUE = MAGENTA = WHITE = RESET = ''
    Style = Fore

class Dice:
    def __init__(self, sides=6):
        self.sides = sides

    def roll(self, count=1):
        return [random.randint(1, self.sides) for _ in range(count)]

def main():
    parser = argparse.ArgumentParser(description="Игральные кости D6")
    parser.add_argument("--roll", type=int, default=1, help="Количество костей в броске")
    parser.add_argument("--count", type=int, default=1, help="Количество бросков")
    parser.add_argument("--seed", type=int, help="Seed для генератора")
    parser.add_argument("--stats", action="store_true", help="Показать статистику")
    parser.add_argument("--export-json", help="Экспорт в JSON")
    parser.add_argument("--export-csv", help="Экспорт в CSV")
    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    dice = Dice()
    all_results = []
    stats = defaultdict(int)
    total_sum = 0

    print(Fore.CYAN + f"Бросаем {args.roll} костей, {args.count} раз(а)" + Style.RESET_ALL)
    for i in range(args.count):
        values = dice.roll(args.roll)
        s = sum(values)
        all_results.append(values)
        total_sum += s
        for v in values:
            stats[v] += 1
        # вывод с цветом
        val_str = ', '.join(str(v) for v in values)
        print(Fore.GREEN + f"Бросок {i+1}: [{val_str}] сумма = {s}" + Style.RESET_ALL)

    if args.count > 1:
        print(Fore.YELLOW + f"Общая сумма всех бросков: {total_sum}, среднее: {total_sum/args.count:.2f}" + Style.RESET_ALL)

    if args.stats:
        print(Fore.MAGENTA + "\nСтатистика выпадений:" + Style.RESET_ALL)
        for side in range(1, 7):
            print(f"  {side}: {stats.get(side, 0)}")

    if args.export_json:
        data = {
            "timestamp": datetime.now().isoformat(),
            "roll_count": args.roll,
            "num_rolls": args.count,
            "results": all_results,
            "statistics": dict(stats),
            "total_sum": total_sum,
            "average": total_sum / args.count if args.count > 0 else 0
        }
        with open(args.export_json, 'w') as f:
            json.dump(data, f, indent=2)
        print(Fore.BLUE + f"Результаты экспортированы в {args.export_json}" + Style.RESET_ALL)

    if args.export_csv:
        with open(args.export_csv, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['roll_number', 'values', 'sum'])
            for i, vals in enumerate(all_results, 1):
                writer.writerow([i, ','.join(map(str, vals)), sum(vals)])
        print(Fore.BLUE + f"Результаты экспортированы в {args.export_csv}" + Style.RESET_ALL)

if __name__ == "__main__":
    main()
