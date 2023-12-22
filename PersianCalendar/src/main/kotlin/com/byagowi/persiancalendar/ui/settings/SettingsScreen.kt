package com.byagowi.persiancalendar.ui.settings

import android.app.StatusBarManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.byagowi.persiancalendar.BuildConfig
import com.byagowi.persiancalendar.DEFAULT_THEME_CYBERPUNK
import com.byagowi.persiancalendar.PREF_HAS_EVER_VISITED
import com.byagowi.persiancalendar.PREF_THEME_CYBERPUNK
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.service.PersianCalendarTileService
import com.byagowi.persiancalendar.ui.about.ColorSchemeDemoDialog
import com.byagowi.persiancalendar.ui.about.DynamicColorsDialog
import com.byagowi.persiancalendar.ui.about.IconsDemoDialog
import com.byagowi.persiancalendar.ui.about.ScheduleAlarm
import com.byagowi.persiancalendar.ui.about.ShapesDemoDialog
import com.byagowi.persiancalendar.ui.about.TypographyDemoDialog
import com.byagowi.persiancalendar.ui.common.ThreeDotsDropdownMenu
import com.byagowi.persiancalendar.ui.settings.interfacecalendar.InterfaceCalendarSettings
import com.byagowi.persiancalendar.ui.settings.locationathan.LocationAthanSettings
import com.byagowi.persiancalendar.ui.settings.widgetnotification.WidgetNotificationSettings
import com.byagowi.persiancalendar.ui.theme.appColorAnimationSpec
import com.byagowi.persiancalendar.ui.utils.AppBlendAlpha
import com.byagowi.persiancalendar.ui.utils.ExtraLargeShapeCornerSize
import com.byagowi.persiancalendar.ui.utils.MaterialCornerExtraLargeTop
import com.byagowi.persiancalendar.ui.utils.getActivity
import com.byagowi.persiancalendar.utils.appPrefs
import com.byagowi.persiancalendar.utils.logException
import com.byagowi.persiancalendar.variants.debugAssertNotNull
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun SettingsScreen(
    openDrawer: () -> Unit,
    initialPage: Int,
    destination: String,
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = LocalContentColor.current,
                    actionIconContentColor = LocalContentColor.current,
                    titleContentColor = LocalContentColor.current,
                ),
                navigationIcon = {
                    IconButton(onClick = { openDrawer() }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.open_drawer)
                        )
                    }
                },
                actions = { ThreeDotsDropdownMenu { closeMenu -> MenuItems(closeMenu) } },
            )
        }
    ) { paddingValues ->
        Column(
            Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
            )
        ) {
            LaunchedEffect(Unit) {
                context.appPrefs.edit {
                    putBoolean(
                        PREF_HAS_EVER_VISITED,
                        true
                    )
                }
            }

            val tabs = listOf<Triple<ImageVector, List<Int>, @Composable () -> Unit>>(
                Triple(Icons.Default.Palette, listOf(R.string.pref_interface, R.string.calendar)) {
                    InterfaceCalendarSettings(destination)
                },
                Triple(
                    Icons.Default.Widgets, listOf(R.string.pref_notification, R.string.pref_widget)
                ) { WidgetNotificationSettings() },
                Triple(Icons.Default.LocationOn, listOf(R.string.location, R.string.athan)) {
                    LocationAthanSettings()
                },
            )

            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = tabs::size)
            val scope = rememberCoroutineScope()

            val selectedTabIndex = pagerState.currentPage
            TabRow(
                selectedTabIndex = selectedTabIndex,
                contentColor = LocalContentColor.current,
                containerColor = Color.Transparent,
                divider = {},
                indicator = @Composable { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        SecondaryIndicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .padding(horizontal = ExtraLargeShapeCornerSize.dp),
                            color = LocalContentColor.current.copy(alpha = AppBlendAlpha)
                        )
                    }
                },
            ) {
                tabs.forEachIndexed { index, (icon, titlesResId) ->
                    val title = titlesResId.joinToString(stringResource(R.string.spaced_and)) {
                        context.getString(it)
                    }
                    val isLandscape =
                        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                    if (isLandscape) Tab(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(title)
                            }
                        },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    ) else Tab(
                        icon = { Icon(icon, contentDescription = null) },
                        text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.clip(MaterialCornerExtraLargeTop()),
            ) { index ->
                val surfaceColor by animateColorAsState(
                    MaterialTheme.colorScheme.surface,
                    animationSpec = appColorAnimationSpec,
                    label = "surface color"
                )
                val onSurfaceColor by animateColorAsState(
                    MaterialTheme.colorScheme.onSurface,
                    animationSpec = appColorAnimationSpec,
                    label = "onSurface color"
                )
                Surface(
                    color = surfaceColor,
                    contentColor = onSurfaceColor,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        tabs[index].third()
                        Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
                    }
                }
            }
        }
    }
}

const val INTERFACE_CALENDAR_TAB = 0
const val WIDGET_NOTIFICATION_TAB = 1
const val LOCATION_ATHAN_TAB = 2

@Composable
private fun MenuItems(closeMenu: () -> Unit) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text(stringResource(R.string.live_wallpaper_settings)) },
        onClick = {
            closeMenu()
            runCatching {
                context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            }.onFailure(logException).getOrNull().debugAssertNotNull
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.screensaver_settings)) },
        onClick = {
            closeMenu()
            runCatching { context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) }.onFailure(
                logException
            ).getOrNull().debugAssertNotNull
        },
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_quick_settings_tile)) },
            onClick = {
                closeMenu()
                context.getSystemService<StatusBarManager>()?.requestAddTileService(
                    ComponentName(
                        context.packageName, PersianCalendarTileService::class.qualifiedName ?: "",
                    ),
                    context.getString(R.string.app_name),
                    Icon.createWithResource(context, R.drawable.day19),
                    {},
                    {},
                )
            },
        )
    }

    if (!BuildConfig.DEVELOPMENT) return // Rest are development only functionalities
    run {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text("Static vs generated icons") },
            onClick = { showDialog = true },
        )
        if (showDialog) IconsDemoDialog { showDialog = false }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text("Dynamic Colors") },
            onClick = { showDialog = true },
        )
        if (showDialog) DynamicColorsDialog { showDialog = false }
    }
    run {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text("Color Scheme") },
            onClick = { showDialog = true },
        )
        if (showDialog) ColorSchemeDemoDialog { showDialog = false }
    }
    run {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text("Typography") },
            onClick = { showDialog = true },
        )
        if (showDialog) TypographyDemoDialog { showDialog = false }
    }
    run {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text("Shapes") },
            onClick = { showDialog = true },
        )
        if (showDialog) ShapesDemoDialog { showDialog = false }
    }
    DropdownMenuItem(
        text = { Text("Clear preferences store and exit") },
        onClick = {
            context.appPrefs.edit { clear() }
            context.getActivity()?.finish()
        },
    )
    run {
        var showDialog by remember { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text("Schedule an alarm") },
            onClick = { showDialog = true },
        )
        if (showDialog) ScheduleAlarm { showDialog = false }
    }
//    fun viewCommandResult(command: String) {
//        val dialogBuilder = AlertDialog.Builder(activity)
//        val result = Runtime.getRuntime().exec(command).inputStream.bufferedReader().readText()
//        val button = ImageButton(activity).also { button ->
//            button.setImageDrawable(activity.getCompatDrawable(R.drawable.ic_baseline_share))
//            button.setOnClickListener {
//                activity.shareTextFile(result, "log.txt", "text/plain")
//            }
//        }
//        dialogBuilder.setCustomTitle(LinearLayout(activity).also {
//            it.layoutDirection = View.LAYOUT_DIRECTION_LTR
//            it.addView(button)
//        })
//        dialogBuilder.setView(ScrollView(activity).also { scrollView ->
//            scrollView.addView(TextView(activity).also {
//                it.text = result
//                it.textDirection = View.TEXT_DIRECTION_LTR
//            })
//            // Scroll to bottom, https://stackoverflow.com/a/3080483
//            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
//        })
//        dialogBuilder.show()
//    }
//    toolbar.menu.addSubMenu("Log Viewer").also {
//        it.add("Filtered").onClick {
//            viewCommandResult("logcat -v raw -t 500 *:S $LOG_TAG:V AndroidRuntime:E")
//        }
//        it.add("Unfiltered").onClick { viewCommandResult("logcat -v raw -t 500") }
//    }
//    toolbar.menu.addSubMenu("Log").also {
//        it.add("Log 'Hello'").onClick { debugLog("Hello!") }
//        it.add("Handled Crash").onClick { logException(Exception("Logged Crash!")) }
//        it.add("Crash!").onClick { error("Unhandled Crash!") }
//    }
    DropdownMenuItem(
        text = { Text("Start Dream") },
        onClick = {
            // https://stackoverflow.com/a/23112947
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_MAIN).setClassName(
                        "com.android.systemui", "com.android.systemui.Somnambulator"
                    )
                )
            }.onFailure(logException).getOrNull().debugAssertNotNull
        },
    )
    DropdownMenuItem(
        text = { Text("Cyberpunk") },
        onClick = {
            val appPrefs = context.appPrefs
            appPrefs.edit {
                putBoolean(
                    PREF_THEME_CYBERPUNK,
                    !appPrefs.getBoolean(PREF_THEME_CYBERPUNK, DEFAULT_THEME_CYBERPUNK)
                )
            }
            closeMenu()
        },
    )
}
