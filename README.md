# 🍱 Sandomeshi SRS (三度飯 SRS)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini-orange.svg)](https://ai.google.dev/)

**Sandomeshi SRS** adalah aplikasi Android berbasis AI yang dirancang untuk membantu pembelajar bahasa Jepang mengubah konten hiburan (seperti anime) menjadi materi pembelajaran yang efektif. Aplikasi ini secara otomatis mengekstraksi kosakata, kanji, dan tata bahasa dari file subtitle (.srt atau .ass) dan mengubahnya menjadi format dek flashcard Anki.

---

## ✨ Fitur Utama

- **🧠 Ekstraksi Berbasis Gemini AI**: Menggunakan kekuatan Google Gemini (Flash & Pro) untuk menganalisis teks subtitle secara cerdas.
- **🏮 Dukungan JLPT Lengkap**: Pilih target pembelajaran Anda dari level N5 hingga N1.
- **📥 Pemroses Subtitle**: Mendukung format file `.srt` dan `.ass`.
- **🇮🇩 Bilingual Support**: Penjelasan kartu dan terjemahan dapat disetel ke dalam Bahasa Indonesia atau Bahasa Inggris.
- **🚀 Background Processing**: Dilengkapi dengan sistem notifikasi progres, memungkinkan pembuatan dek besar tetap berjalan meskipun aplikasi di latar belakang.
- **🎨 Material 3 Expressive UI**: Antarmuka modern dengan desain *Edge-to-Edge*, navigasi melayang, dan elemen visual yang ergonomis.
- **📦 Export to Anki**: Menghasilkan file `.txt` yang siap diimpor langsung ke aplikasi Anki (Desktop/Mobile).

---

## 📸 Tampilan Aplikasi

| Jelajah Subtitle | Pembuat Dek | Pengaturan |
| :---: | :---: | :---: |
| <img src="https://github.com/user-attachments/assets/3bad6f23-494f-445e-89ee-8605810c028f" width="250" alt="Jelajah Subtitle"> | <img src="https://github.com/user-attachments/assets/b0220893-9f9d-4f5a-97d0-03dd00ca2fb1" width="250" alt="Pembuat Dek"> | <img src="https://github.com/user-attachments/assets/627d0e0b-7e64-43a9-acdf-fa8481258755" width="250" alt="Pengaturan"> |

---

## 🛠️ Teknologi yang Digunakan

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Concurrency**: Kotlin Coroutines & Flow
- **Navigation**: Compose Navigation
- **AI Integration**: Google Generative AI SDK (Gemini)
- **Dependency Injection**: ViewModel & Hoisting State
- **Architecture**: MVVM (Model-View-ViewModel)

---

## 🚀 Cara Memulai

### Prasyarat
1. Perangkat Android dengan API Level 27+ (Android 8.1+).
2. Google Gemini API Key. Anda bisa mendapatkannya secara gratis di [Google AI Studio](https://aistudio.google.com/).

### Instalasi
1. Unduh APK terbaru dari tab [Releases](https://github.com/Dwixd/Sandomeshi-SRS/releases).
2. Instal file `.apk` di perangkat Anda.
3. Buka aplikasi, masuk ke menu **Pengaturan**, dan masukkan Gemini API Key Anda.
4. Anda siap membuat dek!

---

## 📝 Cara Penggunaan

1. **Pilih Subtitle**: Masuk ke tab **Jelajah** dan pilih file subtitle anime yang ingin dipelajari.
2. **Konfigurasi**: Pilih level JLPT yang diinginkan dan tipe kartu (Kosakata, Kanji, atau Tata Bahasa).
3. **Generate**: Tekan tombol **Buat Flashcard**. Pantau progres melalui notifikasi atau bar progres di aplikasi.
4. **Export**: Setelah selesai, klik **Export to Anki** untuk menyimpan file hasil ekstraksi.
5. **Import ke Anki**: Pindahkan file `.txt` ke komputer, buka Anki, dan pilih **File > Import**.

---

## 🤝 Kontribusi

Kontribusi selalu terbuka! Jika Anda memiliki ide fitur atau menemukan bug, silakan buat *Issue* atau kirimkan *Pull Request*.

---

## ⚖️ Lisensi

Distribusi di bawah Lisensi MIT. Lihat `LICENSE` untuk informasi lebih lanjut.

---
*Dibuat dengan ❤️ untuk komunitas pembelajar bahasa Jepang.*
