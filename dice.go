// dice.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"os"
	"strconv"
	"time"
)

type Dice struct {
	sides int
}

func NewDice(sides int) *Dice {
	return &Dice{sides: sides}
}

func (d *Dice) Roll(count int) []int {
	results := make([]int, count)
	for i := 0; i < count; i++ {
		results[i] = rand.Intn(d.sides) + 1
	}
	return results
}

func main() {
	var (
		roll      int
		count     int
		seed      int64
		statsFlag bool
		exportJson string
		exportCsv  string
	)
	flag.IntVar(&roll, "roll", 1, "Количество костей в броске")
	flag.IntVar(&count, "count", 1, "Количество бросков")
	flag.Int64Var(&seed, "seed", 0, "Seed для генератора")
	flag.BoolVar(&statsFlag, "stats", false, "Показать статистику")
	flag.StringVar(&exportJson, "export-json", "", "Экспорт в JSON")
	flag.StringVar(&exportCsv, "export-csv", "", "Экспорт в CSV")
	flag.Parse()

	if seed != 0 {
		rand.Seed(seed)
	} else {
		rand.Seed(time.Now().UnixNano())
	}

	dice := NewDice(6)
	results := make([][]int, count)
	stats := make(map[int]int)
	totalSum := 0

	fmt.Printf("\033[36mБросаем %d костей, %d раз(а)\033[0m\n", roll, count)
	for i := 0; i < count; i++ {
		vals := dice.Roll(roll)
		results[i] = vals
		sum := 0
		for _, v := range vals {
			sum += v
			stats[v]++
		}
		totalSum += sum
		fmt.Printf("\033[32mБросок %d: [%v] сумма = %d\033[0m\n", i+1, vals, sum)
	}

	if count > 1 {
		fmt.Printf("\033[33mОбщая сумма всех бросков: %d, среднее: %.2f\033[0m\n", totalSum, float64(totalSum)/float64(count))
	}

	if statsFlag {
		fmt.Println("\033[35m\nСтатистика выпадений:\033[0m")
		for side := 1; side <= 6; side++ {
			fmt.Printf("  %d: %d\n", side, stats[side])
		}
	}

	if exportJson != "" {
		data := map[string]interface{}{
			"timestamp":  time.Now().Format(time.RFC3339),
			"roll_count": roll,
			"num_rolls":  count,
			"results":    results,
			"statistics": stats,
			"total_sum":  totalSum,
			"average":    float64(totalSum) / float64(count),
		}
		jsonData, err := json.MarshalIndent(data, "", "  ")
		if err != nil {
			fmt.Fprintf(os.Stderr, "Ошибка JSON: %v\n", err)
		} else {
			err = os.WriteFile(exportJson, jsonData, 0644)
			if err == nil {
				fmt.Printf("\033[34mРезультаты экспортированы в %s\033[0m\n", exportJson)
			}
		}
	}

	if exportCsv != "" {
		file, err := os.Create(exportCsv)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Ошибка создания CSV: %v\n", err)
			return
		}
		defer file.Close()
		writer := csv.NewWriter(file)
		defer writer.Flush()
		writer.Write([]string{"roll_number", "values", "sum"})
		for i, vals := range results {
			sum := 0
			for _, v := range vals {
				sum += v
			}
			valsStr := ""
			for j, v := range vals {
				if j > 0 {
					valsStr += ","
				}
				valsStr += strconv.Itoa(v)
			}
			writer.Write([]string{strconv.Itoa(i + 1), valsStr, strconv.Itoa(sum)})
		}
		fmt.Printf("\033[34mРезультаты экспортированы в %s\033[0m\n", exportCsv)
	}
}
