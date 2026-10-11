#!/usr/bin/env python3
"""Generate random but always-valid loan datasets for perf testing.

Outputs (in --out-dir, default mock-server/):
  loans_<N>.csv  - canonical dataset, one request per row
  batch_<N>.txt  - same data as credit_simulator file-mode sessions

Seeded RNG: same seed + same code = byte-identical files, so perf runs are
apple-to-apple comparable. Every row passes LoanValidator by construction
(DP is drawn above the condition minimum and below the total).

Usage: python3 mock-server/generate_loans.py --rows 10000 [--seed 42]
"""

import argparse
import csv
import random
from datetime import date

CSV_HEADER = ["vehicle_type", "vehicle_condition", "vehicle_year",
              "total_loan", "tenure_years", "down_payment"]


def generate_row(rng, current_year):
    vehicle_type = rng.choice(["Mobil", "Motor"])
    condition = rng.choice(["Baru", "Bekas"])
    if condition == "Baru":
        year = rng.choice([current_year - 1, current_year])
        min_pct = 35
    else:
        year = rng.randint(1990, current_year)
        min_pct = 25
    total = rng.randint(10_000, 1_000_000) * 1000
    tenor = rng.randint(1, 6)
    dp_floor = (min_pct * total + 99) // 100
    down_payment = rng.randint(dp_floor, total - 1)
    return [vehicle_type, condition, year, total, tenor, down_payment]


def main():
    parser = argparse.ArgumentParser(description="Generate loan perf datasets.")
    parser.add_argument("--rows", type=int, default=10_000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--out-dir", default="mock-server")
    args = parser.parse_args()

    rng = random.Random(args.seed)
    current_year = date.today().year
    rows = [generate_row(rng, current_year) for _ in range(args.rows)]

    csv_path = f"{args.out_dir}/loans_{args.rows}.csv"
    with open(csv_path, "w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(CSV_HEADER)
        writer.writerows(rows)

    batch_path = f"{args.out_dir}/batch_{args.rows}.txt"
    with open(batch_path, "w") as handle:
        for row in rows:
            handle.write("hitung\n")
            for value in row:
                handle.write(f"{value}\n")
        handle.write("exit\n")

    print(f"wrote {csv_path} + {batch_path} ({args.rows} rows, seed {args.seed})")


if __name__ == "__main__":
    main()
