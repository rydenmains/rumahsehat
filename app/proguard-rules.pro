# Room entities (dibaca via reflection oleh Room) wajib dipertahankan.
-keep class * extends androidx.room.RoomDatabase
-keep class com.rumahsehat.data.model.** { *; }
-keep class com.rumahsehat.data.dao.** { *; }

# WorkManager: Worker di-instantiate oleh system via reflection.
-keep class com.rumahsehat.sync.SyncWorker { *; }