// dice.cpp
#include <iostream>
#include <vector>
#include <string>
#include <map>
#include <random>
#include <ctime>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <chrono>
#include <cstring>
#include <json/json.h> // using jsoncpp

using namespace std;

class Dice {
private:
    int sides;
    mt19937 rng;
    uniform_int_distribution<int> dist;

public:
    Dice(int sides, unsigned seed = 0) : sides(sides) {
        if (seed != 0) rng.seed(seed);
        else rng.seed(chrono::steady_clock::now().time_since_epoch().count());
        dist = uniform_int_distribution<int>(1, sides);
    }

    vector<int> roll(int count) {
        vector<int> results;
        results.reserve(count);
        for (int i = 0; i < count; ++i) {
            results.push_back(dist(rng));
        }
        return results;
    }
};

int main(int argc, char* argv[]) {
    int roll = 1, count = 1;
    unsigned seed = 0;
    bool statsFlag = false;
    string exportJson, exportCsv;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--roll" && i+1 < argc) roll = stoi(argv[++i]);
        else if (arg == "--count" && i+1 < argc) count = stoi(argv[++i]);
        else if (arg == "--seed" && i+1 < argc) seed = stoul(argv[++i]);
        else if (arg == "--stats") statsFlag = true;
        else if (arg == "--export-json" && i+1 < argc) exportJson = argv[++i];
        else if (arg == "--export-csv" && i+1 < argc) exportCsv = argv[++i];
    }

    Dice dice(6, seed);
    vector<vector<int>> allResults;
    map<int, int> stats;
    int totalSum = 0;

    cout << "\033[36mБросаем " << roll << " костей, " << count << " раз(а)\033[0m" << endl;
    for (int i = 0; i < count; ++i) {
        auto vals = dice.roll(roll);
        allResults.push_back(vals);
        int sum = 0;
        for (int v : vals) {
            sum += v;
            stats[v]++;
        }
        totalSum += sum;
        cout << "\033[32mБросок " << i+1 << ": [";
        for (size_t j = 0; j < vals.size(); ++j) {
            if (j > 0) cout << ", ";
            cout << vals[j];
        }
        cout << "] сумма = " << sum << "\033[0m" << endl;
    }

    if (count > 1) {
        double avg = (double)totalSum / count;
        cout << "\033[33mОбщая сумма всех бросков: " << totalSum << ", среднее: " << fixed << setprecision(2) << avg << "\033[0m" << endl;
    }

    if (statsFlag) {
        cout << "\033[35m\nСтатистика выпадений:\033[0m" << endl;
        for (int side = 1; side <= 6; ++side) {
            cout << "  " << side << ": " << stats[side] << endl;
        }
    }

    if (!exportJson.empty()) {
        Json::Value root;
        root["timestamp"] = to_string(chrono::system_clock::now().time_since_epoch().count());
        root["roll_count"] = roll;
        root["num_rolls"] = count;
        for (size_t i = 0; i < allResults.size(); ++i) {
            Json::Value row(Json::arrayValue);
            for (int v : allResults[i]) row.append(v);
            root["results"].append(row);
        }
        Json::Value statsJson;
        for (auto& kv : stats) statsJson[to_string(kv.first)] = kv.second;
        root["statistics"] = statsJson;
        root["total_sum"] = totalSum;
        root["average"] = (double)totalSum / count;
        ofstream ofs(exportJson);
        ofs << root.toStyledString();
        cout << "\033[34mРезультаты экспортированы в " << exportJson << "\033[0m" << endl;
    }

    if (!exportCsv.empty()) {
        ofstream ofs(exportCsv);
        ofs << "roll_number,values,sum\n";
        for (size_t i = 0; i < allResults.size(); ++i) {
            int sum = 0;
            for (int v : allResults[i]) sum += v;
            ofs << i+1 << ",\"";
            for (size_t j = 0; j < allResults[i].size(); ++j) {
                if (j > 0) ofs << ",";
                ofs << allResults[i][j];
            }
            ofs << "\"," << sum << "\n";
        }
        cout << "\033[34mРезультаты экспортированы в " << exportCsv << "\033[0m" << endl;
    }

    return 0;
}
