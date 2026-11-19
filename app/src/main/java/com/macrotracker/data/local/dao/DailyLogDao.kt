package com.macrotracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.macrotracker.data.local.entity.DailyLog
import com.macrotracker.data.local.entity.WaterLog
import com.macrotracker.data.local.entity.WeightLog
import com.macrotracker.data.local.relation.DailyMacroSummary
import com.macrotracker.data.local.relation.DailyWaterSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    // --- Daily Food/Recipe Logs ---
    @Query("SELECT * FROM daily_logs WHERE date = :date ORDER BY id ASC")
    fun observeLogsForDate(date: String): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE date = :date ORDER BY id ASC")
    suspend fun getLogsForDate(date: String): List<DailyLog>

    @Query("SELECT * FROM daily_logs ORDER BY id DESC LIMIT 20")
    fun observeRecentLogs(): Flow<List<DailyLog>>

    @Query(
        """
        SELECT date,
               SUM(cachedKcal) AS totalKcal,
               SUM(cachedProtein) AS totalProtein,
               SUM(cachedCarbs) AS totalCarbs,
               SUM(cachedFat) AS totalFat
        FROM daily_logs
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        """
    )
    fun observeDailyMacroSummaries(startDate: String, endDate: String): Flow<List<DailyMacroSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLogs(logs: List<DailyLog>)

    @Update
    suspend fun updateDailyLog(log: DailyLog)

    @Delete
    suspend fun deleteDailyLog(log: DailyLog)

    // --- Water Logs ---
    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE date = :date")
    fun observeWaterTotalForDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE date = :date")
    suspend fun getWaterTotalForDate(date: String): Int

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp DESC")
    fun observeWaterLogsForDate(date: String): Flow<List<WaterLog>>

    @Query(
        """
        SELECT date, SUM(amountMl) AS totalMl
        FROM water_logs
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
        """
    )
    fun observeDailyWaterSummaries(startDate: String, endDate: String): Flow<List<DailyWaterSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog): Long

    @Delete
    suspend fun deleteWaterLog(log: WaterLog)

    // --- Weight Logs ---
    @Query("SELECT * FROM weight_logs ORDER BY date ASC, timestamp ASC")
    fun observeAllWeightLogs(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY date DESC, timestamp DESC LIMIT 1")
    fun observeLatestWeight(): Flow<WeightLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog): Long

    @Delete
    suspend fun deleteWeightLog(log: WeightLog)
}
