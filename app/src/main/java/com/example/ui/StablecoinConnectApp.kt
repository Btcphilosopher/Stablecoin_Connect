package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blockchain.SimBlock
import com.example.database.EventLogEntity
import com.example.database.LicenseEntity
import com.example.database.PaymentEntity
import com.example.database.WalletEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StablecoinConnectApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val events by viewModel.filteredEvents.collectAsStateWithLifecycle()
    val activeCheckoutPayment by viewModel.activeCheckoutPayment.collectAsStateWithLifecycle()
    val checkoutStep by viewModel.checkoutStep.collectAsStateWithLifecycle()
    val checkoutError by viewModel.checkoutError.collectAsStateWithLifecycle()
    val selectedWalletAddress by viewModel.selectedWalletAddress.collectAsStateWithLifecycle()

    var showCreateWalletDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "S",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Stablecoin CONNECT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Enterprise USDT Gateway",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerBlockMinedForAllNetworks() },
                        modifier = Modifier.testTag("mine_blocks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simulate Block Mining",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "TEST NODE",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavigationItem(AppScreen.DASHBOARD, "Merchant", Icons.Default.Dashboard, Icons.Outlined.Dashboard, "merchant_tab"),
                    NavigationItem(AppScreen.WALLETS, "Wallets", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "wallets_tab"),
                    NavigationItem(AppScreen.SETTLEMENT_ESCROW, "Settlements", Icons.Default.Handshake, Icons.Outlined.Handshake, "settlement_tab"),
                    NavigationItem(AppScreen.BROWSER_COMMERCE, "Sovereign Buy", Icons.Default.Language, Icons.Outlined.Language, "commerce_tab"),
                    NavigationItem(AppScreen.BLOCK_EXPLORER, "Blocks", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong, "explorer_tab"),
                    NavigationItem(AppScreen.EVENT_LOGS, "Auditing", Icons.Default.HistoryEdu, Icons.Outlined.HistoryEdu, "auditing_tab"),
                    NavigationItem(AppScreen.COMPLIANCE_SECURITY, "Compliance", Icons.Default.Security, Icons.Outlined.Security, "security_tab")
                )

                items.forEach { item ->
                    val selected = currentScreen == item.screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.setScreen(item.screen) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "ScreenSwitchAnimation"
            ) { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> DashboardScreen(viewModel, payments, wallets) { showCreateWalletDialog = true }
                    AppScreen.WALLETS -> WalletsScreen(viewModel, wallets, selectedWalletAddress) { showCreateWalletDialog = true }
                    AppScreen.SETTLEMENT_ESCROW -> SettlementEscrowScreen(viewModel, payments)
                    AppScreen.BROWSER_COMMERCE -> BrowserCommerceScreen(viewModel)
                    AppScreen.BLOCK_EXPLORER -> BlockExplorerScreen(viewModel)
                    AppScreen.EVENT_LOGS -> EventLogsScreen(viewModel, events)
                    AppScreen.COMPLIANCE_SECURITY -> ComplianceSecurityScreen(viewModel)
                }
            }

            // Checkout Overlay Bottom Sheet/Dialog simulation
            if (activeCheckoutPayment != null) {
                CheckoutGatewayWidget(
                    payment = activeCheckoutPayment!!,
                    step = checkoutStep,
                    error = checkoutError,
                    viewModel = viewModel
                )
            }

            // Create wallet dialog
            if (showCreateWalletDialog) {
                CreateWalletDialog(
                    onDismiss = { showCreateWalletDialog = false },
                    onConfirm = { label, type, network ->
                        viewModel.generateMerchantWallet(label, type, network)
                        showCreateWalletDialog = false
                    }
                )
            }
        }
    }
}

private data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    payments: List<PaymentEntity>,
    wallets: List<WalletEntity>,
    onCreateWalletClicked: () -> Unit
) {
    // Total statistics
    val totalRevenue = remember(wallets) {
        wallets.filter { it.type == "BUSINESS" || it.type == "TREASURY" }.sumOf { it.balance }
    }
    val totalCustomerLocked = remember(wallets) {
        wallets.filter { it.type == "CUSTOMER" }.sumOf { it.balance }
    }
    val totalEscrowLocked = remember(wallets) {
        wallets.filter { it.type == "ESCROW" }.sumOf { it.balance }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "STABLECOIN CONNECT",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Operational Overview",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W900
            )
            Text(
                text = "Real-time USDT settlement & gas metrics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Large Master Balance (Slick Slate-900 / High contrast layout)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Beautiful radial glow imitation from HTML background element
                            drawCircle(
                                color = Color(0xFF00A389).copy(alpha = 0.12f),
                                radius = size.minDimension * 0.55f,
                                center = Offset(size.width * 0.95f, size.height * 0.05f)
                            )
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Premium slate dark card matching bg-slate-900 HTML
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL SURPLUS REVENUE (USDT)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(50.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "SECURED (AES-256)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Bold typography decimal split layout
                        val formattedFull = String.format(Locale.US, "%,.2f", totalRevenue)
                        val dotIndex = formattedFull.indexOf('.')
                        val intPart = if (dotIndex != -1) formattedFull.substring(0, dotIndex) else formattedFull
                        val decPart = if (dotIndex != -1) formattedFull.substring(dotIndex) else ""

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = intPart,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.W900,
                                    color = Color.White
                                )
                            )
                            if (decPart.isNotEmpty()) {
                                Text(
                                    text = decPart,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.W800,
                                        color = Color(0xFF00A389)
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Custom Interactive Native Sparkline Revenue Chart
                        Text(
                            "WEEKLY SETTLEMENT TREND",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RevenueSparklineCanvas()
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Pending clearance & today increment layout from HTML design
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PENDING CLEARANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", totalEscrowLocked)} USDT",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "+12.4% today",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Escrow Pool", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.1f", totalEscrowLocked)} K",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Customer Reserves", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.1f", totalCustomerLocked)} K",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action panel quick indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Connect New Node", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("BIP-44 multi-chain asset derivation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = onCreateWalletClicked,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("onboard_node_button")
                    ) {
                        Text("Derive Key")
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "Recent Transactions Gateway Incoming",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Payments list
        if (payments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No settlement logs present. Run Sandbox payments to populate ledger.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(payments.take(5)) { pay ->
                PaymentRowItem(pay, viewModel)
            }
        }
    }
}

@Composable
fun RevenueSparklineCanvas() {
    val points = listOf(50000.0, 58000.0, 49000.0, 68000.0, 74000.0, 89000.0, 125000.0)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVal = points.maxOrNull() ?: 1.0
        val minVal = points.minOrNull() ?: 0.0
        val diff = maxVal - minVal

        val path = Path()
        val fillPath = Path()

        val itemWidth = width / (points.size - 1)

        points.forEachIndexed { index, value ->
            val ratioPrev = (value - minVal) / (if (diff == 0.0) 1.0 else diff)
            val x = index * itemWidth
            val y = height - (ratioPrev * height * 0.8f).toFloat() - 5f

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw translucent green fill grad
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00A389).copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw spark line
        drawPath(
            path = path,
            color = Color(0xFF00A389),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun PaymentRowItem(pay: PaymentEntity, viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Detail display if required */ },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = when (pay.status) {
                        "CONFIRMED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        "SETTLED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        "PENDING" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                pay.isEscrow -> Icons.Default.Handshake
                                pay.status == "PENDING" -> Icons.Default.Timer
                                pay.status == "CONFIRMED" || pay.status == "SETTLED" -> Icons.Default.CheckCircle
                                else -> Icons.Default.ErrorOutline
                            },
                            contentDescription = "Status Icon",
                            tint = when (pay.status) {
                                "CONFIRMED", "SETTLED" -> MaterialTheme.colorScheme.primary
                                "PENDING" -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = pay.merchantLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pay.network,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pay.id,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${pay.amount} USDT",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = pay.status,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = when (pay.status) {
                            "CONFIRMED", "SETTLED" -> MaterialTheme.colorScheme.primary
                            "PENDING" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun WalletsScreen(
    viewModel: MainViewModel,
    wallets: List<WalletEntity>,
    selectedWalletAddress: String?,
    onCreateWalletClicked: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val currentWallet = remember(selectedWalletAddress, wallets) {
        wallets.find { it.address == selectedWalletAddress }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BIP-44 DERIVED KEYS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Operational Nodes",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.W900
                )
                Text(
                    text = "Corporate digital core routing nodes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(
                onClick = onCreateWalletClicked,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .testTag("fab_create_wallet")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Node", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (wallets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // horizontal key picker
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selected Address Spec Card
            currentWallet?.let { wallet ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "  ${wallet.type}  ",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = wallet.network,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = wallet.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "ROUTING ADDRESS (BIP-44)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = wallet.address,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy address",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { clipboard.setText(AnnotatedString(wallet.address)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "LIQUID USDT BALANCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "${wallet.balance} USDT",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "AML COMPLIANCE SHIELD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Shield Verified",
                                            tint = if (wallet.amlRiskScore < 0.1) Color(0xFF00A389) else Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${(wallet.amlRiskScore * 100).toInt()}% RISK SCORE",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "SECURE HARNESS KEYS:\nPublic key: ${wallet.publicKey.take(16)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = "ENCRYPTED VAULT:\nPath: m/44'/60'/0'/0/0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Operational Network Wallets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(wallets) { wallet ->
                val isSelected = wallet.address == selectedWalletAddress
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setSelectedWallet(wallet.address) }
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (wallet.type) {
                                            "BUSINESS" -> Icons.Default.Domain
                                            "CUSTOMER" -> Icons.Default.Person
                                            "ESCROW" -> Icons.Default.Handshake
                                            else -> Icons.Default.Security
                                        },
                                        contentDescription = "Type Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = wallet.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${wallet.address.take(12)}...${wallet.address.takeLast(6)} (${wallet.network})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                        Text(
                            text = "${wallet.balance} USDT",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementEscrowScreen(viewModel: MainViewModel, payments: List<PaymentEntity>) {
    val escrowPayments = remember(payments) {
        payments.filter { it.isEscrow }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "USDT ESCROWS & SPLITS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Escrow Vaults",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W900
            )
            Text(
                text = "Arbitrated smart deposit execution pool",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Splits and Royalty explanation infographic
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Dynamic Revenue Splitting (Split Payouts)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Stablecoin CONNECT intercepts incoming blockchain orders and distributes revenue automatically to affiliates, partner accounts, and platform commissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple graphical splitter breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Merchant", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = "Merchant Split", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Text("90%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = "splits", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Partner", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(32.dp)
                                    .background(Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Handshake, contentDescription = "Partner Split", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Text("8%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = "splits", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Gateway", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(32.dp)
                                    .background(Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DoubleArrow, contentDescription = "Gateway Commis", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Text("2%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Active Escrow Vault Orders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (escrowPayments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No escrows active. Process an Escrow payment in browser marketplace simulator.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(escrowPayments) { esc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = esc.merchantLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = esc.network,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${esc.amount} USDT",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = esc.escrowStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = when (esc.escrowStatus) {
                                            "HELD" -> Color(0xFFF59E0B)
                                            "RELEASED" -> MaterialTheme.colorScheme.primary
                                            else -> Color(0xFFEF4444)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sender address (Corporate Buyer): ${esc.senderAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Escrow address (Secure contract pool): ${esc.recipientAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        if (esc.escrowStatus == "HELD") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.releaseMerchantEscrow(esc.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("release_escrow_button_${esc.id}")
                                ) {
                                    Text("Release Funds")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.refundMerchantEscrow(esc.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("refund_escrow_button_${esc.id}")
                                ) {
                                    Text("Refund")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserCommerceScreen(viewModel: MainViewModel) {
    var tabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Browser interface mockup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // browser balls
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFF59E0B), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Address Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "https://sovereign.marketplace.connect/web3",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 1
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SANDBOX COMMERCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Marketplace",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.W900
                )
                Text(
                    text = "Test Stripe-for-Stablecoin USDT checkout integrations natively inside browser environment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
                        Text("Standard Assets", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
                        Text("B2B Escrow Services", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (tabIndex == 0) {
                    // Standard assets list
                    val standardAssets = listOf(
                        MarketplaceAsset("Extension Key", 15.0, "Secure IP blocker & cookie proxy extensions for sovereign nodes."),
                        MarketplaceAsset("AI LLM Model Pipeline", 120.0, "Pre-wrapped container model. Encrypted inference pipeline."),
                        MarketplaceAsset("SaaS Cloud Sync License", 1250.0, "Corporate secure cloud cloud database proxy sync license.")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        standardAssets.forEach { asset ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(asset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(asset.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.initCheckoutFlow(asset.price, asset.name, isEscrow = false) },
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.testTag("buy_button_${asset.name.replace(" ", "_")}")
                                    ) {
                                        Text("${asset.price.toInt()} USDT")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Escrow B2B
                    val escrowAssets = listOf(
                        MarketplaceAsset("Shipment Import Vault #980", 3500.0, "Secures shipment logistics. Capital holds in Smart Vault till delivery confirm."),
                        MarketplaceAsset("Enterprise Escrow Retainer", 15000.0, "Corporate development security escrow pool for external audit teams.")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        escrowAssets.forEach { asset ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(asset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(asset.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.initCheckoutFlow(asset.price, asset.name, isEscrow = true) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier.testTag("buy_escrow_${asset.name.replace(" ", "_")}")
                                    ) {
                                        Text("${asset.price.toInt()} USDT")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MarketplaceAsset(val name: String, val price: Double, val desc: String)

@Composable
fun BlockExplorerScreen(viewModel: MainViewModel) {
    val ethBlocks by viewModel.ethBlocks.collectAsStateWithLifecycle()
    val tronBlocks by viewModel.tronBlocks.collectAsStateWithLifecycle()
    val arbBlocks by viewModel.arbBlocks.collectAsStateWithLifecycle()
    val bscBlocks by viewModel.bscBlocks.collectAsStateWithLifecycle()

    var activeTabNetwork by remember { mutableStateOf("Ethereum (ERC20)") }
    val blockData = when (activeTabNetwork) {
        "Ethereum (ERC20)" -> ethBlocks
        "TRON (TRC20)" -> tronBlocks
        "Arbitrum (ERC20)" -> arbBlocks
        else -> bscBlocks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "MULTI-CHAIN NETWORK EXPLORER",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Block Explorer",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W900
            )
            Text(
                text = "Transparent audit of on-chain ledger pools",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = when (activeTabNetwork) {
                    "Ethereum (ERC20)" -> 0
                    "TRON (TRC20)" -> 1
                    "Arbitrum (ERC20)" -> 2
                    else -> 3
                },
                containerColor = Color.Transparent,
                edgePadding = 0.dp
            ) {
                val list = listOf("Ethereum (ERC20)", "TRON (TRC20)", "Arbitrum (ERC20)", "Binance Smart Chain")
                list.forEachIndexed { i, net ->
                    Tab(selected = activeTabNetwork == net, onClick = { activeTabNetwork = net }) {
                        Text(net, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (blockData.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(blockData) { block ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BLOCK #${block.height}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(block.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "HASH: ${block.blockHash}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "PARENT: ${block.previousHash}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Transactions (${block.txCount})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        block.transactions.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TX: ${tx.txHash.take(16)}...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                    )
                                    Text(
                                        text = "From: ${tx.fromAddress.take(8)}... To: ${tx.toAddress.take(8)}...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                                    )
                                }
                                Text(
                                    text = "${tx.amount} USDT",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventLogsScreen(viewModel: MainViewModel, events: List<EventLogEntity>) {
    val searchQuery by viewModel.eventSearchQuery.collectAsStateWithLifecycle()
    var selectedEventForAudit by remember { mutableStateOf<EventLogEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "FORENSIC IMMUTABLE EVENT TRAIL",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "Audit Logs",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.W900
        )
        Text(
            text = "Secured with forensic cryptographic chained hashes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("event_search_bar"),
            placeholder = { Text("Search event codes or data hashes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Audit Records (${events.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Reset Audit State",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable { viewModel.resetEventLogs() }
                    .testTag("reset_logs_button")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events) { ev ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedEventForAudit = ev },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "  ${ev.eventType}  ",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(ev.timestamp)),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ev.payloadJson,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "INTEGRITY SIGNATURE: ${ev.integrityHash.take(32)}...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }

    // Integrity Audit Details dialog
    if (selectedEventForAudit != null) {
        val detail = selectedEventForAudit!!
        AlertDialog(
            onDismissRequest = { selectedEventForAudit = null },
            title = { Text("Audit Details: ${detail.eventType}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(detail.timestamp))}")
                    Text("Details:\n${detail.payloadJson}", fontWeight = FontWeight.Bold)
                    Text("Network context: ${detail.network}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Immuntable Audit Proof:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "SHA-256 Chaining Hash:\n${detail.integrityHash}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp)
                    )
                    Text(
                        "Security Assertion: Calculated using SHA-256 based on (SequenceIndex + PreviousEventHash + CurrentPayload). Immutable verification proven.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedEventForAudit = null }) {
                    Text("Ok")
                }
            }
        )
    }
}

@Composable
fun ComplianceSecurityScreen(viewModel: MainViewModel) {
    val hsmKeyVersion by viewModel.hsmKeyVersion.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "HSM CONFIGURATION & AML RULES",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Compliance & security",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W900
            )
            Text(
                text = "Bank-grade HSM policies and AML rulesets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Compliancy Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Hardware Security Module (HSM)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vault Root Core Status: ONLINE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("AES-256 Core signature encryption", style = MaterialTheme.typography.bodySmall)
                            Text("Current Root Key ID: v$hsmKeyVersion", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = { viewModel.rotateMasterHsmKey() },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("rotate_key_button")
                        ) {
                            Text("Rotate Key")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Regulatory Compliance Compliance Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• KYC Verification Module: ACTIVE\n" +
                        "• AML Screening engine: ENABLED (0.15 limit)\n" +
                        "• Sanctions & OFAC Screening: LIVE\n" +
                        "• Travel Rule (VASP Interoperability): ENABLED",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Stablecoin CONNECT validates the identity of corporate accounts and audits every payee using transaction history screening tools. Senders exceeding 15% risk triggers automatic settlement rejection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Integration Protocol Assertions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "GDPR Auditable: Yes. In compliance with data protection specifications, zero plain-text customer secrets reside on cloud nodes. All private credentials exist as encrypted local instances. SOC2 ready.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun CreateWalletDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, type: String, network: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BUSINESS") }
    var network by remember { mutableStateOf("Ethereum (ERC20)") }

    var expandedType by remember { mutableStateOf(false) }
    var expandedNetwork by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Derive Corporate Node Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Wallet Node Label") },
                    placeholder = { Text("e.g., Marketing Sub-Treasury") },
                    modifier = Modifier.fillMaxWidth().testTag("wallet_label_input")
                )

                // Type picker
                Box {
                    OutlinedButton(
                        onClick = { expandedType = true },
                        modifier = Modifier.fillMaxWidth().testTag("wallet_type_dropdown_btn")
                    ) {
                        Text("Node Type: $type")
                    }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        val types = listOf("BUSINESS", "CUSTOMER", "ESCROW", "TREASURY")
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    expandedType = false
                                },
                                modifier = Modifier.testTag("type_item_$t")
                            )
                        }
                    }
                }

                // Network Picker
                Box {
                    OutlinedButton(
                        onClick = { expandedNetwork = true },
                        modifier = Modifier.fillMaxWidth().testTag("wallet_network_dropdown_btn")
                    ) {
                        Text("Network: $network")
                    }
                    DropdownMenu(expanded = expandedNetwork, onDismissRequest = { expandedNetwork = false }) {
                        val networks = listOf("Ethereum (ERC20)", "TRON (TRC20)", "Arbitrum (ERC20)", "Binance Smart Chain")
                        networks.forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n) },
                                onClick = {
                                    network = n
                                    expandedNetwork = false
                                },
                                modifier = Modifier.testTag("network_item_${n.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (label.isNotEmpty()) onConfirm(label, type, network) },
                modifier = Modifier.testTag("confirm_create_wallet_btn")
            ) {
                Text("Derive")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CheckoutGatewayWidget(
    payment: PaymentEntity,
    step: MainViewModel.CheckoutStep,
    error: String?,
    viewModel: MainViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { /* Block clicks underlying */ },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("checkout_gateway_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Stablecoin CONNECT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.cancelCheckout() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Gateway")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(targetState = step, label = "PaymentProcessStepSwitch") { s ->
                    when (s) {
                        MainViewModel.CheckoutStep.PROMPTING_SIGNATURE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SECURE STABLECOIN SETTLEMENT GATEWAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${payment.amount} USDT",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = payment.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Network details Info
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.background,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Selected Network", style = MaterialTheme.typography.bodySmall)
                                        Text(payment.network, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Corporate Escrow", style = MaterialTheme.typography.bodySmall)
                                        Text(if (payment.isEscrow) "YES" else "NO", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gas Settlement Fee", style = MaterialTheme.typography.bodySmall)
                                        Text("0.15 USDT", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Automated Splits", style = MaterialTheme.typography.bodySmall)
                                        Text(if (payment.splitConfigJson.isNotEmpty()) "ENABLED" else "DISABLED", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("AML Verification Check", style = MaterialTheme.typography.bodySmall)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = "Shield State", tint = Color(0xFF00A389), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("HEALTHY (PASS)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00A389)))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.executeCheckoutSignature() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("submit_signing_checkout_btn")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "sign")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Decrypt Signature & Broadcast")
                                }
                            }
                        }
                        MainViewModel.CheckoutStep.BROADCASTING -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Broadcasting raw transaction...", fontWeight = FontWeight.Bold)
                                Text("Encrypting payload, calling node pools, securing mempool confirmations...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            }
                        }
                        MainViewModel.CheckoutStep.COMPLETED -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Cleared", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Settlement Fully Confirmed!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Ledger updated. Chained event emitted. Customer keys derived.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.cancelCheckout() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("close_success_checkout_btn")
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                        MainViewModel.CheckoutStep.ERROR -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Platform Exception Detected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                Text(error ?: "Unknown cryptographic protocol validation error.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.cancelCheckout() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.size(1.dp))
                        }
                    }
                }
            }
        }
    }
}
