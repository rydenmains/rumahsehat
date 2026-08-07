package com.rumahsehat.data.model

import com.rumahsehat.R

object FormItemsProvider {
    fun getFormItems(): List<FormItem> {
        return listOf(
            // I. Komponen Rumah (8 Indikator)
            FormItem("1.1", R.string.item_ceiling, 20),
            FormItem("1.2", R.string.item_walls, 20),
            FormItem("1.3", R.string.item_floor, 20),
            FormItem("1.4", R.string.item_bedroom_windows, 20),
            FormItem("1.5", R.string.item_living_windows, 20),
            FormItem("1.6", R.string.item_ventilation, 20),
            FormItem("1.7", R.string.item_kitchen_smoke, 20),
            FormItem("1.8", R.string.item_lighting, 20),
            
            // II. Sarana Sanitasi (4 Indikator) - Essential Categories
            FormItem("2.1", R.string.item_clean_water, 150),
            FormItem("2.2", R.string.item_latrine, 150),
            FormItem("2.3", R.string.item_spal, 100),
            FormItem("2.4", R.string.item_garbage, 150),
            
            // III. Perilaku Penghuni (5 Indikator)
            FormItem("3.1", R.string.item_behavior_bedroom_win, 20),
            FormItem("3.2", R.string.item_behavior_living_win, 20),
            FormItem("3.3", R.string.item_behavior_cleaning, 20),
            FormItem("3.4", R.string.item_behavior_feces, 20),
            FormItem("3.5", R.string.item_behavior_trash, 20)
        )
    }
}
