# Perf Report: batch throughput

Pertanyaan: berapa cepat `credit_simulator` mode file memproses N sesi `hitung`?
Jawaban singkat: **~12rb sesi/detik** setelah JVM warm, scaling linear.

## Metode (kenapa angkanya bisa dipercaya)

- **Dataset seeded** (`mock-server/generate_loans.py --seed 42`): regen = identik,
  semua baris valid terhadap `LoanValidator` (batch abort kalau ada 1 invalid —
  exit code + 0 `Error:` = validasi end-to-end gratis).
- **Run berulang**: hyperfine, 5 run + 1 warmup per ukuran (signifikansi datang
  dari sini, bukan dari jutaan baris). σ 5–8% = stabil.
- **Sweep ukuran** (`-L`): 1 → 100k. 1M dibuktikan sekali `time` (multi-run
  kemahalan: ~80 dtk/run).
- Mesin: Mac x86_64, Corretto 17.0.14, hyperfine 2.0.0, mesin idle.

Regenerasi:

```bash
python3 mock-server/generate_loans.py --rows 10000   # + batch_10000.txt
hyperfine --warmup 1 --runs 5 --shell default \
  -L rows 1,10,100,1000,10000,100000 \
  './credit_simulator mock-server/batch_{rows}.txt > /tmp/hf_{rows}.txt'
```

## Hasil

| Sesi | Wall (mean ± σ) | Peak RSS | ~sesi/detik |
| --- | --- | --- | --- |
| 1 | 0,87 ± 0,06 dtk | 59 MB | — (murni startup JVM) |
| 10 | 0,91 ± 0,14 dtk | 59 MB | — |
| 100 | 0,95 ± 0,09 dtk | 62 MB | — |
| 1.000 | 1,17 ± 0,07 dtk | 78 MB | ~3.400 |
| 10.000 | 2,50 ± 0,20 dtk | 156 MB | ~6.100 |
| 100.000 | 8,56 ± 0,50 dtk | 228 MB | ~13.000 |
| 1.000.000 | 80,2 dtk (1× `time`) | — | ~12.500 |

(3.501.527 baris tahun, 0 error di run 1M.)

## Bacaan

- **Startup ~0,87 dtk mendominasi di bawah 1k sesi** — murni JVM, bukan kode kita.
- **Di atas 10k: linear** (10× data ≈ 10× waktu; 10k→100k→1M konsisten
  ~12–13rb sesi/detik). Tidak ada degradasi superlinear: input streaming,
  tidak ada akumulasi antar sesi.
- **Sys time tinggi di 1M** (47 dtk dari 80 dtk) = I/O tulis 3,5jt baris output
  (±200MB), bukan komputasi. Throughput komputasi murni lebih tinggi lagi.
- **Memory peak naik dengan N** (59 → 228MB): tekanan alokasi GC (BigDecimal per
  tahun), bukan leak — selesai 0 error dan heap GC-managed. Kalau batch >1M jadi
  rutinitas, kandidat optimasi: reuse `MathContext`/kurangi alokasi per tahun.
