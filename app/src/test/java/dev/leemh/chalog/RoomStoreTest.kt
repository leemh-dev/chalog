package dev.leemh.chalog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import dev.leemh.chalog.core.*
import dev.leemh.chalog.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomStoreTest {
    @Test fun sqliteCloseReopenPreservesCheckpointAndSessionIdentity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${System.nanoTime()}.db"
        fun db() = Room.databaseBuilder(context, RecordsDb::class.java, name).allowMainThreadQueries().build()
        val first = db(); val clock = TestClock(); val r = Recorder(clock, RoomRecordStore(first))
        r.arm(InputMode.TEST); r.observe(Observation("gear", "PARK", "enum", Origin.TEST, clock.time, clock.wallTime))
        val id = r.start(); r.note("핸들을 돌릴 때 소리", Origin.TEST, true)
        first.close()
        val second = db(); val store = RoomRecordStore(second); val restored = Recorder(clock, store); restored.restore()
        assertEquals(id, restored.session!!.id); assertEquals(Phase.INTERRUPTED, restored.phase)
        assertEquals(1, store.get(id)!!.notes.size); assertEquals(1, store.get(id)!!.observations.size)
        assertNull(store.get(id)!!.endedAt); second.close(); context.deleteDatabase(name)
    }
}
