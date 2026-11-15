package dev.leemh.charok.data

import android.content.Context
import androidx.room.*
import dev.leemh.charok.core.*
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "sessions")
data class StoredSession(@PrimaryKey val id: String, val startedAt: Long, val payload: String)
@Dao interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun put(value: StoredSession)
    @Query("SELECT * FROM sessions WHERE id=:id") fun get(id: String): StoredSession?
    @Query("SELECT * FROM sessions ORDER BY rowid DESC LIMIT 1") fun latest(): StoredSession?
    @Query("SELECT * FROM sessions ORDER BY rowid DESC LIMIT 100") fun all(): List<StoredSession>
}
@Database(entities = [StoredSession::class], version = 1, exportSchema = true)
abstract class RecordsDb : RoomDatabase() {
    abstract fun sessions(): SessionDao
    companion object { fun create(context: Context) = Room.databaseBuilder(context, RecordsDb::class.java, "charok.db").build() }
}

class RoomRecordStore(private val db: RecordsDb) : RecordStore {
    override fun save(session: Session) {
        val payload = SessionCodec.encode(session)
        check(payload.toByteArray(Charsets.UTF_8).size <= 10 * 1024 * 1024) { "기록 크기 상한" }
        db.runInTransaction {
            db.sessions().put(StoredSession(session.id, session.startedAt, payload))
            check(get(session.id) == session) { "DB 재조회 불일치" }
        }
    }
    override fun get(id: String) = db.sessions().get(id)?.let { SessionCodec.decode(it.payload) }
    override fun latest() = db.sessions().latest()?.let { SessionCodec.decode(it.payload) }
    override fun list() = db.sessions().all().map { SessionCodec.decode(it.payload) }
}

object SessionCodec {
    fun encode(s: Session): String = JSONObject().apply {
        put("schema", 1); put("id", s.id); put("mode", s.mode.name); put("startedAt", s.startedAt)
        put("triggerElapsed", s.triggerElapsed); put("phase", s.phase.name); put("endedAt", s.endedAt ?: JSONObject.NULL); put("reason", s.reason)
        put("observations", JSONArray().apply { s.observations.forEach { o -> put(JSONObject().apply {
            put("field", o.field); put("value", o.value); put("unit", o.unit); put("origin", o.origin.name)
            put("elapsed", o.elapsed); put("receivedAt", o.receivedAt); put("available", o.available); put("sampleAt", o.sampleAt ?: JSONObject.NULL)
        }) } })
        put("notes", JSONArray().apply { s.notes.forEach { n -> put(JSONObject().apply {
            put("id", n.id); put("text", n.text); put("origin", n.origin.name); put("elapsed", n.elapsed); put("at", n.at); put("bookmark", n.bookmark)
        }) } })
    }.toString()
    fun decode(text: String): Session {
        val j = JSONObject(text); require(j.getInt("schema") == 1)
        val observations = j.getJSONArray("observations")
        val notes = j.getJSONArray("notes")
        return Session(j.getString("id"), InputMode.valueOf(j.getString("mode")), j.getLong("startedAt"), j.getLong("triggerElapsed"),
            Phase.valueOf(j.getString("phase")), if (j.isNull("endedAt")) null else j.getLong("endedAt"), j.getString("reason"),
            List(observations.length()) { i -> observations.getJSONObject(i).let { o -> Observation(o.getString("field"), o.getString("value"), o.getString("unit"), Origin.valueOf(o.getString("origin")), o.getLong("elapsed"), o.getLong("receivedAt"), o.getBoolean("available"), if (o.isNull("sampleAt")) null else o.getLong("sampleAt")) } },
            List(notes.length()) { i -> notes.getJSONObject(i).let { n -> Note(n.getString("id"), n.getString("text"), Origin.valueOf(n.getString("origin")), n.getLong("elapsed"), n.getLong("at"), n.getBoolean("bookmark")) } })
    }
}

object ConsultationReport {
    fun text(s: Session): String = buildString {
        appendLine("차록 · 정비 상담 기록"); appendLine("기록 ID: ${s.id}")
        appendLine("입력 환경: ${s.mode} / 상태: ${s.phase}"); appendLine("시작 UTC epoch ms: ${s.startedAt}")
        appendLine("종료 UTC epoch ms: ${s.endedAt ?: "미확인: 마지막 checkpoint까지만 복원"}")
        appendLine("종료 사유: ${s.reason}")
        appendLine("원음 녹음·고장 진단·업체 수신 확인은 포함하지 않습니다.")
        appendLine("\n사용자 설명과 순간 표시")
        s.notes.forEach { appendLine("${it.at} [${it.origin}] ${if (it.bookmark) "사용자 순간 표시: " else ""}${it.text}") }
        appendLine("\n관측된 경고·표시등 / 기록 구간의 관측이며 현재 상태·고장 진단이 아닙니다")
        val lamps = s.observedLamps()
        if (lamps.isEmpty()) appendLine("${s.lampCoverage()} · 미수신은 정상 판정이 아닙니다.")
        lamps.forEach { appendLine("${it.label} [${it.type}] · 최초 관측 ${it.firstLit.receivedAt} [${it.firstLit.origin}] · 마지막 확인 ${it.latest.receivedAt}: ${it.stateLabel}") }
        appendLine("\n실제 확보한 관측 / TEST는 실차 데이터가 아닙니다")
        s.observations.forEach { appendLine("${it.receivedAt} [${it.origin}] ${it.field}: ${if (it.available) it.value else "수신 불가"} ${it.unit}; sampleAt=${it.sampleAt ?: "미제공"}") }
        appendLine("\n받지 못한 필드/관측 사이 구간은 미확인입니다. 누락을 정상으로 판단하지 않습니다.")
    }
}
