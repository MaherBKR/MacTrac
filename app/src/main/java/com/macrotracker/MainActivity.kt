package com.macrotracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.macrotracker.notifications.NotificationHelper
import com.macrotracker.ui.MainViewModel
import com.macrotracker.ui.scanner.BarcodeScannerScreen
import com.macrotracker.ui.screens.FoodsRecipesScreen
import com.macrotracker.ui.screens.PantryScreen
import com.macrotracker.ui.screens.ProgressScreen
import com.macrotracker.ui.screens.ShoppingScreen
import com.macrotracker.ui.screens.TodayScreen
import com.macrotracker.ui.theme.MacroColors
import com.macrotracker.ui.theme.MacroTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class BottomTab(
    val label: String,
    val icon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)

        setContent {
            MacroTrackerTheme {
                MacroTrackerRoot()
            }
        }
    }
}

@Composable
private fun MacroTrackerRoot(
    viewModel: MainViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val tabs = remember {
        listOf(
            BottomTab("Today", Icons.Default.Today),
            BottomTab("Progress", Icons.Default.Assessment),
            BottomTab("Foods", Icons.Default.RestaurantMenu),
            BottomTab("Pantry", Icons.Default.Kitchen),
            BottomTab("Shopping", Icons.Default.ShoppingCart)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MacroColors.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = MacroColors.CardSurface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTabIndex) {
                    0 -> TodayScreen(
                        viewModel = viewModel,
                        onOpenBarcodeScanner = { showBarcodeScanner = true }
                    )
                    1 -> ProgressScreen(
                        viewModel = viewModel,
                        onExportBackup = {
                            val stamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            exportBackupLauncher.launch("MacroTracker_Backup_$stamp.json")
                        },
                        onImportBackup = {
                            importBackupLauncher.launch(arrayOf("application/json"))
                        },
                        onJumpToTodayTab = { selectedTabIndex = 0 }
                    )
                    2 -> FoodsRecipesScreen(
                        viewModel = viewModel,
                        onOpenBarcodeScanner = { showBarcodeScanner = true }
                    )
                    3 -> PantryScreen(viewModel = viewModel)
                    4 -> ShoppingScreen(viewModel = viewModel)
                }
            }
        }

        if (showBarcodeScanner) {
            BarcodeScannerScreen(
                onBarcodeScanned = { barcode ->
                    showBarcodeScanner = false
                    viewModel.lookupBarcode(barcode)
                },
                onClose = { showBarcodeScanner = false }
            )
        }
    }
}
