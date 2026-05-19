package com.example.maszyna_stanow.data

import androidx.room.*
import com.example.maszyna_stanow.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StateMachineDao {
    @Transaction
    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<FullProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStates(states: List<StateEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransitions(transitions: List<TransitionEntity>): List<Long>

    @Query("DELETE FROM states WHERE projectId = :projectId")
    suspend fun deleteStatesByProject(projectId: Long): Int

    @Query("DELETE FROM transitions WHERE projectId = :projectId")
    suspend fun deleteTransitionsByProject(projectId: Long): Int

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: Long): Int
}
