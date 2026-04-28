package com.example.maszyna_stanow.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.maszyna_stanow.model.StateNode
import com.example.maszyna_stanow.model.Transition
import java.util.UUID

class MainViewModel : ViewModel() {
    private val _states = mutableStateListOf<StateNode>()
    val states: List<StateNode> = _states

    private val _transitions = mutableStateListOf<Transition>()
    val transitions: List<Transition> = _transitions

    private val _selectedState = mutableStateOf<StateNode?>(null)
    val selectedState: State<StateNode?> = _selectedState

    private val _selectedTransition = mutableStateOf<Transition?>(null)
    val selectedTransition: State<Transition?> = _selectedTransition

    private val _isMoveMode = mutableStateOf(false)
    val isMoveMode: State<Boolean> = _isMoveMode

    private val _transitionSourceId = mutableStateOf<String?>(null)
    val transitionSourceId: State<String?> = _transitionSourceId

    // Etap 3: Aktywny stan podczas symulacji
    private val _activeStateId = mutableStateOf<String?>(null)
    val activeStateId: State<String?> = _activeStateId

    fun addState(name: String, position: Offset, isInitial: Boolean = false, isFinal: Boolean = false) {
        val newState = StateNode(
            id = UUID.randomUUID().toString(),
            name = name,
            position = position,
            isInitial = isInitial,
            isFinal = isFinal
        )
        _states.add(newState)
        // Jeśli to pierwszy stan, ustawiamy go jako aktywny
        if (_activeStateId.value == null && isInitial) {
            _activeStateId.value = newState.id
        }
    }

    // Edycja sygnału przejścia
    fun updateTransitionSignal(id: String, newSignal: String) {
        val index = _transitions.indexOfFirst { it.id == id }
        if (index != -1) {
            _transitions[index] = _transitions[index].copy(signal = newSignal)
        }
    }

    // Rdzeń Etapu 3: Procesowanie sygnału
    fun processSignal(signalName: String) {
        val currentId = _activeStateId.value ?: _states.find { it.isInitial }?.id ?: return
        _activeStateId.value = currentId // upewnij się że startujemy z initial

        val transition = _transitions.find { it.fromStateId == currentId && it.signal == signalName }
        if (transition != null) {
            _activeStateId.value = transition.toStateId
        }
    }

    fun resetSimulation() {
        _activeStateId.value = _states.find { it.isInitial }?.id
    }

    fun toggleMoveMode() {
        _isMoveMode.value = !_isMoveMode.value
        _transitionSourceId.value = null
        _selectedTransition.value = null
    }

    fun toggleTransitionMode() {
        if (_transitionSourceId.value == null) {
            _transitionSourceId.value = _selectedState.value?.id
        } else {
            val toId = _selectedState.value?.id
            val fromId = _transitionSourceId.value
            if (toId != null && fromId != null) {
                // Generujemy unikalny sygnał na podstawie liczby wyjść z tego konkretnego stanu
                val outTransitionsCount = _transitions.count { it.fromStateId == fromId }
                val signal = outTransitionsCount.toString()
                
                addTransition(fromId, toId, signal)
            }
            _transitionSourceId.value = null
        }
    }

    fun updateStatePosition(id: String, newPosition: Offset) {
        val index = _states.indexOfFirst { it.id == id }
        if (index != -1) {
            _states[index] = _states[index].copy(position = newPosition)
        }
        _isMoveMode.value = false
    }

    fun toggleInitial(id: String) {
        val index = _states.indexOfFirst { it.id == id }
        if (index != -1) {
            val isCurrentlyInitial = _states[index].isInitial
            for (i in _states.indices) {
                _states[i] = _states[i].copy(isInitial = false)
            }
            _states[index] = _states[index].copy(isInitial = !isCurrentlyInitial)
            if (_states[index].isInitial) _activeStateId.value = id
        }
    }

    fun toggleFinal(id: String) {
        val index = _states.indexOfFirst { it.id == id }
        if (index != -1) {
            _states[index] = _states[index].copy(isFinal = !_states[index].isFinal)
        }
    }

    fun deleteSelectedState() {
        _selectedState.value?.let { state ->
            _transitions.removeIf { it.fromStateId == state.id || it.toStateId == state.id }
            _states.remove(state)
            _selectedState.value = null
            if (_activeStateId.value == state.id) _activeStateId.value = null
        }
    }

    fun deleteSelectedTransition() {
        _selectedTransition.value?.let { transition ->
            _transitions.remove(transition)
            _selectedTransition.value = null
        }
    }

    fun selectState(state: StateNode?) {
        _selectedState.value = state
        _selectedTransition.value = null
        if (state == null) {
            _isMoveMode.value = false
            _transitionSourceId.value = null
        }
    }

    fun selectTransition(transition: Transition?) {
        _selectedTransition.value = transition
        _selectedState.value = null
    }

    private fun addTransition(fromId: String, toId: String, signal: String) {
        // Dodatkowe zabezpieczenie: jeśli taki sygnał już istnieje, dodaj przyrostek, aby był unikalny
        var finalSignal = signal
        var counter = 1
        while (_transitions.any { it.fromStateId == fromId && it.signal == finalSignal }) {
            finalSignal = "$signal($counter)"
            counter++
        }

        val newTransition = Transition(
            id = UUID.randomUUID().toString(),
            fromStateId = fromId,
            toStateId = toId,
            signal = finalSignal
        )
        _transitions.add(newTransition)
    }
}
