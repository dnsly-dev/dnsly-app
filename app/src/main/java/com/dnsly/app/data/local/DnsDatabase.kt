package com.dnsly.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dnsly.app.model.QueryLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance SQLite database helper for local DNS query logging and analytics.
 * Employs Write-Ahead Logging (WAL) for non-blocking concurrent reads & writes.
 */
class DnsDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "dnsly_analytics.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_QUERY_LOGS = "query_logs"
        private const val COL_ID = "id"
        private const val COL_DOMAIN = "domain"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_IS_BLOCKED = "is_blocked"
        private const val COL_QUERY_TYPE = "query_type"
        private const val COL_UPSTREAM = "upstream_server"
        private const val COL_LATENCY_MS = "latency_ms"
        private const val COL_REASON = "reason"
        private const val COL_PROTOCOL = "protocol"

        private const val MAX_STORED_LOGS = 1000

        @Volatile
        private var INSTANCE: DnsDatabase? = null

        fun getInstance(context: Context): DnsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DnsDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Enable Write-Ahead Logging for maximum concurrency and zero write-blocking
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_QUERY_LOGS (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_DOMAIN TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_BLOCKED INTEGER NOT NULL,
                $COL_QUERY_TYPE TEXT NOT NULL,
                $COL_UPSTREAM TEXT NOT NULL,
                $COL_LATENCY_MS INTEGER NOT NULL,
                $COL_REASON TEXT NOT NULL,
                $COL_PROTOCOL TEXT DEFAULT 'DoH'
            );
        """.trimIndent()

        db.execSQL(createTableQuery)
        db.execSQL("CREATE INDEX idx_logs_timestamp ON $TABLE_QUERY_LOGS ($COL_TIMESTAMP DESC);")
        db.execSQL("CREATE INDEX idx_logs_blocked ON $TABLE_QUERY_LOGS ($COL_IS_BLOCKED);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_QUERY_LOGS ADD COLUMN $COL_PROTOCOL TEXT DEFAULT 'DoH';")
            } catch (_: Exception) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_QUERY_LOGS")
                onCreate(db)
            }
        }
    }

    /**
     * Inserts a query log record and trims older logs when capacity exceeds MAX_STORED_LOGS.
     */
    suspend fun insertLog(log: QueryLog): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_ID, log.id)
            put(COL_DOMAIN, log.domain)
            put(COL_TIMESTAMP, log.timestamp)
            put(COL_IS_BLOCKED, if (log.isBlocked) 1 else 0)
            put(COL_QUERY_TYPE, log.queryType)
            put(COL_UPSTREAM, log.upstreamServer)
            put(COL_LATENCY_MS, log.latencyMs)
            put(COL_REASON, log.reason)
            put(COL_PROTOCOL, log.protocol)
        }

        val db = writableDatabase
        val rowId = db.insertWithOnConflict(TABLE_QUERY_LOGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)

        // Periodic pruning: keep only latest MAX_STORED_LOGS
        if (log.id % 50 == 0L) {
            pruneOldLogs(db)
        }

        rowId
    }

    /**
     * Efficiently loads the latest N query logs ordered by descending timestamp.
     */
    suspend fun getRecentLogs(limit: Int = 200): List<QueryLog> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<QueryLog>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_QUERY_LOGS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_ID)
            val domainIndex = c.getColumnIndexOrThrow(COL_DOMAIN)
            val timestampIndex = c.getColumnIndexOrThrow(COL_TIMESTAMP)
            val isBlockedIndex = c.getColumnIndexOrThrow(COL_IS_BLOCKED)
            val queryTypeIndex = c.getColumnIndexOrThrow(COL_QUERY_TYPE)
            val upstreamIndex = c.getColumnIndexOrThrow(COL_UPSTREAM)
            val latencyIndex = c.getColumnIndexOrThrow(COL_LATENCY_MS)
            val reasonIndex = c.getColumnIndexOrThrow(COL_REASON)
            val protocolIndex = c.getColumnIndex(COL_PROTOCOL)

            while (c.moveToNext()) {
                val protocol = if (protocolIndex >= 0 && !c.isNull(protocolIndex)) {
                    c.getString(protocolIndex)
                } else "DoH"

                logs.add(
                    QueryLog(
                        id = c.getLong(idIndex),
                        domain = c.getString(domainIndex),
                        timestamp = c.getLong(timestampIndex),
                        isBlocked = c.getInt(isBlockedIndex) == 1,
                        queryType = c.getString(queryTypeIndex),
                        upstreamServer = c.getString(upstreamIndex),
                        latencyMs = c.getLong(latencyIndex),
                        reason = c.getString(reasonIndex),
                        protocol = protocol
                    )
                )
            }
        }
        logs
    }

    /**
     * Clears all stored query logs from SQLite.
     */
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_QUERY_LOGS, null, null)
    }

    private fun pruneOldLogs(db: SQLiteDatabase) {
        try {
            db.execSQL(
                """
                DELETE FROM $TABLE_QUERY_LOGS 
                WHERE $COL_ID NOT IN (
                    SELECT $COL_ID FROM $TABLE_QUERY_LOGS 
                    ORDER BY $COL_TIMESTAMP DESC 
                    LIMIT $MAX_STORED_LOGS
                );
                """.trimIndent()
            )
        } catch (_: Exception) {
            // Ignore pruning errors if database is locked
        }
    }
}
