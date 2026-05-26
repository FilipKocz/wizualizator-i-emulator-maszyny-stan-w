package com.example.maszyna_stanow.model

import androidx.compose.ui.geometry.Offset
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

// Modele UI
data class StateNode(
    val id: String,
    val name: String,
    val position: Offset = Offset.Zero,
    val isInitial: Boolean = false,
    val isFinal: Boolean = false,
    val message: String = ""
)

data class Transition(
    val id: String,
    val fromStateId: String,
    val toStateId: String,
    val signal: String
)

// Encje Room
//Nazwa maszyny
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)


//Przechowuje stany
@Entity(tableName = "states")
data class StateEntity(
    @PrimaryKey val id: String,
    val projectId: Long,
    val name: String,
    val posX: Float,
    val posY: Float,
    val isInitial: Boolean,
    val isFinal: Boolean,
    val message: String
)

//Przechowuje sygnały
@Entity(tableName = "transitions")
data class TransitionEntity(
    @PrimaryKey val id: String,
    val projectId: Long,
    val fromStateId: String,
    val toStateId: String,
    val signal: String
)


//Klasa relacyjna, która pozwala jednym zapytaniem wyciągnąć z bazy cały porjekt
//z jego wszystkimi elementami
data class FullProject(
    @Embedded val project: ProjectEntity,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val states: List<StateEntity>,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val transitions: List<TransitionEntity>
)
