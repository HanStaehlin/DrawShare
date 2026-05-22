package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: DrawViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Sleek Interface: Light minimal design, pure black & white accents
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color.White,
                    surface = Color.White,
                    primary = Color.Black,
                    onBackground = Color.Black,
                    onSurface = Color.Black,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    DrawShareApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DrawShareApp(viewModel: DrawViewModel) {
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    // Smooth system time updater mimicking top header of mockups
    val systemTime = remember {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Sleek Status & Clock Bar (Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = systemTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp,
                    color = Color.Black
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isInternetAvailable && (connectionState == WebSocketConnectionState.CONNECTED)) 
                                    Color.Black 
                                else 
                                    Color(0xFFD4D4D8)
                            )
                    )
                    Text(
                        text = if (isInternetAvailable) "ONLINE" else "OFFLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        color = Color.Black
                    )
                }
            }

            // Minimalist decorative thin divider
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFF4F4F5)))

            // Screen Selection flow (Join screen or Active Session tabs)
            if (inviteCode == null) {
                ConnectionScreen(
                    viewModel = viewModel
                )
            } else {
                ActiveBoardScreen(
                    viewModel = viewModel,
                    inviteCode = inviteCode!!,
                    connectionState = connectionState
                )
            }
        }
    }
}

@Composable
fun ConnectionScreen(
    viewModel: DrawViewModel
) {
    var codeInput by remember { mutableStateOf("") }
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "YOUR PROFILE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextField(
            value = userName,
            onValueChange = { viewModel.setUserName(it) },
            placeholder = { Text("Enter your name") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF9F9FB),
                unfocusedContainerColor = Color(0xFFF9F9FB),
                focusedIndicatorColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 32.dp)
        )

        Text(
            text = "CONNECT TO A PARTNER",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Enter an invite code to join a shared drawing board, or create a brand new board to invite a partner.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF71717A),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Sleek Single digit input field
        TextField(
            value = codeInput,
            onValueChange = { if (it.length <= 6) codeInput = it.filter { c -> c.isDigit() } },
            placeholder = {
                Text(
                    text = "ENTER FIVE-DIGIT CODE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFFD4D4D8)
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Black,
                unfocusedIndicatorColor = Color(0xFFE4E4E7)
            ),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 32.dp)
                .testTag("invite_code_input")
        )

        // Connect button (Highly stylized, sleek modern rounded)
        Button(
            onClick = {
                if (codeInput.isNotEmpty()) {
                    viewModel.joinRoom(codeInput)
                }
            },
            enabled = codeInput.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFF4F4F5),
                disabledContentColor = Color(0xFFA1A1AA)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .testTag("connect_button")
        ) {
            Text(
                text = "CONNECT BOARD",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "— OR —",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFD4D4D8)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Create new drawing board underlines
        Text(
            text = "CREATE NEW DRAW BOARD",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.Black,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    viewModel.generateInviteCode()
                }
                .padding(12.dp)
                .testTag("create_board_button")
        )
    }
}

@Composable
fun ActiveBoardScreen(
    viewModel: DrawViewModel,
    inviteCode: String,
    connectionState: WebSocketConnectionState
) {
    // 3 Tabs matching the bottom navigation of design theme:
    // Tab 0 = History Log, Tab 1 = Active Drawing Canvas, Tab 2 = Group Room Info
    var activeTab by remember { mutableIntStateOf(1) }

    val roomMessages by viewModel.roomMessages.collectAsStateWithLifecycle()
    val selectedColor by viewModel.selectedColor.collectAsStateWithLifecycle()
    val selectedAlpha by viewModel.selectedAlpha.collectAsStateWithLifecycle()
    val selectedWidth by viewModel.selectedWidth.collectAsStateWithLifecycle()
    val debugLog by viewModel.debugLog.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        
        // ---------------- SLEEK TOP HEADER ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PARTNER BOARD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    letterSpacing = (-0.5).sp,
                    fontFamily = FontFamily.SansSerif
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    val (dotColor, statusText) = when (connectionState) {
                        WebSocketConnectionState.CONNECTED -> Color(0xFF22C55E) to "Connected"
                        WebSocketConnectionState.CONNECTING -> Color(0xFFEAB308) to "Connecting..."
                        WebSocketConnectionState.DISCONNECTED -> Color(0xFFEF4444) to "Disconnected"
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CODE: $inviteCode",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Horizontal partition line
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFF4F4F5)))
        Spacer(modifier = Modifier.height(8.dp))

        // ---------------- ACTIVE TAB BODY ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: HISTORICAL CHAT FEED
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SHARED DRAWINGS (${roomMessages.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )

                            if (roomMessages.isNotEmpty()) {
                                Text(
                                    text = "CLEAR ALL",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textDecoration = TextDecoration.Underline,
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .clickable { viewModel.clearRoomHistory() }
                                        .padding(vertical = 4.dp)
                                        .testTag("clear_history_button")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (roomMessages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your log is empty.\nSend cute drawings in the editor!",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFD4D4D8),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("history_list"),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(roomMessages, key = { it.id }) { message ->
                                    DrawingHistoryCard(message = message)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: DRAW CANVAS AND ACTIVE SLIDERS
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Rounded canvas box matching "rounded-3xl border border-zinc-100 bg-zinc-50"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFFF9F9FB))
                                .border(1.dp, Color(0xFFF4F4F5), RoundedCornerShape(32.dp))
                        ) {
                            DrawingCanvas(
                                viewModel = viewModel,
                                selectedColor = selectedColor,
                                selectedAlpha = selectedAlpha,
                                selectedWidth = selectedWidth
                            )

                            // Clean, absolute drawing hint overlay
                            if (viewModel.activeStrokes.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DRAW HERE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Light,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 4.sp,
                                        color = Color(0xFFD4D4D8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interaction controls (PX-6 PB-10 Space-y-8 alignment)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            
                            // Colors configuration with anim sliding trigger to avoid screen clutter
                            var showColors by remember { mutableStateOf(value = false) }
                            val colorsList = listOf(
                                0xFF000000.toInt(), 0xFF747474.toInt(), 0xFFB1B1B1.toInt(), 0xFFFFFFFF.toInt(),
                                0xFFFF6B6B.toInt(), 0xFFFA5252.toInt(), 0xFFC92A2A.toInt(),
                                0xFF339AF0.toInt(), 0xFF1C7ED6.toInt(), 0xFF1864AB.toInt(),
                                0xFF51CF66.toInt(), 0xFF37B24D.toInt(), 0xFF2B8A3E.toInt(),
                                0xFFFCC419.toInt(), 0xFFFAB005.toInt(), 0xFFE67700.toInt(),
                                0xFFFF922B.toInt(), 0xFFFD7E14.toInt(), 0xFFD9480F.toInt(),
                                0xFFB197FC.toInt(), 0xFF845EF7.toInt(), 0xFF5F3DC4.toInt(),
                                0xFFF06595.toInt(), 0xFFD6336C.toInt(), 0xFFA61E4D.toInt()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Color Tool Preview Widget
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .clickable { showColors = !showColors }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .padding(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(Color(selectedColor))
                                            )
                                        }
                                    }
                                    Text(
                                        text = "COLOR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }

                                AnimatedVisibility(
                                    visible = showColors,
                                    enter = slideInHorizontally { it } + fadeIn(),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                ) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(colorsList) { colorArgb ->
                                            val isSelected = selectedColor == colorArgb
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorArgb))
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = if (isSelected) Color.Black else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        viewModel.changeColor(colorArgb)
                                                        showColors = false
                                                    }
                                                    .testTag("color_$colorArgb")
                                            )
                                        }
                                    }
                                }

                                // Quick clear & undo text buttons in minimal style
                                if (!showColors) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(
                                            text = "UNDO",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .clickable { viewModel.undoLastStroke() }
                                                .testTag("undo_button")
                                        )
                                        Text(
                                            text = "RESET",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .clickable { viewModel.clearCanvas() }
                                                .testTag("clear_canvas_button")
                                        )
                                    }
                                }
                            }

                            // Sliders custom layout mimicking original slider parameters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OPACITY: ${(selectedAlpha * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black,
                                    modifier = Modifier.width(95.dp)
                                )

                                Slider(
                                    value = selectedAlpha,
                                    onValueChange = { viewModel.changeAlpha(it) },
                                    valueRange = 0.1f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Black,
                                        activeTrackColor = Color.Black,
                                        inactiveTrackColor = Color(0xFFF4F4F5)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("opacity_slider")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SIZE: ${selectedWidth.toInt()}PX",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black,
                                    modifier = Modifier.width(95.dp)
                                )

                                Slider(
                                    value = selectedWidth,
                                    onValueChange = { viewModel.changeWidth(it) },
                                    valueRange = 2f..32f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Black,
                                        activeTrackColor = Color.Black,
                                        inactiveTrackColor = Color(0xFFF4F4F5)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("size_slider")
                                )
                            }

                            // Action SEND Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (connectionState != WebSocketConnectionState.CONNECTED) {
                                    Text(
                                        text = if (connectionState == WebSocketConnectionState.CONNECTING) "Connecting..." else "Disconnected (Check network)",
                                        color = if (connectionState == WebSocketConnectionState.CONNECTING) Color(0xFFEAB308) else Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(start = 4.dp).weight(1f)
                                    )
                                } else if (viewModel.activeStrokes.isEmpty()) {
                                    Text(
                                        text = "Draw on canvas to send",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(start = 4.dp).weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                Button(
                                    onClick = { viewModel.sendCurrentDrawing() },
                                    enabled = viewModel.activeStrokes.isNotEmpty() && connectionState == WebSocketConnectionState.CONNECTED,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFF4F4F5),
                                        disabledContentColor = Color(0xFFA1A1AA)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .testTag("send_drawing_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send drawing",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SEND OUT",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: PARTNER / ROOM INFORMATION VIEW
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "ROOM DATA & SETTINGS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "INVITATION SHORTCODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inviteCode,
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "COPY",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(inviteCode))
                                                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(8.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Join my drawing board on DrawShare! Code: $inviteCode")
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, null)
                                            context.startActivity(shareIntent)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share code",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "CONNECTION STATISTICS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            val statusDescription = when (connectionState) {
                                WebSocketConnectionState.CONNECTED -> "Connected"
                                WebSocketConnectionState.CONNECTING -> "Connecting..."
                                WebSocketConnectionState.DISCONNECTED -> "Disconnected"
                            }
                            Text(
                                text = "Relay Network: PieSocket Pub-Sub Relay Server\n" +
                                       "Status: $statusDescription\n" +
                                       "Messages Exchanged: ${roomMessages.size}",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.DarkGray
                            )
                        }

                        // Debug Log Panel
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "DEBUG LOG",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF9F9FB))
                                    .border(1.dp, Color(0xFFF4F4F5), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                if (debugLog.isEmpty()) {
                                    Text(
                                        text = "No events yet...",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD4D4D8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        reverseLayout = true
                                    ) {
                                        items(debugLog.reversed()) { entry ->
                                            val textColor = when {
                                                entry.contains("\u2705") -> Color(0xFF22C55E)
                                                entry.contains("\u274C") -> Color(0xFFEF4444)
                                                entry.contains("FAILURE") -> Color(0xFFEF4444)
                                                else -> Color.DarkGray
                                            }
                                            Text(
                                                text = entry,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = textColor,
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Share App Button
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Download the DrawShare app and let's draw together!")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share DrawShare")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SHARE APP TO FRIEND",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // High contrast Danger Zone disconnect link
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black, RoundedCornerShape(24.dp))
                                .height(46.dp)
                                .testTag("leave_board_button")
                        ) {
                            Text(
                                text = "DISCONNECT CURRENT SESSION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------- SLEEK BOTTOM TAB BAR ----------------
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFF4F4F5)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: History Log Icon
            IconButton(
                onClick = { activeTab = 0 }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "History Log",
                    tint = if (activeTab == 0) Color.Black else Color(0xFFD4D4D8),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 1: Edit Draw Canvas Icon (Active Default)
            IconButton(
                onClick = { activeTab = 1 }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Canvas",
                    tint = if (activeTab == 1) Color.Black else Color(0xFFD4D4D8),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 2: Group Config Icon
            IconButton(
                onClick = { activeTab = 2 }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Group Connection",
                    tint = if (activeTab == 2) Color.Black else Color(0xFFD4D4D8),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DrawingHistoryCard(message: UIMessage) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()) }
    val timeString = formatter.format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        
        // Thumbnail card showing stored message drawing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF9F9FB))
                .border(1.dp, Color(0xFFF4F4F5), RoundedCornerShape(20.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (stroke in message.strokes) {
                    if (stroke.points.isNotEmpty()) {
                        val path = Path()
                        val first = stroke.points.first()
                        path.moveTo(first.x * size.width, first.y * size.height)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            path.lineTo(pt.x * size.width, pt.y * size.height)
                        }
                        drawPath(
                            path = path,
                            color = Color(stroke.colorArgb),
                            alpha = stroke.alpha,
                            style = Stroke(
                                width = stroke.width * (size.width / 400f).coerceAtLeast(1f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History logs details row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = if (message.isReceived) message.senderName.uppercase() else "YOU"
            val labelColor = if (message.isReceived) Color(0xFFFF6B6B) else Color.Black

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = labelColor
            )

            // Dynamic ACK Check status mapping
            val statusDisplay = if (message.isReceived) {
                "OK"
            } else {
                if (message.isConfirmedDelivered) "DELIVERED ✓" else "WAITING FOR PARTNER..."
            }
            val statusColor = if (message.isConfirmedDelivered || message.isReceived) Color.Black else Color.Gray

            Text(
                text = statusDisplay,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = statusColor
            )
        }

        Text(
            text = timeString,
            fontSize = 9.sp,
            color = Color.LightGray,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DrawingCanvas(
    viewModel: DrawViewModel,
    selectedColor: Int,
    selectedAlpha: Float,
    selectedWidth: Float
) {
    var rawWidth by remember { mutableIntStateOf(0) }
    var rawHeight by remember { mutableIntStateOf(0) }
    val currentPath = remember { mutableStateListOf<StrokePoint>() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedColor, selectedAlpha, selectedWidth, rawWidth, rawHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (rawWidth > 0 && rawHeight > 0) {
                            currentPath.clear()
                            currentPath.add(StrokePoint(offset.x / rawWidth, offset.y / rawHeight))
                        }
                    },
                    onDragEnd = {
                        if (currentPath.isNotEmpty()) {
                            viewModel.addStroke(
                                DrawStroke(
                                    points = currentPath.toList(),
                                    colorArgb = selectedColor,
                                    width = selectedWidth,
                                    alpha = selectedAlpha
                                )
                            )
                        }
                        currentPath.clear()
                    },
                    onDragCancel = {
                        currentPath.clear()
                    },
                    onDrag = { change, _ ->
                        val offset = change.position
                        if (rawWidth > 0 && rawHeight > 0) {
                            currentPath.add(StrokePoint(offset.x / rawWidth, offset.y / rawHeight))
                        }
                    }
                )
            }
    ) {
        rawWidth = size.width.toInt()
        rawHeight = size.height.toInt()

        // 1. Draw completed committed strokes
        for (stroke in viewModel.activeStrokes) {
            if (stroke.points.isNotEmpty()) {
                val path = Path()
                val first = stroke.points.first()
                path.moveTo(first.x * size.width, first.y * size.height)
                for (i in 1 until stroke.points.size) {
                    val pt = stroke.points[i]
                    path.lineTo(pt.x * size.width, pt.y * size.height)
                }
                drawPath(
                    path = path,
                    color = Color(stroke.colorArgb),
                    alpha = stroke.alpha,
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // 2. Draw current active in-progress stroke
        if (currentPath.isNotEmpty()) {
            val path = Path()
            val first = currentPath.first()
            path.moveTo(first.x * size.width, first.y * size.height)
            for (i in 1 until currentPath.size) {
                val pt = currentPath[i]
                path.lineTo(pt.x * size.width, pt.y * size.height)
            }
            drawPath(
                path = path,
                color = Color(selectedColor),
                alpha = selectedAlpha,
                style = Stroke(
                    width = selectedWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

