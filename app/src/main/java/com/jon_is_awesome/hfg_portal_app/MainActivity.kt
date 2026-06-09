package com.jon_is_awesome.hfg_portal_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jon_is_awesome.hfg_portal_app.ui.auth.AuthViewModel
import com.jon_is_awesome.hfg_portal_app.ui.auth.AuthViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.auth.LoginScreen
import com.jon_is_awesome.hfg_portal_app.ui.navigation.NavGraph
import com.jon_is_awesome.hfg_portal_app.ui.navigation.Screen
import com.jon_is_awesome.hfg_portal_app.ui.theme.Hfg_portal_appTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hfg_portal_appTheme {
                val app = applicationContext as HfgApplication
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(app.remoteDataSource)
                )
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                when (isLoggedIn) {
                    true -> MainScreen(authViewModel)
                    false -> LoginScreen(viewModel = authViewModel, onLoginSuccess = {})
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val context = LocalContext.current
    val app = context.applicationContext as HfgApplication
    val userPrefs = app.userPreferencesRepository
    val userName by userPrefs.userName.collectAsState(initial = "User")
    
    var isAdminMode by remember { mutableStateOf(false) }

    val menuItems = listOf(
        Screen.Dashboard,
        Screen.Budget,
        Screen.CarLogs
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E2632),
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ROOK",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Rounded.Pets, 
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.LightGray)
                            Spacer(Modifier.width(16.dp))
                            Text(userName, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(Icons.Rounded.Logout, contentDescription = "Logout", tint = Color.LightGray)
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAdminMode,
                            onCheckedChange = { isAdminMode = it },
                            colors = CheckboxDefaults.colors(uncheckedColor = Color.White)
                        )
                        Text("Admin Mode", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))

                    menuItems.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { 
                                Column {
                                    Text(screen.title)
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color.Transparent,
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.LightGray,
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.LightGray
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color.Transparent,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.LightGray,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.LightGray
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = when(currentDestination?.route) {
                                Screen.Dashboard.route -> "Home"
                                Screen.CarLogs.route -> "Car Maintenance"
                                Screen.Budget.route -> "Budget"
                                else -> "HFG Portal"
                            }
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
