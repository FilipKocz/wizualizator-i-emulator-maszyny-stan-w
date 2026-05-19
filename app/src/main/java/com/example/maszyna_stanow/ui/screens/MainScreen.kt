package com.example.maszyna_stanow.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    val isSimulationActive by viewModel.isSimulationActive
    val projectName by viewModel.projectName
    
    val allProjects by viewModel.allProjects.collectAsState()
    var showProjectList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSimulationActive) {
                        Text(projectName)
                    } else {
                        TextField(
                            value = projectName,
                            onValueChange = { viewModel.updateProjectName(it) },
                            placeholder = { Text("Nazwa projektu...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                actions = {
                    if (!isSimulationActive) {
                        IconButton(onClick = { viewModel.createNewProject() }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Nowy")
                        }
                        IconButton(onClick = { viewModel.saveProject() }) {
                            Icon(Icons.Default.Check, contentDescription = "Zapisz", tint = Color(0xFF2E7D32))
                        }
                        IconButton(onClick = { showProjectList = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Lista")
                        }
                    }
                    TextButton(onClick = { viewModel.toggleSimulation(!isSimulationActive) }) {
                        Icon(if (isSimulationActive) Icons.Default.Edit else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isSimulationActive) "EDYCJA" else "SYMULACJA")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSimulationActive && transitionSourceId == null) {
                FloatingActionButton(onClick = {
                    viewModel.addState("S${viewModel.states.size}", Offset(200f, 300f))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj stan")
                }
            }
        },
        bottomBar = {
            if (!isSimulationActive && (selectedState != null || selectedTransition != null)) {
                BottomAppBar {
                    if (selectedState != null) {
                        var messageText by remember(selectedState!!.id) { mutableStateOf(selectedState!!.message) }
                        TextField(
                            value = messageText,
                            onValueChange = { 
                                messageText = it
                                viewModel.updateStateMessage(selectedState!!.id, it)
                            },
                            label = { Text("Wiadomość") },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { viewModel.toggleInitial(selectedState!!.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = if (selectedState!!.isInitial) Color.Blue else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleFinal(selectedState!!.id) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Koniec", tint = if (selectedState!!.isFinal) Color.Red else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleMoveMode() }) {
                            Icon(Icons.Default.Build, contentDescription = "Przesuń", tint = if (isMoveMode) Color.Green else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleTransitionMode() }) {
                            Icon(Icons.Default.Share, contentDescription = "Połącz", tint = if (transitionSourceId != null) Color.Green else Color.Gray)
                        }
                    } else if (selectedTransition != null) {
                        var signalText by remember(selectedTransition!!.id) { mutableStateOf(selectedTransition!!.signal) }
                        TextField(
                            value = signalText,
                            onValueChange = { signalText = it },
                            label = { Text("Sygnał") },
                            modifier = Modifier.width(120.dp).padding(4.dp)
                        )
                        IconButton(onClick = { viewModel.updateTransitionSignal(selectedTransition!!.id, signalText) }) {
                            Icon(Icons.Default.Check, contentDescription = "Zapisz", tint = Color.Green)
                        }
                    }
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
        if (isSimulationActive) {
            SimulationView(viewModel, innerPadding)
        } else {
            EditorView(viewModel, innerPadding)
        }

        if (showProjectList) {
            AlertDialog(
                onDismissRequest = { showProjectList = false },
                title = { Text("Zapisane Projekty") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(allProjects) { fullProject ->
                            ListItem(
                                headlineContent = { Text(fullProject.project.name) },
                                modifier = Modifier.clickable {
                                    viewModel.loadProject(fullProject)
                                    showProjectList = false
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.deleteProject(fullProject.project.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Red)
                                    }
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProjectList = false }) { Text("Zamknij") }
                }
            )
        }
    }
}

@Composable
fun EditorView(viewModel: MainViewModel, innerPadding: PaddingValues) {
    val selectedState by viewModel.selectedState
    val selectedTransition by viewModel.selectedTransition
    val isMoveMode by viewModel.isMoveMode
    val transitionSourceId by viewModel.transitionSourceId
    val activeStateId by viewModel.activeStateId
    val isValid by viewModel.isValid
    val validationMessage by viewModel.validationMessage

    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        Surface(
            color = if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            modifier = Modifier.fillMaxWidth().border(1.dp, if (isValid) Color.Green else Color.Red)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isValid) Color(0xFF2E7D32) else Color.Red
                )
                Spacer(Modifier.width(8.dp))
                Text(text = validationMessage, fontWeight = FontWeight.Bold, color = if (isValid) Color(0xFF2E7D32) else Color.Red)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp).pointerInput(Unit) {
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
                            transition.signal, (start.x + end.x) / 2, (start.y + end.y) / 2 - 20f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 35f
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
            
            if (isMoveMode) StatusInfo("Kliknij w miejsce docelowe...")
            else if (transitionSourceId != null) StatusInfo("Zaznacz drugi stan aby połączyć.")
        }
    }
}

@Composable
fun SimulationView(viewModel: MainViewModel, innerPadding: PaddingValues) {
    val activeStateId by viewModel.activeStateId
    val currentState = viewModel.states.find { it.id == activeStateId }
    val availableTransitions = viewModel.transitions.filter { it.fromStateId == activeStateId }
    val history = viewModel.history
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        Box(
            modifier = Modifier.padding(16.dp).width(220.dp).height(200.dp).align(Alignment.TopStart)
                .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("HISTORIA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                LazyColumn(state = listState) {
                    items(history) { entry -> Text(text = entry, fontSize = 10.sp) }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = currentState?.name ?: "Błąd", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Text(
                    text = currentState?.message?.ifEmpty { "Brak wiadomości." } ?: "Brak danych.",
                    modifier = Modifier.padding(32.dp),
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(32.dp))
            availableTransitions.forEach { transition ->
                Button(
                    onClick = { viewModel.processSignal(transition.signal) },
                    modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp)
                ) {
                    Text(text = transition.signal, fontSize = 18.sp)
                }
            }
            if (availableTransitions.isEmpty() && currentState?.isFinal == true) {
                Text("🏁 OSIĄGNIĘTO KONIEC", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }
        }

        Box(
            modifier = Modifier.padding(16.dp).size(150.dp).align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.9f))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val scale = 0.12f
                viewModel.transitions.forEach { t ->
                    val from = viewModel.states.find { it.id == t.fromStateId }?.position ?: Offset.Zero
                    val to = viewModel.states.find { it.id == t.toStateId }?.position ?: Offset.Zero
                    drawLine(Color.LightGray, from * scale + Offset(15f, 15f), to * scale + Offset(15f, 15f), strokeWidth = 2f)
                }
                viewModel.states.forEach { s ->
                    val isCurrent = s.id == activeStateId
                    drawCircle(
                        color = if (isCurrent) Color(0xFFFFD700) else Color.LightGray,
                        radius = if (isCurrent) 10f else 6f,
                        center = s.position * scale + Offset(15f, 15f)
                    )
                }
            }
        }
        
        Button(
            onClick = { viewModel.resetSimulation() }, 
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("RESET")
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
