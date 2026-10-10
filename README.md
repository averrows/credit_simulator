# credit_simulator

Aplikasi console untuk menghitung cicilan bulanan kredit kendaraan (Mobil/Motor,
Baru/Bekas), sesuai soal *Backend Test*. Java 17 + Maven, **nol dependensi
production** (HTTP pakai JDK, JSON pakai parser internal) — test-only: JUnit 5,
Mockito, AssertJ.

## Syarat

- Java 17 (Corretto/LTS) dan Maven 3.9.x
- Docker (opsional, untuk image)

## Build

```bash
mvn verify              # compile + 65 test + gate coverage 85%
mvn -q -DskipTests package   # cepat, tanpa test
```

Hasil: `target/credit-simulator-*.jar` (tidak di-commit, lihat `.gitignore`).

## Cara menjalankan

```bash
./credit_simulator                  # interaktif (prompt)
./credit_simulator file_inputs.txt  # batch (baris file = ketikan user)
bin/credit_simulator                # sama, lewat folder bin/
./credit_simulator --help           # bantuan
./credit_simulator --url URL        # ganti endpoint load (lihat "Load")
```

Contoh sesi interaktif:

```
> show
  hitung  - simulasi baru (tanya 6 input berurutan)
  load    - ambil perhitungan tersimpan + tampilkan hasil
  show    - tampilkan perintah ini
  exit    - keluar
> hitung
  Jenis Kendaraan [Mobil|Motor]: mobil
  Kondisi [Baru|Bekas]: Baru
  Tahun Kendaraan [YYYY]: 2025
  Jumlah Pinjaman Total: 100000000
  Tenor Pinjaman (1-6 thn): 3
  Jumlah DP: 35000000
  tahun 1 : Rp. 1,950,000.00/bln , Suku Bunga : 8%
  tahun 2 : Rp. 2,107,950.00/bln , Suku Bunga : 8,1%
  tahun 3 : Rp. 2,289,233.70/bln , Suku Bunga : 8,6%
> exit
```

Input case-insensitive dan prefix unik otomatis dipilih (`mot` → Motor,
`bek` → Bekas, disertai pesan `(otomatis: …)`). Prefix ambigu (`mo`, `b`)
ditolak dengan pesan yang nyebutin pilihannya. Input salah di mode interaktif
mengulang field itu saja; di mode file langsung berhenti dengan exit code
non-zero.

`file_inputs.txt` (contoh batch, timeless — pakai Bekas 2015) juga bisa
dicoba langsung: `./credit_simulator file_inputs.txt`.

## Aturan validasi (Rules of the Game 1–4)

- Baru: tahun ≥ tahun-berjalan − 1; tahun selalu 4 digit angka.
- Tenor 1–6 tahun. Total pinjaman 1–1.000.000.000.
- DP Baru ≥ 35%, DP Bekas ≥ 25%, dan DP harus kurang dari total pinjaman.

## Bunga & rumus (Rules 7–9 + Rumus.xlsx)

- Pokok: Mobil 8%, Motor 9%. Tiap tahun naik selang-seling: tahun genap +0,1%,
  tahun ganjil (≥3) +0,5% → Mobil: 8 → 8,1 → 8,6 → 8,7 → 9,2 → 9,3.
- Metode *sequential flat*: cicilan-tahun = sisa-pokok × (1+rate) / sisa-bulan;
  yang sudah dibayar dikurangi dulu sebelum tahun berikut dihitung. Semua
  angka `BigDecimal` (pembulatan `HALF_UP` 2 desimal).

## Load (perhitungan tersimpan)

`load` GET JSON `{vehicleType, vehicleCondition, vehicleYear, totalLoanAmount,
loanTenure, downPayment}` dari endpoint soal, lalu otomatis menghitung.
**Endpoint bawaan di soal sudah mati (404)**, jadi perilaku aplikasinya:

- Sukses (HTTP 200 + JSON valid + lolos validasi) → hitung data remote.
- Gagal apa pun → warning `(peringatan: …)` + hitung **contoh bawaan PDF**
  (Mobil Baru 2025, 1M, tenor 6, DP 500jt). Tidak pernah crash diam-diam.

Mencoba path remote pakai mock sendiri (payload di `mock-server/loan.json`):

```bash
python3 -m http.server 8080 --directory mock-server
./credit_simulator --url http://127.0.0.1:8080/loan.json
> load     # tanpa warning = data beneran dari mock lokal
```

URL permanen lain (Beeceptor/dll.): ganti satu baris `DEFAULT_URL` di
`RemoteLoader`.

## Testing & coverage

```bash
mvn test     # 65 test: mvn test saja
mvn verify   # + gate JaCoCo minimal 85% line coverage (sekarang ~90%)
open target/site/jacoco/index.html   # laporan coverage
```

## Struktur (MVC + Factory)

```
src/main/java/com/bcadigital/creditsimulator/
  App.java                    # argumen (file/--url), wiring sekali jalan
  model/      MenuCommand, VehicleType, VehicleCondition, Match,
              LoanRequest, YearlyResult, SimulationResult  (data, no logic)
  view/       ConsoleView, InputSource (+Stream/File)      (I/O saja)
  controller/ SimulatorController                          (deep module: loop,
              retry per-field, dispatch)
  service/    LoanValidator (+4 domain exception), InstallmentCalculator,
              RatePolicy + SteppedRatePolicy + VehicleRateFactory,
              RemoteLoader + MiniJson + RemoteLoad
```

## Docker & CI

```bash
docker build -t credit-simulator .
docker run --rm -i credit-simulator                     # interaktif
docker run --rm credit-simulator file_inputs.txt 2>/dev/null || \
  docker run --rm -i credit-simulator < file_inputs.txt # batch via stdin
```

Workflow `.github/workflows/ci.yml`: `mvn verify` + `docker build` tiap push/PR.

## Asumsi & batasan

- Endpoint mocky bawaan mati → fallback contoh PDF (termasuk aging wajar:
  contoh Baru-2025 lolos validasi selama tahun-berjalan ≤ 2026).
- DP wajib `<` total (DP = total bikin pokok 0 — nonsense loan).
- Bekas: tahun bebas asal 4 digit (soal tidak membatasi).
- Tanpa edit-distance typo (`mobli` tetap invalid) — prefix unik saja, deterministik.
