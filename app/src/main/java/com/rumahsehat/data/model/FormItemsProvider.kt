package com.rumahsehat.data.model

import com.rumahsehat.R

object FormItemsProvider {
    fun getFormItems(): List<FormItem> {
        return listOf(
            // I. Komponen Rumah (8 Indikator)
            FormItem("1.1", R.string.item_ceiling, 20, listOf(
                Option('a', "Tidak ada langit-langit", 0f),
                Option('b', "Ada, tapi kotor / sulit dibersihkan / rawan kecelakaan", 0.5f),
                Option('c', "Ada, bersih, dan tidak rawan kecelakaan", 1f)
            )),
            FormItem("1.2", R.string.item_walls, 20, listOf(
                Option('a', "Bukan tembok (anyaman bambu / ilalang / daun)", 0f),
                Option('b', "Semi permanen (tembok tanpa plesteran / papan tidak kedap)", 0.5f),
                Option('c', "Permanen (tembok dipelester / papan kedap air)", 1f)
            )),
            FormItem("1.3", R.string.item_floor, 20, listOf(
                Option('a', "Tanah murni", 0f),
                Option('b', "Papan/bambu dekat tanah, plesteran retak", 0.5f),
                Option('c', "Ubin / keramik / plesteran keras kedap air", 1f)
            )),
            FormItem("1.4", R.string.item_bedroom_windows, 20, listOf(
                Option('a', "Tidak ada jendela", 0f),
                Option('b', "Ada, tapi tidak dapat dibuka / terlalu kecil", 0.5f),
                Option('c', "Ada, dapat dibuka, luas memenuhi syarat", 1f)
            )),
            FormItem("1.5", R.string.item_living_windows, 20, listOf(
                Option('a', "Tidak ada jendela", 0f),
                Option('b', "Ada, tapi tidak dapat dibuka / terlalu kecil", 0.5f),
                Option('c', "Ada, dapat dibuka, luas memenuhi syarat", 1f)
            )),
            FormItem("1.6", R.string.item_ventilation, 20, listOf(
                Option('a', "Tidak ada ventilasi", 0f),
                Option('b', "Ada, tapi luas &lt; 10% luas lantai / jarang dibuka", 0.5f),
                Option('c', "Ada, luas ≥ 10% luas lantai dan dapat dibuka", 1f)
            )),
            FormItem("1.7", R.string.item_kitchen_smoke, 20, listOf(
                Option('a', "Tidak ada lubang asap dapur", 0f),
                Option('b', "Ada, tapi tidak berfungsi / selalu tertutup", 0.5f),
                Option('c', "Ada, dan asap dapur keluar dengan baik", 1f)
            )),
            FormItem("1.8", R.string.item_lighting, 20, listOf(
                Option('a', "Tidak ada pencahayaan yang cukup", 0f),
                Option('b', "Ada, tapi redup / tidak memenuhi syarat", 0.5f),
                Option('c', "Ada, cukup terang untuk kegiatan sehari-hari", 1f)
            )),

            // II. Sarana Sanitasi (4 Indikator) - Essential Categories
            FormItem("2.1", R.string.item_clean_water, 150, listOf(
                Option('a', "Tidak ada", 0f),
                Option('b', "Ada, bukan milik sendiri dan tidak memenuhi syarat kesehatan", 0.5f),
                Option('c', "Ada, milik sendiri dan memenuhi syarat kesehatan", 1f)
            )),
            FormItem("2.2", R.string.item_latrine, 150, listOf(
                Option('a', "Tidak ada jamban", 0f),
                Option('b', "Ada, non-leher angsa / tanpa tutup / disalurkan ke sungai-kolam", 0.5f),
                Option('c', "Ada, leher angsa, ada tutup, septic tank", 1f)
            )),
            FormItem("2.3", R.string.item_spal, 100, listOf(
                Option('a', "Tidak ada SPAL", 0f),
                Option('b', "Ada, tapi terbuka / air limbah menggenang", 0.5f),
                Option('c', "Ada, tertutup dan berfungsi baik", 1f)
            )),
            FormItem("2.4", R.string.item_garbage, 150, listOf(
                Option('a', "Tidak ada tempat sampah", 0f),
                Option('b', "Ada, tapi terbuka / tidak ditutup / mudah didatangi vektor", 0.5f),
                Option('c', "Ada, tertutup dan memenuhi syarat", 1f)
            )),

            // III. Perilaku Penghuni (5 Indikator)
            FormItem("3.1", R.string.item_behavior_bedroom_win, 20, listOf(
                Option('a', "Tidak pernah dibuka", 0f),
                Option('b', "Kadang-kadang", 0.5f),
                Option('c', "Setiap hari dibuka", 1f)
            )),
            FormItem("3.2", R.string.item_behavior_living_win, 20, listOf(
                Option('a', "Tidak pernah dibuka", 0f),
                Option('b', "Kadang-kadang", 0.5f),
                Option('c', "Setiap hari dibuka", 1f)
            )),
            FormItem("3.3", R.string.item_behavior_cleaning, 20, listOf(
                Option('a', "Tidak pernah membersihkan", 0f),
                Option('b', "Kadang-kadang", 0.5f),
                Option('c', "Setiap hari", 1f)
            )),
            FormItem("3.4", R.string.item_behavior_feces, 20, listOf(
                Option('a', "Dibuang ke sungai/kebun/kolam sembarangan", 0f),
                Option('b', "Kadang-kadang ke jamban", 0.5f),
                Option('c', "Setiap hari dibuang ke jamban", 1f),
                Option('d', "Tidak ada bayi/balita di rumah (tidak berlaku)", 1f)
            )),
            FormItem("3.5", R.string.item_behavior_trash, 20, listOf(
                Option('a', "Dibuang ke sungai / kebun / kolam sembarangan", 0f),
                Option('b', "Kadang-kadang dibuang ke tempat sampah", 0.5f),
                Option('c', "Setiap hari dibuang ke tempat sampah", 1f)
            ))
        )
    }
}