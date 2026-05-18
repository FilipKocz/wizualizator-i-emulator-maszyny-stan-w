package com.example.maszyna_stanow.model

import androidx.compose.ui.geometry.Offset

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

data class StateMachineProject(
    val id: Long = 0,
    val name: String,
    val states: List<StateNode> = emptyList(),
    val transitions: List<Transition> = emptyList()
)
