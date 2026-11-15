package dev.leemh.charok

/** History is capped at 100 rows; preservation/duplicate tests must count the actual database. */
internal fun CharokApp.savedRecordCount(): Long = db.openHelper.readableDatabase
    .query("SELECT COUNT(*) FROM sessions").use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }
