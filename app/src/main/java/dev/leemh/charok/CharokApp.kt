package dev.leemh.charok

import android.app.Application
import dev.leemh.charok.data.RecordsDb
import dev.leemh.charok.data.RoomRecordStore

class CharokApp : Application() {
    // The service and every Activity share one serialized recorder for this process.
    val recorder by lazy { RecorderModel(this) }
    val db by lazy { RecordsDb.create(this) }
    val store by lazy { RoomRecordStore(db) }
}
