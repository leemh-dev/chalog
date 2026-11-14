package dev.leemh.charok

import android.app.Application
import dev.leemh.charok.data.RecordsDb
import dev.leemh.charok.data.RoomRecordStore

class CharokApp : Application() {
    val db by lazy { RecordsDb.create(this) }
    val store by lazy { RoomRecordStore(db) }
}
