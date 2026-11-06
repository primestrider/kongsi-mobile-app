# Kotlin Android Starter Kit — Design

Tanggal: 2026-10-30
Proyek: `D:\Developer Stuff\Frontend_Mobile\kotlin-starter` (proyek Android Studio kosong, template Views, package `com.primestrider.kotlinstarter`, belum git)
Referensi: `D:\RN\rn-expo-boilerplate` (boilerplate React Native Expo milik user)

Tanda **[call Claude]** = keputusan yang diambil Claude, belum dikonfirmasi user. Selain itu = keputusan user.

## 1. Intent

User sudah punya boilerplate React Native/Expo yang lengkap dan siap produksi. Mereka ingin padanannya untuk **Kotlin Android native** supaya setiap aplikasi Android baru bisa dimulai dari fondasi yang sama: navigasi, auth, networking, tema, i18n, komponen, demo fitur device, tes, dan pipeline rilis — tanpa setup ulang dari nol.

Prinsip utama: **paritas fitur dengan boilerplate RN**, diterjemahkan ke cara idiomatik Android (bukan meniru API JavaScript). Bila ragu tentang perilaku sebuah bagian, sumber kebenarannya adalah kode di `rn-expo-boilerplate` (file-file yang dirujuk di bawah).

## 2. Goals, non-goals, anti-goals

**Goals**
- Semua bagian boilerplate RN punya padanan yang berfungsi di Android, **termasuk semua ~21 demo SDK** (keputusan user: "perlu ikut").
- Kode contoh (`example`) terpisah jelas dari inti, sehingga mudah dihapus saat memulai aplikasi nyata **[call Claude]**.
- Build release dan distribusi Firebase App Distribution lewat GitHub Actions, seperti di RN.

**Non-goals**
- iOS, Kotlin Multiplatform, dan web (keputusan user: Android saja dulu).
- Push notification (FCM) — RN pun hanya notifikasi lokal.
- Backend sungguhan; contoh memakai DummyJSON seperti RN.

**Anti-goals**
- Demo yang "ada halamannya" tapi tidak benar-benar berfungsi di device.
- Logika contoh yang menyatu dengan inti (auth, network, tema) sehingga sulit dibuang.
- Abstraksi spekulatif (multi-module, interface dengan satu implementasi tanpa alasan) yang memperberat starter kit.

## 3. Constraints

- Developer memakai **Windows**; semua harus bisa di-build dan dites di Windows (Gradle wrapper, tanpa skrip khusus macOS).
- Proyek yang ada: AGP 8.13.2, Gradle 8.13, Kotlin 2.0.21, compileSdk/targetSdk 36, **minSdk 24**, template Views. Template Views diganti Compose. Toolchain dinaikkan ke versi stabil terbaru per 2026-10-30 (permintaan user): **AGP 9.4.0, Gradle 9.6.0, Kotlin 2.4.21, compileSdk/targetSdk 37, JDK 17+ (di mesin user: JDK 21 via JBR Android Studio; platform android-37.0 dan build-tools 37.0.0 sudah terpasang)**. AGP 9 sudah punya Kotlin bawaan, jadi plugin `org.jetbrains.kotlin.android` dan blok `kotlinOptions` dihapus (konfigurasi lewat `kotlin { compilerOptions { … } }`); plugin Compose compiler & KSP mengikuti versi Kotlin. Library lain juga memakai versi stabil terbaru saat implementasi; minSdk dipertahankan 24, fitur yang butuh API lebih tinggi wajib punya fallback yang ditampilkan jelas di layar **[call Claude]**.
- Semua dependensi lewat version catalog `gradle/libs.versions.toml`.

## 4. Stack (padanan RN → Kotlin)

| Area | RN | Kotlin Android |
|---|---|---|
| UI | React Native + @expo/ui | Jetpack Compose + Material 3 |
| Navigasi | Expo Router (typed routes, `Stack.Protected`) | Navigation Compose, route type-safe (`@Serializable`), guard untuk grup protected **[call Claude]** |
| State | Zustand | ViewModel + `StateFlow` |
| Penyimpanan key-value | MMKV | DataStore (Preferences) **[call Claude]** |
| Data fetching | TanStack Query | Repository + `Flow`; Paging 3 untuk infinite list **[call Claude]** |
| HTTP | Axios + interceptor | Retrofit + OkHttp; `Interceptor` untuk token, `Authenticator` untuk refresh 401 **[call Claude]** |
| JSON | — | kotlinx.serialization **[call Claude]** |
| Form/validasi | React Hook Form + Zod | State form di ViewModel + fungsi validator murni yang bisa dites **[call Claude]** |
| i18n | i18next (en, id) | String resources `values/` + `values-in/`, ganti bahasa per-app via `AppCompatDelegate.setApplicationLocales` **[call Claude]** |
| DB lokal | expo-sqlite | Room |
| DI | (tidak ada) | Hilt (dikonfirmasi user) |
| Gambar remote | expo-image | Coil 3 **[call Claude]** |
| Tes | Jest + Testing Library | JUnit + Turbine/coroutines-test untuk logika; Compose UI test dijalankan via Robolectric agar jalan di CI tanpa emulator **[call Claude]** |

Struktur modul: **satu modul `:app`** dengan paket per lapisan/fitur, mengikuti struktur folder RN **[call Claude]**:

```
com.primestrider.kotlinstarter/
  app/            # MainActivity, Application, NavHost root, AppProvider-equivalent
  core/
    network/      # Retrofit/OkHttp, interceptor, authenticator, ApiError
    storage/      # DataStore, keys
    database/     # Room
    i18n/
    designsystem/ # tokens, theme, ThemeMode store, komponen shared
    util/         # formatter, parser, locale helpers
  feature/
    auth/         # session store, AuthAdapter, refresh, SessionExpiry toast, sign-in screen
    account/      # layar protected
    example/      # semua showcase & demo (bisa dihapus)
```

## 5. Inti (core)

### 5.1 Auth & sesi — rujukan `src/features/auth/**`, `src/plugins/axios/**`
Pertahankan perilaku RN:
- `AuthAdapter` (signIn, refresh) sebagai satu-satunya titik yang tahu backend; implementasi contoh untuk DummyJSON (`/auth/login`, `/auth/refresh`) ada di `feature/example`. Mengganti backend = menulis adapter baru.
- Sesi (user + tokens) disimpan persisten; status login ditentukan oleh keberadaan token. Dibaca sebelum frame pertama (atau ditahan splash screen) agar tidak ada kedipan layar sign-in saat cold start.
- Request otomatis membawa `Authorization: Bearer` kecuali endpoint ditandai anonim (padanan `meta.requiresAuth === false`).
- 401 pada request terautentikasi → refresh sekali; request yang bersamaan berbagi satu refresh; retry sekali; gagal lagi → sign out dengan alasan "expired" → toast "sesi berakhir" sekali (tidak muncul lagi saat cold start).
- Semua error HTTP dinormalisasi ke satu bentuk `ApiError(message, status, data, isNetworkError)`.
- Token disimpan terenkripsi (Keystore) **[call Claude]** — RN memakai MMKV biasa, tapi di Android native ini murah dan lebih aman.

### 5.2 Design system & tema — rujukan `src/styles/**`, README bagian Styling
- Font aplikasi: Plus Jakarta Sans, sama seperti RN (`configs/font.config.ts`).
- Token warna (semantic: background, foreground, card, border, primary, …, plus palet mentah), spacing, radii, tipografi — dipetakan ke `MaterialTheme` + `CompositionLocal` tambahan untuk token yang tidak ada di M3.
- Tema: System / Light / Dark, disimpan, dibaca sebelum render pertama (tanpa kilatan tema salah). Edge-to-edge dengan ikon system bar mengikuti tema.
- Komponen (keputusan user, 2026-10-30): **tidak** membuat library pembungkus seperti `src/shared/components/**` di RN. Starter kit memakai komponen Jetpack Compose / Material 3 langsung dan menyediakan **halaman contoh** cara memakainya: Button (filled/tonal/outlined/text/elevated), IconButton, FAB, Chip, Badge, Card, ListItem, Divider, TextField, Checkbox, RadioButton, Switch, Slider, dropdown/select, SegmentedButton, Tabs, progress indicator, AlertDialog, ModalBottomSheet, Snackbar, DropdownMenu, Tooltip. Pola yang tidak ada di M3 dibuat sebagai contoh di halaman: avatar inisial, skeleton, empty state, banner alert. Supaya komponen M3 bawaan tampil sesuai token, **semua** peran warna `ColorScheme` dipetakan ke token. `ThemeModeSelector` tetap menjadi padanan ThemeToggle.
- **Revisi (permintaan user, 2026-10-30, setelah halaman contoh selesai):** tambahkan **komponen reusable** di `core/designsystem/component/`, dibangun di atas M3 (bukan menulis ulang), untuk pola yang sering dipakai: modal/dialog dan bottom sheet; navigasi & layout layar (scaffold dengan top bar + kembali + aksi, bottom navigation bar); form & tombol (text field dengan error/password toggle, select/dropdown, loading button); feedback & state (snackbar/toast global yang bisa dipanggil dari mana saja, loading/empty/error state, skeleton, avatar, banner); serta komponen umum lain yang sering dipakai (dipilih Claude). Halaman contoh diubah memakai komponen ini.
- **Shell navigasi (keputusan user):** aplikasi memakai **bottom navigation bar** sebagai struktur utama, dengan back stack terpisah per tab. Isi tab **[call Claude]**: Home (indeks contoh), Components (galeri komponen), Profile (Settings sekarang; Account dari rencana auth).
- Error boundary RN → error state per layar + handler crash global yang menampilkan layar fallback **[call Claude]**.

### 5.3 Konfigurasi
- `APP_NAME`, `API_URL`, `APP_SCHEME` (padanan `.env.example`) → `BuildConfig`/`manifestPlaceholders` dari `local.properties` atau env var, dengan `.env.example`-equivalent terdokumentasi **[call Claude]**.

## 6. Contoh (feature/example) — rujukan `src/app/(public)/example/**`, `src/features/example/**`

Layar beranda publik → indeks contoh, dikelompokkan seperti RN.

**Design system pages:** colors, typography, spacing, sizing, layout, appearance, helpers, theme.
**Komponen:** halaman contoh komponen Compose/M3: actions, inputs, display, overlays. Halaman `native-android`/`native-universal` RN (showcase @expo/ui) digabung ke galeri karena di Kotlin semuanya sudah native **[call Claude]**.
**Form:** contoh form dengan validasi (padanan `form.tsx` + `form.schema.ts`).
**Features:**
- Products: list DummyJSON dengan pencarian (debounce), infinite scroll, detail produk; ID tidak valid tidak memanggil API.
- Todos: list lokal yang persisten.
- Settings: bahasa (en/id) dan tema.
**Protected:** layar Account (profil user, sign out), hanya bisa diakses setelah sign-in.

**Demo SDK** — semuanya ikut. Padanan Android **[call Claude untuk pilihan library]**:

| RN screen | Android |
|---|---|
| app-info | `PackageManager` (versi, build), Play In-App Review API |
| background-task | WorkManager periodic (min 15 menit), daftar/ubah/batal jadwal |
| biometric | `androidx.biometric` + kunci Keystore; PIN disimpan terenkripsi, hanya dibaca setelah verifikasi biometrik |
| blob | Manipulasi `ByteArray`/Okio: membangun payload, slicing per byte untuk upload chunk |
| calendar | `CalendarContract`: tambah event + reminder 1 jam sebelum (izin kalender) |
| camera | CameraX: ambil foto + scan QR (ML Kit Barcode), satu preview dua mode; satu kode memicu satu hasil |
| clipboard | `ClipboardManager` + listener perubahan + haptic feedback |
| contacts | Pemilih kontak sistem + pencarian `ContactsContract` (debounce, izin kontak) |
| device | Info `Build`, status jaringan via `ConnectivityManager` callback (banner offline), mode pesawat |
| effects | Gradien linear (Brush), blur (`Modifier.blur`, API 31+ dengan fallback), mesh gradient (RuntimeShader API 33+ dengan fallback) |
| files | Kecilkan foto sebelum upload (hanya scale down), unduh PDF ke cache, kelola file cache |
| image-picker | Photo Picker, kamera (izin), pemilih dokumen; crop untuk satu gambar |
| live-photo | Halaman yang menjelaskan Live Photo hanya ada di iOS (sama seperti RN di platform lain) |
| location | Satu kali fix, live tracking di foreground selama switch aktif (bukan background, sama seperti RN), reverse geocoding (Fused Location Provider + `Geocoder`) |
| media | Rekam & putar voice note; video promo loop dengan picture-in-picture (Media3/ExoPlayer) |
| notifications | Notifikasi lokal: izin `POST_NOTIFICATIONS`, channel, terjadwal, tampil saat app di foreground |
| payment-code | QR untuk kasir (ZXing), brightness maksimal, layar tetap menyala, `FLAG_SECURE`, deteksi screenshot (API 34+); semua dipulihkan saat switch mati/layar ditutup |
| share | Struk → PDF, cetak via `PrintManager`, bagikan via `FileProvider` + share sheet |
| sqlite | Riwayat transaksi offline dengan Room |
| status-bar | Override ikon system bar per layar, kembali ke default saat layar ditutup |
| web-browser | Custom Tabs, buka app lain via intent (tangani bila tidak ada handler), deep link kembali ke app |

**Verifikasi (keputusan user):** demo dicek di emulator dan di device fisik. Demo yang butuh hardware (kamera/QR, biometrik, PiP, kontak, kalender) belum dianggap selesai sebelum dicek di device fisik. Fallback API level dicek di emulator dengan API lama.

Setiap demo yang butuh izin memakai satu komponen `PermissionGate` bersama (padanan RN).

## 7. Build, versi, rilis — rujukan README, `configs/signing.config.ts`, `.github/workflows/deploy-android.yml`, `docs/firebase-app-distribution-setup.md`

- **Versi satu sumber**: `version=1.0.0` di `gradle.properties` **[call Claude]**; `versionCode = major*10000 + minor*100 + patch`.
- **Signing release** dari env var `ANDROID_KEYSTORE_PATH/PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`; bila tidak ada, fallback ke debug key agar build release lokal tetap jalan. Ditulis langsung di `app/build.gradle.kts` (tidak perlu trik prebuild seperti RN).
- **APK universal** release; R8/minify aktif **[call Claude]**.
- **GitHub Actions** `deploy-android.yml`: trigger merge ke `main` + manual; tentukan bump (feat → minor, lainnya → patch, manual pilih; hanya bump dari main); jalankan tes; bump versi + commit `[skip ci]` sebelum build; build APK bertanda tangan; verifikasi signature dengan `apksigner`; upload ke Firebase App Distribution. Secrets/vars mengikuti RN sejauh relevan.
- Git diinisialisasi lokal (permintaan user). Repo GitHub, environment `app-distribution`, secrets, dan proyek Firebase belum ada: pipeline ditulis lengkap dan diverifikasi sejauh bisa secara lokal (build release bertanda tangan, `apksigner verify`); menghubungkan ke GitHub/Firebase dilakukan user mengikuti dokumen setup **[call Claude]**.
- Dokumen setup Firebase diporting ke `docs/` dan disesuaikan.
- Task Gradle untuk lint (Android Lint; ktlint/detekt **[call Claude: ktlint saja]**) dan tes.

## 8. Tes

Padanan cakupan RN (`tests/**`): logika auth (store sesi, refresh, interceptor/authenticator, adapter), validator form, formatter/parser/locale, store todos/settings, signing/versi (unit test fungsi versionCode), kelengkapan terjemahan (setiap key ada di en dan id), dan Compose UI test untuk komponen shared serta guard navigasi. Demo SDK yang bergantung hardware cukup dites logika ViewModel-nya, bukan hardware-nya **[call Claude]**.

## 9. Dokumentasi

`README.md` berbahasa Inggris seperti RN **[call Claude]**: tech stack, requirements (Android Studio, JDK), menjalankan app, styling & dark mode, struktur folder, cara menghapus `feature/example`, versi, release build, Firebase App Distribution.

### 9.1 Penjelasan kode untuk maintainer & agent AI (permintaan user)

User ingin markdown yang menjelaskan kode beserta "skill"-nya, supaya developer lain maupun agent AI lain mudah memahami dan memelihara proyek.

- **`AGENTS.md`** di root: titik masuk untuk agent AI (dan manusia). Berisi peta proyek, aturan arsitektur (lapisan mana memiliki apa, arah dependensi, `feature/example` tidak boleh diimpor oleh `core`), konvensi penamaan, perintah build/test/lint, dan daftar skill yang tersedia. `CLAUDE.md` cukup merujuk ke `AGENTS.md` agar tidak ada dua sumber **[call Claude]**.
- **`docs/architecture/*.md`**: satu dokumen per area inti, menjelaskan *apa*, *kenapa*, dan *alur*-nya, dengan rujukan ke file kode: `overview.md` (lapisan & alur data), `navigation.md` (route, guard protected), `auth-session.md` (sesi, interceptor, refresh 401, sign-out & toast), `networking.md` (Retrofit/OkHttp, ApiError), `design-system.md` (token, tema, komponen), `i18n.md`, `storage-database.md` (DataStore, Room), `testing.md`, `release.md` (versi, signing, CI, Firebase). Demo SDK dijelaskan ringkas dalam satu `docs/examples.md` (tujuan, API Android, izin, fallback API level per demo).
- **Skill proyek** (format `SKILL.md` dengan frontmatter `name` + `description`, seperti `.agents/skills` di repo RN): panduan langkah demi langkah untuk tugas yang sering diulang di starter kit ini, minimal:
  - `add-feature` — membuat paket fitur baru (screen, ViewModel, repository, DI, route, tes).
  - `add-screen-route` — menambah layar dan route, publik atau protected.
  - `add-api-endpoint` — menambah endpoint Retrofit + model + mapping error + tes.
  - `add-component-example` — menambah contoh komponen Compose ke halaman galeri, dengan tes UI.
  - `add-translation` — menambah string en/id dan menjaga tes kelengkapan.
  - `add-permission-feature` — fitur device yang butuh izin memakai `PermissionGate`.
  - `swap-auth-backend` — menulis `AuthAdapter` untuk backend nyata.
  - `remove-examples` — membuang `feature/example` dengan bersih.
  - `release-build` — menaikkan versi, build release, distribusi.
- Dokumen dan skill ditulis dalam bahasa Inggris (konsisten dengan README) **[call Claude]**, dan harus tetap sinkron dengan kode: setiap rujukan file di dokumen harus menunjuk file yang benar-benar ada.

## 10. Diserahkan ke builder

Versi pasti library, desain visual detail komponen (ikuti token & RN sebagai acuan), urutan implementasi, dan detail kecil yang tidak mengubah perilaku di atas.
