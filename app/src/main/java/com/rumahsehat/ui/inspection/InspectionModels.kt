package com.rumahsehat.ui.inspection

data class FormQuestion(
    val id: String,
    val title: String,
    val subtitle: String,
    val options: List<String>,
    val maxScore: Int
)

data class FormSection(
    val stepNumber: Int,
    val title: String,
    val photoTitle: String,
    val photoSubtitle: String,
    val photoCategoryId: String,
    val questions: List<FormQuestion>
)

val TahapKomponenRumah = FormSection(
    stepNumber = 1,
    title = "Komponen Rumah",
    photoTitle = "Dokumentasi Tampak Depan Rumah (Foto 1)",
    photoSubtitle = "Ambil foto rumah dari arah depan untuk verifikasi lokasi.",
    photoCategoryId = "house_front",
    questions = listOf(
        FormQuestion("1.1", "Kondisi Langit-langit (Plafon)", "Periksa kondisi dan kelayakan plafon rumah.", listOf(
            "Tidak ada langit-langit",
            "Ada, tapi kotor / sulit dibersihkan / rawan kecelakaan",
            "Ada, bersih, dan tidak rawan kecelakaan"
        ), 20),
        FormQuestion("1.2", "Material & Kondisi Dinding", "Pilih jenis dan kondisi material dinding rumah.", listOf(
            "Bukan tembok (anyaman bambu / ilalang / daun)",
            "Semi permanen (tembok tanpa plesteran / papan tidak kedap)",
            "Permanen (tembok dipelester / papan kedap air)"
        ), 20),
        FormQuestion("1.3", "Jenis Lantai Rumah", "Pilih jenis dan kondisi lantai rumah.", listOf(
            "Tanah murni",
            "Papan/bambu dekat tanah, plesteran retak",
            "Ubin / keramik / plesteran keras kedap air"
        ), 20),
        FormQuestion("1.4", "Jendela Kamar Tidur", "Periksa ketersediaan dan fungsi jendela kamar tidur.", listOf(
            "Tidak ada jendela",
            "Ada, tapi tidak dapat dibuka / terlalu kecil",
            "Ada, dapat dibuka, luas memenuhi syarat"
        ), 20),
        FormQuestion("1.5", "Jendela Ruang Keluarga", "Periksa ketersediaan dan fungsi jendela ruang keluarga.", listOf(
            "Tidak ada jendela",
            "Ada, tapi tidak dapat dibuka / terlalu kecil",
            "Ada, dapat dibuka, luas memenuhi syarat"
        ), 20),
        FormQuestion("1.6", "Ventilasi", "Nilai kecukupan ventilasi rumah (min. 10% luas lantai).", listOf(
            "Tidak ada ventilasi",
            "Ada, tapi luas < 10% luas lantai / jarang dibuka",
            "Ada, luas ≥ 10% luas lantai dan dapat dibuka"
        ), 20),
        FormQuestion("1.7", "Lubang Asap Dapur", "Periksa keberadaan dan fungsi lubang asap dapur.", listOf(
            "Tidak ada lubang asap dapur",
            "Ada, tapi tidak berfungsi / selalu tertutup",
            "Ada, dan asap dapur keluar dengan baik"
        ), 20),
        FormQuestion("1.8", "Pencahayaan", "Nilai kecukupan pencahayaan untuk kegiatan sehari-hari.", listOf(
            "Tidak ada pencahayaan yang cukup",
            "Ada, tapi redup / tidak memenuhi syarat",
            "Ada, cukup terang untuk kegiatan sehari-hari"
        ), 20)
    )
)

val TahapSanitasi = FormSection(
    stepNumber = 2,
    title = "Sarana Sanitasi",
    photoTitle = "Dokumentasi Sarana Sanitasi (Foto 2)",
    photoSubtitle = "Wajib melampirkan 1 foto representatif untuk tahap ini.",
    photoCategoryId = "sanitation",
    questions = listOf(
        FormQuestion("2.1", "Sarana Air Bersih", "Pilih kondisi ketersediaan sarana air bersih yang sesuai.", listOf(
            "Tidak ada",
            "Ada, bukan milik sendiri dan tidak memenuhi syarat kesehatan",
            "Ada, milik sendiri dan memenuhi syarat kesehatan"
        ), 150),
        FormQuestion("2.2", "Jamban", "Evaluasi kondisi dan jenis jamban yang tersedia.", listOf(
            "Tidak ada jamban",
            "Ada, non-leher angsa / tanpa tutup / disalurkan ke sungai-kolam",
            "Ada, leher angsa, ada tutup, septic tank"
        ), 150),
        FormQuestion("2.3", "Saluran Pembuangan Air Limbah (SPAL)", "Periksa kondisi saluran pembuangan air limbah.", listOf(
            "Tidak ada SPAL",
            "Ada, tapi terbuka / air limbah menggenang",
            "Ada, tertutup dan berfungsi baik"
        ), 100),
        FormQuestion("2.4", "Tempat Sampah", "Nilai ketersediaan dan kondisi tempat sampah.", listOf(
            "Tidak ada tempat sampah",
            "Ada, tapi terbuka / tidak ditutup / mudah didatangi vektor",
            "Ada, tertutup dan memenuhi syarat"
        ), 150)
    )
)

val TahapPerilaku = FormSection(
    stepNumber = 3,
    title = "Perilaku Penghuni",
    photoTitle = "Dokumentasi Area Dapur / SPAL (Foto 3)",
    photoSubtitle = "Wajib melampirkan 1 foto representatif untuk tahap ini.",
    photoCategoryId = "kitchen_spal",
    questions = listOf(
        FormQuestion("3.1", "Kebiasaan Membuka Jendela Kamar Tidur", "Seberapa sering penghuni membuka jendela kamar tidur?", listOf(
            "Tidak pernah dibuka",
            "Kadang-kadang",
            "Setiap hari dibuka"
        ), 20),
        FormQuestion("3.2", "Kebiasaan Membuka Jendela Ruang Keluarga", "Seberapa sering penghuni membuka jendela ruang keluarga?", listOf(
            "Tidak pernah dibuka",
            "Kadang-kadang",
            "Setiap hari dibuka"
        ), 20),
        FormQuestion("3.3", "Kebiasaan Membersihkan Rumah", "Seberapa sering penghuni membersihkan rumah?", listOf(
            "Tidak pernah membersihkan",
            "Kadang-kadang",
            "Setiap hari"
        ), 20),
        FormQuestion("3.4", "Pembuangan Tinja Bayi/Balita", "Bagaimana cara penghuni membuang tinja bayi/balita?", listOf(
            "Dibuang ke sungai/kebun/kolam sembarangan",
            "Kadang-kadang ke jamban",
            "Setiap hari dibuang ke jamban",
            "Tidak ada bayi/balita di rumah (tidak berlaku)"
        ), 20),
        FormQuestion("3.5", "Kebiasaan Membuang Sampah", "Bagaimana kebiasaan penghuni membuang sampah rumah tangga?", listOf(
            "Dibuang ke sungai / kebun / kolam sembarangan",
            "Kadang-kadang dibuang ke tempat sampah",
            "Setiap hari dibuang ke tempat sampah"
        ), 20)
    )
)

val AllFormSections = listOf(TahapKomponenRumah, TahapSanitasi, TahapPerilaku)
val AllFormQuestions = AllFormSections.flatMap { it.questions }
