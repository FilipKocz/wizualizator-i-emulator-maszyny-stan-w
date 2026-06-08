package com.example.maszyna_stanow.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.maszyna_stanow.data.AppDatabase
import com.example.maszyna_stanow.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.stateMachineDao()

    val allProjects: StateFlow<List<FullProject>> = dao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var currentProjectId = 0L
    private val _projectName = mutableStateOf("Mój Projekt")
    val projectName: State<String> = _projectName

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

    private val _isSimulationActive = mutableStateOf(false)
    val isSimulationActive: State<Boolean> = _isSimulationActive

    private val _transitionSourceId = mutableStateOf<String?>(null)
    val transitionSourceId: State<String?> = _transitionSourceId

    private val _activeStateId = mutableStateOf<String?>(null)
    val activeStateId: State<String?> = _activeStateId

    private val _history = mutableStateListOf<String>()
    val history: List<String> = _history

    // Licznik dla unikalnej numeracji stanów
    private var nextStateNumber = 0

    // Walidacja
    private val _isValid = mutableStateOf(false)
    val isValid: State<Boolean> = _isValid

    private val _validationMessage = mutableStateOf("Brak punktów")
    val validationMessage: State<String> = _validationMessage

    fun updateProjectName(newName: String) {
        _projectName.value = newName
    }

    fun createNewProject() {
        currentProjectId = 0L
        nextStateNumber = 0
        _projectName.value = "Nowy Projekt"
        _states.clear()
        _transitions.clear()
        _selectedState.value = null
        _selectedTransition.value = null
        _activeStateId.value = null
        _history.clear()
        validateMachine()
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            dao.deleteProject(projectId)
        }
    }

    fun saveProject() {
        viewModelScope.launch {
            db.withTransaction {
                val pId = if (currentProjectId == 0L) {
                    dao.insertProject(ProjectEntity(name = _projectName.value))
                } else {
                    dao.insertProject(ProjectEntity(id = currentProjectId, name = _projectName.value))
                    currentProjectId
                }
                currentProjectId = pId

                dao.deleteStatesByProject(pId)
                dao.deleteTransitionsByProject(pId)

                if (_states.isNotEmpty()) {
                    dao.insertStates(_states.map { 
                        StateEntity(it.id, pId, it.name, it.position.x, it.position.y, it.isInitial, it.isFinal, it.message) 
                    })
                }
                if (_transitions.isNotEmpty()) {
                    dao.insertTransitions(_transitions.map { 
                        TransitionEntity(it.id, pId, it.fromStateId, it.toStateId, it.signal) 
                    })
                }
            }
        }
    }

    fun loadProject(fullProject: FullProject) {
        currentProjectId = fullProject.project.id
        _projectName.value = fullProject.project.name
        _states.clear()
        _states.addAll(fullProject.states.map { 
            StateNode(it.id, it.name, Offset(it.posX, it.posY), it.isInitial, it.isFinal, it.message) 
        })
        _transitions.clear()
        _transitions.addAll(fullProject.transitions.map { 
            Transition(it.id, it.fromStateId, it.toStateId, it.signal) 
        })
        
        // Ustalenie następnego numeru stanu na podstawie wczytanych danych
        val maxNum = _states.mapNotNull { 
            it.name.removePrefix("S").toIntOrNull() 
        }.maxOrNull() ?: -1
        nextStateNumber = maxNum + 1
        
        _activeStateId.value = _states.find { it.isInitial }?.id
        validateMachine()
    }

    fun validateMachine() {
        if (_states.isEmpty()) {
            _isValid.value = false
            _validationMessage.value = "Dodaj pierwszy stan"
            return
        }

        val initialState = _states.find { it.isInitial }
        val finalStates = _states.filter { it.isFinal }

        if (initialState == null) {
            _isValid.value = false
            _validationMessage.value = "Błąd: Brak stanu początkowego!"
            return
        }
        if (finalStates.isEmpty()) {
            _isValid.value = false
            _validationMessage.value = "Błąd: Brak stanów końcowych!"
            return
        }

        // Sprawdzenie determinizmu (brak 2 różnych wyjść dla tego samego sygnału)
        for (state in _states) {
            val outgoingTransitions = _transitions.filter { it.fromStateId == state.id }
            val signals = outgoingTransitions.map { it.signal }
            if (signals.size != signals.distinct().size) {
                _isValid.value = false
                val duplicateSignal = signals.groupBy { it }.filter { it.value.size > 1 }.keys.firstOrNull()
                _validationMessage.value = "Błąd: Stan ${state.name} ma duplikaty sygnału '$duplicateSignal'"
                return
            }
        }

        val reachableFromStart = mutableSetOf<String>()
        val forwardQueue = mutableListOf(initialState.id)
        reachableFromStart.add(initialState.id)

        while (forwardQueue.isNotEmpty()) {
            val curr = forwardQueue.removeAt(0)
            _transitions.filter { it.fromStateId == curr }.forEach { t ->
                if (t.toStateId !in reachableFromStart) {
                    reachableFromStart.add(t.toStateId)
                    forwardQueue.add(t.toStateId)
                }
            }
        }

        val unreachableStates = _states.filter { it.id !in reachableFromStart }
        if (unreachableStates.isNotEmpty()) {
            _isValid.value = false
            val names = unreachableStates.joinToString { it.name }
            _validationMessage.value = "Błąd: Nieosiągalne: $names"
            return
        }

        val canReachFinal = mutableSetOf<String>()
        val reverseQueue = mutableListOf<String>()
        finalStates.forEach { canReachFinal.add(it.id); reverseQueue.add(it.id) }

        while (reverseQueue.isNotEmpty()) {
            val currId = reverseQueue.removeAt(0)
            _transitions.filter { it.toStateId == currId }.forEach { t ->
                if (t.fromStateId !in canReachFinal) {
                    canReachFinal.add(t.fromStateId)
                    reverseQueue.add(t.fromStateId)
                }
            }
        }

        val deadEndStates = _states.filter { it.id !in canReachFinal }
        if (deadEndStates.isNotEmpty()) {
            _isValid.value = false
            val names = deadEndStates.joinToString { it.name }
            _validationMessage.value = "Błąd: $names nie prowadzą do końca!"
            return
        }

        _isValid.value = true
        _validationMessage.value = "Logika poprawna: graf spójny."
    }

    fun addState(position: Offset) {
        val name = "S$nextStateNumber"
        nextStateNumber++
        _states.add(StateNode(UUID.randomUUID().toString(), name, position, false, false))
        validateMachine()
    }

    fun updateStateMessage(id: String, msg: String) {
        val i = _states.indexOfFirst { it.id == id }
        if (i != -1) _states[i] = _states[i].copy(message = msg)
    }

    fun updateTransitionSignal(id: String, sig: String) {
        val i = _transitions.indexOfFirst { it.id == id }
        if (i != -1) _transitions[i] = _transitions[i].copy(signal = sig)
        validateMachine()
    }

    fun toggleInitial(id: String) {
        val currentState = _states.find { it.id == id }
        val becomesInitial = currentState?.isInitial == false
        for (i in _states.indices) {
            _states[i] = _states[i].copy(isInitial = _states[i].id == id && becomesInitial)
        }
        _activeStateId.value = _states.find { it.isInitial }?.id
        validateMachine()
    }

    fun toggleFinal(id: String) {
        val i = _states.indexOfFirst { it.id == id }
        if (i != -1) _states[i] = _states[i].copy(isFinal = !_states[i].isFinal)
        validateMachine()
    }

    fun updateStatePosition(id: String, pos: Offset) {
        val i = _states.indexOfFirst { it.id == id }
        if (i != -1) _states[i] = _states[i].copy(position = pos)
        _isMoveMode.value = false
    }

    fun deleteSelectedState() {
        _selectedState.value?.let { s ->
            _transitions.removeIf { it.fromStateId == s.id || it.toStateId == s.id }
            _states.remove(s)
            _selectedState.value = null
            validateMachine()
        }
    }

    fun deleteSelectedTransition() {
        _selectedTransition.value?.let { t -> 
            _transitions.remove(t)
            _selectedTransition.value = null
            validateMachine() 
        }
    }

    fun toggleMoveMode() { _isMoveMode.value = !_isMoveMode.value }
    fun toggleSimulation(act: Boolean) { _isSimulationActive.value = act; if (act) resetSimulation() }
    fun selectState(s: StateNode?) { _selectedState.value = s; _selectedTransition.value = null }
    fun selectTransition(t: Transition?) { _selectedTransition.value = t; _selectedState.value = null }
    
    fun resetSimulation() { 
        val initial = _states.find { it.isInitial }
        _activeStateId.value = initial?.id
        _history.clear() 
        if (initial?.message?.isNotEmpty() == true) {
            _history.add("💬 [${initial.name}]: ${initial.message}")
        }
    }

    fun processSignal(sig: String) {
        val currId = _activeStateId.value ?: return
        val transition = _transitions.find { it.fromStateId == currId && it.signal == sig }
        if (transition != null) {
            val fromState = _states.find { it.id == currId }
            val toState = _states.find { it.id == transition.toStateId }
            
            val fromName = fromState?.name ?: ""
            val toName = toState?.name ?: ""
            
            _history.add("$fromName --($sig)--> $toName")
            
            if (toState?.message?.isNotEmpty() == true) {
                _history.add("💬 [${toName}]: ${toState.message}")
            }
            
            _activeStateId.value = transition.toStateId
        }
    }

    fun toggleTransitionMode() {
        if (_transitionSourceId.value == null) {
            _transitionSourceId.value = _selectedState.value?.id
        } else {
            val fromId = _transitionSourceId.value!!
            val toId = _selectedState.value?.id
            if (toId != null) {
                val sig = _transitions.count { it.fromStateId == fromId }.toString()
                _transitions.add(Transition(UUID.randomUUID().toString(), fromId, toId, sig))
                validateMachine()
            }
            _transitionSourceId.value = null
        }
    }
}
