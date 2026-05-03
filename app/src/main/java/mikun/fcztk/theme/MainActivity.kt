package mikun.fcztk.theme

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import mikun.fcztk.theme.TeamScreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.net.HttpURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1001
        private const val REQUEST_CODE_MANAGE_STORAGE = 1002
        const val CURRENT_VERSION = 41 // 当前应用版本号
        const val UPDATE_CHECK_DELAY = 500L // 进入app后0.5秒检查更新
        const val UPDATE_URL = "https://fcztk.netlify.app/gx"
        const val DOWNLOAD_URL = "https://fcztk.netlify.app/d"
        const val POETRY_URL = "https://v2.jinrishici.com/one.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var useSystemTheme by rememberSaveable { mutableStateOf(true) }
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }
            
            val currentTheme = if (useSystemTheme) systemDarkTheme else isDarkTheme
            
            凡尘主题库Theme(darkTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    凡尘主题库App(
                        isDarkTheme = currentTheme,
                        useSystemTheme = useSystemTheme,
                        onThemeChange = { isDarkTheme = it },
                        onSystemThemeChange = { useSystemTheme = it }
                    )
                }
            }
        }
    }

    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val writePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            writePermission && readPermission
        }
    }

    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // 权限已授予
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_MANAGE_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        // 权限已授予
                    }
                }
            }
        }
    }

    // 检查更新的方法
    fun checkForUpdates(context: Context, onUpdateAvailable: (version: Int, content: String) -> Unit, onLatestVersion: () -> Unit) {
        Thread {
            try {
                val url = URL(UPDATE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    bufferedReader.close()
                    inputStream.close()
                    
                    val content = stringBuilder.toString()
                    // 解析版本号和更新内容
                    val versionPattern = Regex("版本号:『(\\d+)』")
                    val contentPattern = Regex("更新内容:『(.*?)』")
                    
                    val versionMatch = versionPattern.find(content)
                    val contentMatch = contentPattern.find(content)
                    
                    if (versionMatch != null) {
                        val latestVersion = versionMatch.groupValues[1].toInt()
                        val updateContent = contentMatch?.groupValues?.get(1) ?: ""
                        
                        if (latestVersion > CURRENT_VERSION) {
                            // 主线程显示更新对话框
                            runOnUiThread {
                                onUpdateAvailable(latestVersion, updateContent)
                            }
                        } else {
                            // 已是最新版本
                            runOnUiThread {
                                onLatestVersion()
                            }
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 打开更新链接
    fun openUpdateLink(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL))
        context.startActivity(intent)
    }

    // 获取今日诗词的方法
    fun getTodayPoetry(onPoetryLoaded: (PoetryData) -> Unit, onError: () -> Unit) {
        Thread {
            try {
                val url = URL(POETRY_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    bufferedReader.close()
                    inputStream.close()
                    
                    val jsonString = stringBuilder.toString()
                    
                    // 简单解析JSON - 提取所需字段
                    val contentPattern = Regex("\"content\":\"([^\"]+)\"")
                    val dynastyPattern = Regex("\"dynasty\":\"([^\"]+)\"")
                    val authorPattern = Regex("\"author\":\"([^\"]+)\"")
                    val titlePattern = Regex("\"title\":\"([^\"]+)\"")
                    
                    val contentMatch = contentPattern.find(jsonString)
                    val dynastyMatch = dynastyPattern.find(jsonString)
                    val authorMatch = authorPattern.find(jsonString)
                    val titleMatch = titlePattern.find(jsonString)
                    
                    if (contentMatch != null && dynastyMatch != null && authorMatch != null && titleMatch != null) {
                        val poetryData = PoetryData(
                            content = contentMatch.groupValues[1],
                            dynasty = dynastyMatch.groupValues[1],
                            author = authorMatch.groupValues[1],
                            title = titleMatch.groupValues[1]
                        )
                        runOnUiThread {
                            onPoetryLoaded(poetryData)
                        }
                    } else {
                        runOnUiThread {
                            onError()
                        }
                    }
                } else {
                    runOnUiThread {
                        onError()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    onError()
                }
            }
        }.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun 凡尘主题库Theme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val darkColorScheme = androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF41A6FF),
        secondary = Color(0xFF41A6FF),
        tertiary = Color(0xFF41A6FF),
        background = Color(0xFF1E1E1E),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF2D2D2D),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
    )

    val lightColorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF262626),
        secondary = Color(0xFF000000),
        tertiary = Color(0xFF3C59FF),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF5F5F5),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.Black,
        onSurface = Color.Black,
    )

    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(
            bodyLarge = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
            bodyMedium = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp
            ),
            titleLarge = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
            titleSmall = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            labelLarge = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            )
        ),
        content = content
    )
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onSplashFinished()
    }

    val isDarkTheme = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "凡尘主题库",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(40.dp))
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun 凡尘主题库App(
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSystemThemeChange: (Boolean) -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf(0) }
    var updateContent by remember { mutableStateOf("") }
    val navController = rememberNavController()
    var phoneBrand by rememberSaveable { mutableStateOf("华为") }
    val context = LocalContext.current
    val mainActivity = context as MainActivity

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else {
        // 进入app3秒后检查更新
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(MainActivity.UPDATE_CHECK_DELAY)
            mainActivity.checkForUpdates(
                context = context,
                onUpdateAvailable = { version, content ->
                    latestVersion = version
                    updateContent = content
                    showUpdateDialog = true
                },
                onLatestVersion = {
                    Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavigationGraph(
                navController = navController,
                phoneBrand = phoneBrand,
                onBrandChange = { brand -> phoneBrand = brand },
                isDarkTheme = isDarkTheme,
                useSystemTheme = useSystemTheme,
                onThemeChange = onThemeChange,
                onSystemThemeChange = onSystemThemeChange
            )
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNavigationBar(navController)
            }
        }
    }

    // 更新对话框
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Column {
                    Text("发现新版本", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("版本号：$latestVersion", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column {
                    Text("更新内容：", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    Text(updateContent)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        mainActivity.openUpdateLink(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("更新", fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("不了", fontSize = 16.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    var selectedItem by rememberSaveable { mutableStateOf("home") }
    val items = listOf(
        NavigationItem("home", "主题", Icons.Default.Home),
        NavigationItem("import", "导入", Icons.Default.FileUpload),
        NavigationItem("about", "关于", Icons.Default.Info)
    )
    
    val glassTabs = items.map { GlassTabItem(it.icon, it.label) }
    
    GlassBottomNavigationBar(
        selectedTabIndex = items.indexOfFirst { it.route == selectedItem },
        onTabSelected = { index ->
            selectedItem = items[index].route
            navController.navigate(items[index].route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        tabs = glassTabs
    )
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun NavigationGraph(
    navController: NavHostController,
    phoneBrand: String,
    onBrandChange: (String) -> Unit,
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSystemThemeChange: (Boolean) -> Unit
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen()
        }
        composable("import") {
            ImportToolScreen(phoneBrand = phoneBrand, onBrandChange = onBrandChange)
        }
        composable("about") {
            AboutScreen(
                navController = navController,
                isDarkTheme = isDarkTheme,
                useSystemTheme = useSystemTheme,
                onThemeChange = onThemeChange,
                onSystemThemeChange = onSystemThemeChange
            )
        }
        composable("team") {
            TeamScreen(navController = navController)
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val mainActivity = context as MainActivity
    val themes = remember { getThemes() }
    val scrollState = rememberScrollState()

    var showDialog by remember { mutableStateOf(false) }
    var currentExtractCode by remember { mutableStateOf("") }
    var currentLink by remember { mutableStateOf("") }
    
    var poetryData by remember { mutableStateOf<PoetryData?>(null) }
    
    // 获取今日诗词
    LaunchedEffect(Unit) {
        mainActivity.getTodayPoetry(
            onPoetryLoaded = { data ->
                poetryData = data
            },
            onError = {
                // 加载失败时使用默认诗词
                poetryData = PoetryData(
                    content = "重唱梅边新度曲，催发寒梢冻蕊",
                    dynasty = "宋",
                    author = "吴文英",
                    title = "江城子"
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        handleThemeClick(
                            context = context,
                            link = "https://www.123912.com/s/FtDojv-D6DFv?pwd=HM66#",
                            extractCode = "HM66"
                        ) { code, link ->
                            currentExtractCode = code
                            currentLink = link
                            showDialog = true
                        }
                    },
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    Image(
                        painter = rememberAsyncImagePainter("file:///android_asset/hm6.jpg"),
                        contentDescription = "仿鸿蒙6",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "仿鸿蒙6",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "点击图片跳转（提取码自动复制）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.123865.com/s/4SljVv-H3yUh"))
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC6A9FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第三方主题",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_share),
                        contentDescription = "跳转",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 2000.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(themes) { theme ->
                    ThemeCard(
                        theme = theme,
                        onClick = {
                            handleThemeClick(
                                context = context,
                                link = theme.link,
                                extractCode = theme.extractCode
                            ) { code, link ->
                                currentExtractCode = code
                                currentLink = link
                                showDialog = true
                            }
                        }
                    )
                }
            }
        }

        GlassTopBar(
            title = "凡尘主题库",
            subtitle = poetryData?.content ?: "重唱梅边新度曲，催发寒梢冻蕊",
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "图标",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("提取码已复制", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "提取码：$currentExtractCode",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                        fontSize = 18.sp
                    )
                    Text("提取码已复制到剪贴板，部分网盘可能会复制一些文字，请在键盘的剪贴板中查看")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentLink))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("立即跳转", fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消", fontSize = 16.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ThemeCard(theme: ThemeData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter("file:///android_asset/${theme.imageName}"),
                contentDescription = theme.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 19.5f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = theme.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class FileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val path: String
) {
    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$size B"
        }
    }
}

data class FileOperationResult(
    val success: Boolean,
    val message: String,
    val targetPath: String? = null
)

data class PoetryData(
    val content: String,
    val dynasty: String,
    val author: String,
    val title: String
)

@Composable
fun ImportToolScreen(phoneBrand: String, onBrandChange: (String) -> Unit) {
    val context = LocalContext.current
    val activity = LocalContext.current as? MainActivity
    var showMessage by remember { mutableStateOf(false) }
    var showNoThemesDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var selectedFileInfo by remember { mutableStateOf<FileInfo?>(null) }
    var isMovingFile by remember { mutableStateOf(false) }

    val checkAndRequestPermission = {
        val hasPermission = activity?.checkStoragePermission() ?: false
        if (!hasPermission) {
            showPermissionDialog = true
            false
        } else {
            true
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    val fileInfo = getFileInfoFromUri(context, uri)
                    if (fileInfo != null) {
                        if (fileInfo.name.endsWith(".hwt", ignoreCase = true) ||
                            fileInfo.name.endsWith(".hnt", ignoreCase = true)) {
                            selectedFileInfo = fileInfo
                            messageText = "文件选择成功！\n\n文件名：${fileInfo.name}\n文件大小：${fileInfo.getFormattedSize()}\n文件路径：${fileInfo.path}"
                        } else {
                            messageText = "请选择 .hwt 或 .hnt 格式的主题文件\n\n当前选择：${fileInfo.name}"
                        }
                    } else {
                        messageText = "无法获取文件信息，请重试"
                    }
                } catch (e: Exception) {
                    messageText = "文件处理失败：${e.message}"
                }
                showMessage = true
            }
        }
    )

    val checkThemesFolder = { brand: String ->
        val brandFolder = if (brand == "华为") "Huawei" else "Honor"
        val themesDir = File("/storage/emulated/0/$brandFolder/Themes")

        if (!themesDir.exists()) {
            showNoThemesDialog = true
            false
        } else {
            true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(114.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "导入工具",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "主题导入工具",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持 .hwt 和 .hnt 格式文件",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "手机品牌选择",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable {
                                onBrandChange("华为")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (phoneBrand == "华为")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (phoneBrand == "华为") null else CardDefaults.outlinedCardBorder()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "华为",
                                color = if (phoneBrand == "华为") Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable {
                                onBrandChange("荣耀")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (phoneBrand == "荣耀")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (phoneBrand == "荣耀") null else CardDefaults.outlinedCardBorder()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "荣耀",
                                color = if (phoneBrand == "荣耀") Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "当前品牌：$phoneBrand",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "主题文件将复制到：/storage/emulated/0/${if (phoneBrand == "华为") "Huawei" else "Honor"}/Themes/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        selectedFileInfo?.let { fileInfo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.hwthnt),
                            contentDescription = "文件",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "已选择的文件",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FileInfoRow(
                            label = "文件名：",
                            value = fileInfo.name,
                            maxLines = 1
                        )
                        FileInfoRow(
                            label = "文件大小：",
                            value = fileInfo.getFormattedSize()
                        )
                        FileInfoRow(
                            label = "文件路径：",
                            value = fileInfo.path,
                            maxLines = 3
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (checkAndRequestPermission()) {
                                if (checkThemesFolder(phoneBrand)) {
                                    isMovingFile = true
                                    val result = moveFileToThemeDirectory(context, fileInfo.uri, fileInfo.name, phoneBrand)
                                    isMovingFile = false
                                    messageText = result.message
                                    showMessage = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        enabled = !isMovingFile
                    ) {
                        if (isMovingFile) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("复制文件中...", fontSize = 16.sp)
                        } else {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("复制文件到主题目录", fontSize = 16.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "功能说明",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FeatureItem(
                    title = "主题文件导入",
                    description = "支持 .hwt 和 .hnt 格式的华为/荣耀主题文件"
                )
                FeatureItem(
                    title = "文件移动功能",
                    description = "自动移动文件到手机主题目录"
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "文件操作",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("选择主题文件 (.hwt/.hnt)", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.123865.com/s/4SljVv-H3yUh"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("下载更多主题", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        selectedFileInfo = null
                        messageText = "已清除选择的文件"
                        showMessage = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("清除选择", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(min = 200.dp, max = 400.dp)
            ) {
                Text(
                    text = "使用说明",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StepItem(
                        number = 1,
                        title = "选择手机品牌",
                        description = "在本页选择您的手机品牌（华为/荣耀）"
                    )
                    StepItem(
                        number = 2,
                        title = "下载主题文件",
                        description = "从主题页下载.hwt或.hnt格式的主题文件"
                    )
                    StepItem(
                        number = 3,
                        title = "选择文件",
                        description = "点击'选择主题文件'按钮，选择下载的文件"
                    )
                    StepItem(
                        number = 4,
                        title = "移动文件",
                        description = "点击'复制文件到主题目录'按钮，文件将自动移动到正确目录"
                    )
                    StepItem(
                        number = 5,
                        title = "导入主题",
                        description = "手动打开华为/荣耀主题应用，在主题应用中导入文件"
                    )
                    Text(
                        text = "提示：文件会自动复制到 /storage/emulated/0/[品牌]/Themes/ 目录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "注意：如果手机中没有Themes文件夹，那说明您可能不是 华为/荣耀 手机",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE57373),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "权限说明：首次使用需要授予存储权限",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
    
    GlassTopBar(
        title = "导入工具"
    )
}

    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = {
                Text("提示", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(messageText, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMessage = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确定", fontSize = 16.sp)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showNoThemesDialog) {
        AlertDialog(
            onDismissRequest = { showNoThemesDialog = false },
            title = {
                Text("Themes文件夹不存在", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "在您的手机中没有找到Themes文件夹：\n\n" +
                                "路径：/storage/emulated/0/${if (phoneBrand == "华为") "Huawei" else "Honor"}/Themes/\n\n" +
                                "您可能不是 华为/荣耀 手机",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                Column {
                    Button(
                        onClick = {
                            showNoThemesDialog = false
                            messageText = "您可能不是 华为/荣耀 手机"
                            showMessage = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("知道了", fontSize = 16.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNoThemesDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消", fontSize = 16.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text("需要存储权限", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
            },
            text = {
                Column {
                    Text(
                        text = "应用需要存储权限才能移动文件到主题目录。\n\n" +
                                "所需权限：\n" +
                                "• 读取外部存储\n" +
                                "• 写入外部存储\n\n" +
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    "Android 11+ 说明：\n" +
                                            "需要授予'管理所有文件'权限，请按以下步骤操作：\n" +
                                            "1. 点击下方按钮跳转到设置\n" +
                                            "2. 找到本应用\n" +
                                            "3. 开启'允许管理所有文件'选项"
                                } else {
                                    "Android 10及以下：\n" +
                                            "将请求运行时权限，请在弹出的对话框中允许"
                                },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        activity?.requestStoragePermission()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("前往设置授予权限", fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消", fontSize = 16.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun FeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StepItem(number: Int, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FileInfoRow(label: String, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getFileInfoFromUri(context: Context, uri: Uri): FileInfo? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                val displayName = if (displayNameIndex >= 0) cursor.getString(displayNameIndex) else "未知文件"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val path = uri.toString()

                FileInfo(uri, displayName, size, mimeType, path)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun moveFileToThemeDirectory(
    context: Context,
    uri: Uri,
    fileName: String,
    phoneBrand: String
): FileOperationResult {
    return try {
        val brandFolder = if (phoneBrand == "华为") "Huawei" else "Honor"
        val themesDir = File("/storage/emulated/0/$brandFolder/Themes")

        if (!themesDir.exists()) {
            return FileOperationResult(
                success = false,
                message = "Themes文件夹不存在\n\n" +
                        "路径：${themesDir.absolutePath}\n\n" +
                        "您可能不是 华为/荣耀 手机"
            )
        }

        try {
            val testFile = File(themesDir, ".test_write_permission")
            val created = testFile.createNewFile()
            if (created) {
                testFile.delete()
            } else {
                return FileOperationResult(
                    success = false,
                    message = "没有写入权限\n\n" +
                            "无法在以下目录创建文件：\n${themesDir.absolutePath}\n\n" +
                            "请检查存储权限设置"
                )
            }
        } catch (e: SecurityException) {
            return FileOperationResult(
                success = false,
                message = "权限被拒绝\n\n" +
                        "无法写入到：${themesDir.absolutePath}\n\n" +
                        "解决方法：\n" +
                        "1. 检查应用权限设置\n" +
                        "2. 授予存储权限\n" +
                        "3. Android 11+ 需要开启'管理所有文件'"
            )
        }

        val targetFile = File(themesDir, fileName)

        if (targetFile.exists()) {
            val deleted = targetFile.delete()
            if (!deleted) {
                return FileOperationResult(
                    success = false,
                    message = "文件已存在且无法覆盖\n\n" +
                            "请先删除原有文件：\n${targetFile.absolutePath}"
                )
            }
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }

                    outputStream.flush()

                    if (targetFile.exists() && targetFile.length() == totalBytes) {
                        FileOperationResult(
                            success = true,
                            message = "文件移动成功！\n\n" +
                                    "文件名：$fileName\n" +
                                    "文件大小：${String.format("%.2f", totalBytes / 1024.0 / 1024.0)} MB\n" +
                                    "目标路径：${targetFile.absolutePath}\n\n" +
                                    "现在可以打开华为/荣耀主题应用进行导入了",
                            targetPath = targetFile.absolutePath
                        )
                    } else {
                        FileOperationResult(
                            success = false,
                            message = "文件写入不完整\n\n" +
                                    "预期：$totalBytes 字节\n" +
                                    "实际：${targetFile.length()} 字节"
                        )
                    }
                }
            } ?: FileOperationResult(
                success = false,
                message = "无法打开文件流\n\n文件可能已被移动或删除"
            )
        } catch (e: IOException) {
            FileOperationResult(
                success = false,
                message = "文件读写错误：${e.message}"
            )
        }

    } catch (e: SecurityException) {
        FileOperationResult(
            success = false,
            message = "权限不足：${e.message}\n\n" +
                    "请确保已授予存储权限"
        )
    } catch (e: Exception) {
        FileOperationResult(
            success = false,
            message = "文件移动失败：${e.message}"
        )
    }
}

@Composable
fun AboutScreen(
    navController: NavHostController,
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSystemThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val systemDarkTheme = isSystemInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(114.dp))

            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "凡尘主题库",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(36.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column {
                    AboutItem(label = "当前版本", value = "Stable4.1")
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    AboutItem(
                        label = "跟随系统",
                        isClickable = true,
                        isSwitch = true,
                        switchValue = useSystemTheme,
                        onSwitchChange = { onSystemThemeChange(!useSystemTheme) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    AboutItem(
                        label = "深色模式",
                        isClickable = !useSystemTheme,
                        isSwitch = true,
                        switchValue = isDarkTheme,
                        onSwitchChange = { if (!useSystemTheme) onThemeChange(!isDarkTheme) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    AboutItem(
                        label = "意见反馈",
                        isClickable = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pd.qq.com/s/1g1xblhyi?b=9"))
                            context.startActivity(intent)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    AboutItem(
                        label = "开发组",
                        isClickable = true,
                        onClick = { navController.navigate("team") }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    AboutItem(label = "官方群聊", value = "905445554")
                }
            }
            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = rememberAsyncImagePainter("file:///android_asset/MIKUN-LOGO/mikungzs.png"),
                contentDescription = "品牌 Logo",
                modifier = Modifier.size(140.dp, 48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Copyright © MIKUN All Rights Reserved",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        GlassTopBar(
            title = "关于"
        )
    }
}

@Composable
fun AboutItem(
    label: String,
    value: String? = null,
    isClickable: Boolean = false,
    isSwitch: Boolean = false,
    switchValue: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isEnabled = !isSwitch || isClickable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 14.dp)
            .then(if (isClickable && !isSwitch) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (isEnabled) 
                MaterialTheme.colorScheme.onSurfaceVariant 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Box(
            modifier = Modifier.widthIn(min = 50.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isSwitch) {
                androidx.compose.material3.Switch(
                    checked = switchValue,
                    onCheckedChange = if (isEnabled) onSwitchChange else null
                )
            } else if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (isClickable) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_send),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

data class ThemeData(
    val id: Int,
    val name: String,
    val desc: String,
    val imageName: String,
    val link: String,
    val extractCode: String
)

fun getThemes(): List<ThemeData> {
    return listOf(
        ThemeData(
            id = 1,
            name = "拟态白昼",
            desc = "提取码自动复制",
            imageName = "ntbz.jpg",
            link = "https://sss00001.lanzoui.com/b007t4sx3g",
            extractCode = "YJDD"
        ),
        ThemeData(
            id = 2,
            name = "寰宇NEXT 3.0主题",
            desc = "提取码自动复制",
            imageName = "hm.png",
            link = "https://www.123912.com/s/FtDojv-WzGFv?",
            extractCode = "1rWT"
        ),
        ThemeData(
            id = 3,
            name = "月球一镜到底主题",
            desc = "提取码自动复制",
            imageName = "yq.jpg",
            link = "https://sss00001.lanzoui.com/b007t4sx3g",
            extractCode = "YJDD"
        ),
        ThemeData(
            id = 4,
            name = "鸿果26 Pro",
            desc = "提取码自动复制",
            imageName = "ios26.jpg",
            link = "https://sss00001.lanzoui.com/b007tsec1i",
            extractCode = "HG26"
        ),
        ThemeData(
            id = 5,
            name = "警鸿-凡尘版V3主题",
            desc = "提取码自动复制",
            imageName = "jingwu.jpg",
            link = "https://www.123865.com/s/FtDojv-s7MFv?",
            extractCode = "8E0N"
        ),
        ThemeData(
            id = 6,
            name = "土星一镜到底主题",
            desc = "提取码自动复制",
            imageName = "tuxing.jpg",
            link = "https://sss00001.lanzoui.com/b007t4sx3g",
            extractCode = "YJDD"
        ),
        ThemeData(
            id = 7,
            name = "火星一镜到底主题",
            desc = "提取码自动复制",
            imageName = "huoxing.jpg",
            link = "https://sss00001.lanzoui.com/b007t4sx3g",
            extractCode = "YJDD"
        ),
        ThemeData(
            id = 8,
            name = "少女OS",
            desc = "提取码自动复制",
            imageName = "shonu26.jpg",
            link = "https://www.123865.com/s/FtDojv-8sMFv?",
            extractCode = "Ovv2"
        ),
        ThemeData(
            id = 9,
            name = "华果主题",
            desc = "by 曾相见cxj",
            imageName = "HPOS.jpg",
            link = "https://wwr.lanzoui.com/b00rnx9uxc",
            extractCode = "4cy4"
        ),
        ThemeData(
            id = 10,
            name = "鸿果 18 Pro主题",
            desc = "提取码自动复制",
            imageName = "hg18.png",
            link = "https://sss00001.lanzoui.com/b007t3tu8d",
            extractCode = "HG18"
        )
    )
}

fun handleThemeClick(
    context: Context,
    link: String,
    extractCode: String,
    onShowDialog: (String, String) -> Unit
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("提取码", extractCode)
    clipboard.setPrimaryClip(clip)
    onShowDialog(extractCode, link)
}