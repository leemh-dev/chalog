package dev.leemh.chalog

import android.app.Application
import dev.leemh.chalog.data.RecordsDb
import dev.leemh.chalog.data.RoomRecordStore

class ChalogApp : Application() {
    // The service and every Activity share one serialized recorder for this process.
    val recorder by lazy { RecorderModel(this) }
    val db by lazy { RecordsDb.create(this) }
    val store by lazy { RoomRecordStore(db) }
}
