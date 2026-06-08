package com.example.maszyna_stanow.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    viewModel.addState(Offset(200f, 300f))
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
                        
                        drawTransition(transition, start, end, isSelected)
                    }
                }
            }

            viewModel.states.forEach { state ->
                StateNodeView(
                    stateNode = state,
                    isSelected = selectedState?.id == state.id,
                    isMoveMode = isMoveMode,
                    isActive = activeStateId == state.id,
                    onClick = { viewModel.selectState(state) }
                )
            }
        }
    }
}

@Composable
fun MiniMapView(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val states = viewModel.states
    val transitions = viewModel.transitions
    val activeStateId by viewModel.activeStateId

    if (states.isEmpty()) return

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val stateRadius = 40.dp.toPx()
            
            val minX = states.minOf { it.position.x } - stateRadius
            val minY = states.minOf { it.position.y } - stateRadius
            val maxX = states.maxOf { it.position.x } + stateRadius * 2
            val maxY = states.maxOf { it.position.y } + stateRadius * 2

            val contentWidth = maxX - minX
            val contentHeight = maxY - minY
            
            val scaleX = size.width / contentWidth
            val scaleY = size.height / contentHeight
            val scale = min(scaleX, scaleY).coerceAtMost(1f)

            val centeringOffsetX = (size.width - contentWidth * scale) / 2
            val centeringOffsetY = (size.height - contentHeight * scale) / 2

            withTransform({
                translate(centeringOffsetX - minX * scale, centeringOffsetY - minY * scale)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                transitions.forEach { transition ->
                    val fromState = states.find { it.id == transition.fromStateId }
                    val toState = states.find { it.id == transition.toStateId }
                    if (fromState != null && toState != null) {
                        val start = fromState.position + Offset(stateRadius, stateRadius)
                        val end = toState.position + Offset(stateRadius, stateRadius)
                        drawTransition(transition, start, end, isSelected = false)
                    }
                }

                states.forEach { state ->
                    val isActive = state.id == activeStateId
                    val center = state.position + Offset(stateRadius, stateRadius)
                    
                    val backgroundColor = when {
                        state.isInitial -> Color(0xFFBBDEFB)
                        state.isFinal -> Color(0xFFFFCCBC)
                        else -> Color.White
                    }

                    drawCircle(
                        color = backgroundColor,
                        radius = stateRadius,
                        center = center
                    )
                    
                    drawCircle(
                        color = if (isActive) Color(0xFFFFD700) else Color.Black,
                        radius = stateRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isActive) 6.dp.toPx() else 2.dp.toPx())
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        state.name,
                        center.x,
                        center.y + 10f,
                        android.graphics.Paint().apply {
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = isActive
                            color = android.graphics.Color.BLACK
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SimulationView(viewModel: MainViewModel, innerPadding: PaddingValues) {
    val activeStateId by viewModel.activeStateId
    val history = viewModel.history
    val activeState = viewModel.states.find { it.id == activeStateId }

    Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
        Text("Mini Mapa:", style = MaterialTheme.typography.titleSmall)
        MiniMapView(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aktywny stan:", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = activeState?.name ?: "Brak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (activeState?.message?.isNotEmpty() == true) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = activeState.message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                if (activeState?.isFinal == true) {
                    Spacer(Modifier.height(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Stan końcowy") },
                        icon = { Icon(Icons.Default.Check, null) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Dostępne sygnały:", style = MaterialTheme.typography.titleMedium)
        
        val availableTransitions = viewModel.transitions.filter { it.fromStateId == activeStateId }
        
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTransitions.forEach { transition ->
                Button(onClick = { viewModel.processSignal(transition.signal) }) {
                    Text(transition.signal)
                }
            }
            if (availableTransitions.isEmpty()) {
                Text("Brak wychodzących przejść", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Historia i Wiadomości:", style = MaterialTheme.typography.titleMedium)
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(history.reversed()) { entry ->
                val isMessage = entry.startsWith("💬")
                Text(
                    text = entry,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = if (isMessage) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    fontWeight = if (isMessage) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        
        Button(
            onClick = { viewModel.resetSimulation() },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Resetuj symulację")
        }
    }
}

private fun DrawScope.drawTransition(transition: com.example.maszyna_stanow.model.Transition, start: Offset, end: Offset, isSelected: Boolean) {
    val color = if (isSelected) Color.Blue else Color.Black
    val strokeWidth = if (isSelected) 4.dp.toPx() else 2.dp.toPx()
    
    if (start == end) {
        val rectSize = 60.dp.toPx()
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = start - Offset(rectSize / 2, rectSize),
            size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        drawArrowHead(color, start - Offset(0f, -2.dp.toPx()), -10f)
        
        drawContext.canvas.nativeCanvas.drawText(
            transition.signal,
            start.x,
            start.y - rectSize - 5f,
            android.graphics.Paint().apply {
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = isSelected
            }
        )
    } else {
        val angle = atan2(end.y - start.y, end.x - start.x)
        val stateRadius = 40.dp.toPx()
        
        val actualStart = start + Offset(cos(angle) * stateRadius, sin(angle) * stateRadius)
        val actualEnd = end - Offset(cos(angle) * stateRadius, sin(angle) * stateRadius)

        drawLine(
            color = color,
            start = actualStart,
            end = actualEnd,
            strokeWidth = strokeWidth
        )
        
        drawArrowHead(color, actualEnd, Math.toDegrees(angle.toDouble()).toFloat())
        
        val midX = (actualStart.x + actualEnd.x) / 2
        val midY = (actualStart.y + actualEnd.y) / 2
        
        drawContext.canvas.nativeCanvas.drawText(
            transition.signal,
            midX,
            midY - 10f,
            android.graphics.Paint().apply {
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = isSelected
            }
        )
    }
}

private fun DrawScope.drawArrowHead(color: Color, tip: Offset, angleDegrees: Float) {
    val arrowSize = 15.dp.toPx()
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    
    val p1 = tip - Offset(
        (arrowSize * cos(angleRad - PI / 6)).toFloat(),
        (arrowSize * sin(angleRad - PI / 6)).toFloat()
    )
    val p2 = tip - Offset(
        (arrowSize * cos(angleRad + PI / 6)).toFloat(),
        (arrowSize * sin(angleRad + PI / 6)).toFloat()
    )
    
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color)
}

private fun findClickedTransition(viewModel: MainViewModel, click: Offset): com.example.maszyna_stanow.model.Transition? {
    for (t in viewModel.transitions) {
        val fromState = viewModel.states.find { it.id == t.fromStateId }
        val toState = viewModel.states.find { it.id == t.toStateId }
        if (fromState != null && toState != null) {
            val start = fromState.position + Offset(110f, 110f)
            val end = toState.position + Offset(110f, 110f)
            
            if (start == end) {
                val dist = sqrt((click.x - start.x).pow(2) + (click.y - (start.y - 60f)).pow(2))
                if (abs(dist - 60f) < 20f) return t
            } else {
                val d = distanceToSegment(click, start, end)
                if (d < 30f) return t
            }
        }
    }
    return null
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (a.x - b.x).pow(2) + (a.y - b.y).pow(2)
    if (l2 == 0f) return sqrt((p.x - a.x).pow(2) + (p.y - a.y).pow(2))
    var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
    t = max(0f, min(1f, t))
    return sqrt((p.x - (a.x + t * (b.x - a.x))).pow(2) + (p.y - (a.y + t * (b.y - a.y))).pow(2))
}
