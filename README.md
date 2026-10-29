# RT/RW Billing

Backend Spring Boot untuk ISP kecil (RT/RW net) berbasis MikroTik:
manajemen pelanggan, billing bulanan, isolir otomatis, tiket gangguan,
monitoring router, dan notifikasi WhatsApp.

## Fitur

| Area | Yang dikerjakan |
|---|---|
| Pelanggan | Daftar pelanggan → user PPPoE otomatis dibuat di MikroTik; ganti paket; berhenti langganan |
| Billing | Tagihan terbit otomatis tiap tanggal 1; jatuh tempo per pelanggan; pengingat H-3 via WA |
| Isolir | Tiap malam, pelanggan yang menunggak lewat masa tenggang dipindah ke PPP profile `isolir` |
| Pembayaran | Bayar tagihan → pelanggan otomatis aktif kembali dengan paketnya |
| Tiket | Buka tiket, tugaskan teknisi, catatan pengerjaan, selesai/tutup; gangguan "internet mati" otomatis prioritas HIGH |
| Monitoring | Cek router tiap menit (UP/DOWN, CPU, uptime, jumlah sesi PPPoE); alert WA ke admin saat status berubah |
| Dashboard | Ringkasan pelanggan aktif/terisolir, tagihan lunas/belum, tiket terbuka, router down |

## Teknologi

Java 21, Spring Boot 3.3 (Web, Data JPA, Validation, Scheduling), H2 untuk development,
PostgreSQL untuk produksi, MikroTik RouterOS v7 REST API, Fonnte untuk WhatsApp.

## Menjalankan

```bash
mvn test              # 9 test alur utama (H2 + router tiruan)
mvn spring-boot:run   # http://localhost:8080
```

Default-nya mode **FAKE**: router MikroTik ditiru di memori dan pesan WA hanya dicetak ke log,
jadi bisa dicoba tanpa perangkat apa pun. Data demo (3 paket, 1 router, 3 pelanggan) dibuat otomatis.

Coba endpoint lewat file `api.http` (VS Code REST Client / IntelliJ HTTP Client).
Database H2 bisa dilihat di http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/rtrw-billing`).

## Menghubungkan ke MikroTik asli

Syarat: RouterOS **v7.1 ke atas** (REST API belum ada di v6).

Di router (lewat terminal Winbox):

```
/ip service enable www
/user group add name=billing policy=read,write,api,rest-api,test
/user add name=billing group=billing password=GANTI_PASSWORD
/ppp profile add name=paket-10m rate-limit=10M/10M
/ppp profile add name=paket-20m rate-limit=20M/20M
/ppp profile add name=isolir rate-limit=256k/256k
```

Nama profile harus sama dengan kolom `mikrotikProfile` pada paket di aplikasi.
Profile `isolir` biasanya juga diberi address-list + aturan firewall untuk mengarahkan
pelanggan ke halaman "silakan bayar tagihan".

Lalu jalankan dengan profile `prod`, atau ubah `billing.mikrotik.mode: REST` di `application.yml`.

> Keamanan: `www` (HTTP) mengirim password tanpa enkripsi. Batasi aksesnya hanya dari IP server
> (`/ip service set www address=IP_SERVER/32`) atau pakai `www-ssl` dengan sertifikat yang valid.

## Produksi

```bash
export DB_URL=jdbc:postgresql://localhost:5432/rtrw_billing
export DB_USERNAME=rtrw DB_PASSWORD=...
export FONNTE_TOKEN=... ADMIN_PHONE=6281xxxxxxxx
mvn package && java -jar target/rtrw-billing-0.1.0.jar --spring.profiles.active=prod
```

## Endpoint utama

| Method | Path | Fungsi |
|---|---|---|
| GET/POST | `/api/plans` | Daftar / tambah paket |
| GET/POST | `/api/routers` | Daftar / tambah router |
| POST | `/api/routers/{id}/check` | Cek kesehatan router sekarang |
| GET/POST | `/api/customers` | Cari (`?q=&status=`) / daftarkan pelanggan |
| PUT | `/api/customers/{id}/plan` | Ganti paket |
| POST | `/api/customers/{id}/isolate`, `/reactivate`, `/terminate` | Ubah status manual |
| GET | `/api/customers/{id}/invoices`, `/tickets` | Riwayat tagihan dan tiket |
| GET | `/api/invoices?period=2026-11` | Tagihan per periode |
| POST | `/api/invoices/generate?period=2026-11` | Terbitkan tagihan manual |
| POST | `/api/invoices/{id}/pay` | Catat pembayaran |
| POST | `/api/invoices/isolate-overdue` | Jalankan isolir otomatis sekarang |
| GET/POST | `/api/tickets` | Antrian tiket terbuka / buka tiket |
| POST | `/api/tickets/{id}/assign`, `/notes`, `/resolve`, `/close` | Alur tiket |
| GET | `/api/dashboard` | Ringkasan |

## Catatan desain

- **Sinkron dengan router.** Perubahan status pelanggan memanggil MikroTik di dalam transaksi database.
  Jika router menolak atau tidak bisa dihubungi, transaksi di-rollback supaya data tidak beda dengan router.
- **Pembayaran tidak boleh hilang.** Pembayaran di-commit dulu, baru pelanggan diaktifkan kembali.
  Kalau router sedang down, uang tetap tercatat dan pelanggan bisa diaktifkan manual nanti.
- **Isolir per pelanggan.** Isolir massal memproses tiap pelanggan di transaksinya sendiri,
  jadi satu kegagalan tidak membatalkan yang lain.
- **Idempotent.** Generate tagihan, isolir, dan aktivasi aman dijalankan berulang.
- **Gateway bisa ditukar.** `MikrotikGateway` dan `WhatsAppSender` berupa interface;
  implementasi dipilih lewat konfigurasi (FAKE/REST, LOG/FONNTE).

## Pengembangan berikutnya

- Autentikasi admin dan teknisi (Spring Security + JWT)
- Integrasi FreeRADIUS (tulis ke `radcheck`/`radreply`) sebagai alternatif PPP secret lokal
- Voucher hotspot
- Payment gateway (Midtrans/Xendit) dengan callback otomatis
- Flyway migration menggantikan `ddl-auto`
- Frontend admin panel
- PPPoE password saat ini disimpan apa adanya karena dibutuhkan router; enkripsi di level kolom bisa ditambahkan
