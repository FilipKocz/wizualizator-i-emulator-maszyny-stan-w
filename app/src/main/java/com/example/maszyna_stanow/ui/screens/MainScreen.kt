package com.example.maszyna_stanow.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maszyna_stanow.model.Transition
import com.example.maszyna_stanow.ui.components.StateNodeView
import com.example.maszyna_stanow.ui.viewmodel.MainViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val selectedState by viewModel.selectedState
    val selectedTransition by viewModel.selectedTransition
    val isMoveMode by viewModel.isMoveMode
    val transitionSourceId by viewModel.transitionSourceId
    val activeStateId by viewModel.activeStateId

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Emulator Maszyny Stanów") })
                // Panel Symulacji (Etap 3)
                activeStateId?.let { currentId ->
                    val signals = viewModel.transitions.filter { it.fromStateId == currentId }
                    if (signals.isNotEmpty()) {
                        ScrollableTabRow(selectedTabIndex = 0, edgePadding = 16.dp, containerColor = Color.LightGray.copy(alpha = 0.2f)) {
                            signals.forEach { transition ->
                                Button(
                                    onClick = { viewModel.processSignal(transition.signal) },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(transition.signal)
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Przycisk dodawania stanów jest teraz zawsze widoczny (chyba że trwa łączenie)
            if (transitionSourceId == null) {
                FloatingActionButton(onClick = {
                    // Dodajemy nowy stan w dynamicznym miejscu
                    viewModel.addState("S${viewModel.states.size}", Offset(200f + (viewModel.states.size % 3) * 100f, 200f + (viewModel.states.size / 3) * 100f))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj stan")
                }
            }
        },
        bottomBar = {
            if (selectedState != null || selectedTransition != null) {
                BottomAppBar {
                    if (selectedState != null) {
                        IconButton(onClick = { viewModel.toggleInitial(selectedState!!.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = if (selectedState!!.isInitial) Color.Blue else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleFinal(selectedState!!.id) }) {
                            Icon(Icons.Default.Star, contentDescription = "Koniec", tint = if (selectedState!!.isFinal) Color.Red else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleMoveMode() }) {
                            Icon(Icons.Default.Build, contentDescription = "Przesuń", tint = if (isMoveMode) Color.Green else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleTransitionMode() }) {
                            Icon(Icons.Default.Share, contentDescription = "Połącz", tint = if (transitionSourceId != null) Color.Green else Color.Gray)
                        }
                    } else if (selectedTransition != null) {
                        // Edytor Sygnału (Etap 3)
                        var signalText by remember(selectedTransition!!.id) { mutableStateOf(selectedTransition!!.signal) }
                        TextField(
                            value = signalText,
                            onValueChange = { signalText = it },
                            label = { Text("Sygnał") },
                            modifier = Modifier.width(150.dp).padding(4.dp)
                        )
                        IconButton(onClick = { viewModel.updateTransitionSignal(selectedTransition!!.id, signalText) }) {
                            Icon(Icons.Default.Check, contentDescription = "Zapisz", tint = Color.Green)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { 
                        if (selectedState != null) viewModel.deleteSelectedState()
                        else viewModel.deleteSelectedTransition()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { offset ->
                        if (isMoveMode && selectedState != null) {
                            viewModel.updateStatePosition(selectedState!!.id, offset - Offset(110f, 110f))
                        } else {
                            val clickedTransition = findClickedTransition(viewModel, offset)
                            if (clickedTransition != null) {
                                viewModel.selectTransition(clickedTransition)
                            } else {
                                viewModel.selectState(null)
                            }
                        }
                    })
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                viewModel.transitions.forEach { transition ->
                    val fromState = viewModel.states.find { it.id == transition.fromStateId }
                    val toState = viewModel.states.find { it.id == transition.toStateId }
                    
                    if (fromState != null && toState != null) {
                        val start = fromState.position + Offset(40.dp.toPx(), 40.dp.toPx())
                        val end = toState.position + Offset(40.dp.toPx(), 40.dp.toPx())
                        val isSelected = selectedTransition?.id == transition.id
                        
                        drawLine(
                            color = if (isSelected) Color.Blue else Color.Black,
                            start = start, end = end,
                            strokeWidth = if (isSelected) 8f else 4f,
                            cap = StrokeCap.Round
                        )
                        drawArrowHead(start, end, color = if (isSelected) Color.Blue else Color.Black)
                        



                        drawContext.canvas.nativeCanvas.drawText(
                            transition.signal,
                            (start.x + end.x) / 2,
                            (start.y + end.y) / 2 - 20f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 40f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            viewModel.states.forEach { state ->
                StateNodeView(
                    stateNode = state,
                    isSelected = selectedState?.id == state.id,
                    isMoveMode = isMoveMode || (transitionSourceId != null && selectedState?.id == state.id),
                    isActive = activeStateId == state.id,
                    onClick = { viewModel.selectState(state) }
                )
            }
            
            if (isMoveMode) StatusInfo("Tryb przesuwania: Kliknij w miejsce docelowe...")
            else if (transitionSourceId != null) StatusInfo("Tryb łączenia: Zaznacz drugi stan i kliknij ikonę połączenia.")
        }
    }
}

private fun findClickedTransition(viewModel: MainViewModel, clickOffset: Offset): Transition? {
    val threshold = 30f
    return viewModel.transitions.find { transition ->
        val from = viewModel.states.find { it.id == transition.fromStateId }?.let { it.position + Offset(110f, 110f) }
        val to = viewModel.states.find { it.id == transition.toStateId }?.let { it.position + Offset(110f, 110f) }
        if (from != null && to != null) distanceFromPointToLine(clickOffset, from, to) < threshold else false
    }
}

private fun distanceFromPointToLine(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (b.x - a.x).pow(2) + (b.y - a.y).pow(2)
    if (l2 == 0f) return sqrt((p.x - a.x).pow(2) + (p.y - a.y).pow(2))
    var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
    t = max(0f, min(1f, t))
    return sqrt((p.x - (a.x + t * (b.x - a.x))).pow(2) + (p.y - (a.y + t * (b.y - a.y))).pow(2))
}

fun DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowLength = 25f
    val arrowAngle = Math.toRadians(30.0)
    val radius = 42.dp.toPx()
    val arrowTip = Offset(end.x - radius * cos(angle), end.y - radius * sin(angle))
    val path = Path().apply {
        moveTo(arrowTip.x, arrowTip.y)
        lineTo(arrowTip.x - arrowLength * cos(angle - arrowAngle).toFloat(), arrowTip.y - arrowLength * sin(angle - arrowAngle).toFloat())
        moveTo(arrowTip.x, arrowTip.y)
        lineTo(arrowTip.x - arrowLength * cos(angle + arrowAngle).toFloat(), arrowTip.y - arrowLength * sin(angle + arrowAngle).toFloat())
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
}

@Composable
fun StatusInfo(text: String) {
    Surface(color = Color.Green.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
        Text(text = text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
