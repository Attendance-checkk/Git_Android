package com.example.myapplication
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.CaptureActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class UserInfo(
    val id: Int,
    val student_code: String,
    val major: String,
    val name: String,
    val participant_count: Int,
    val createdAt: String
)

data class Event(val eventName: String, val eventTime: String, val showLogo: Boolean)

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private var userInfo by mutableStateOf<UserInfo?>(null)
    private var events by mutableStateOf<List<Event>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewMainScreen() {
        AppContent()
    }

    @Composable
    fun AppContent() {
        var isLoggedIn by remember { mutableStateOf(false) }
        var userId by remember { mutableStateOf("") }
        var userName by remember { mutableStateOf("") }
        var selectedDepartment by remember { mutableStateOf("학과 선택") }
        var showMapScreen by remember { mutableStateOf(false) }
        var showEventScreen by remember { mutableStateOf(false) }
        var showHomeScreen by remember { mutableStateOf(true) }
        var showMenuScreen by remember { mutableStateOf(false) }
        var showCalendarScreen by remember { mutableStateOf(false) } // Calendar 화면 변수 추가

        if (isLoggedIn) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        onMapClick = {
                            showMapScreen = true
                            showEventScreen = false
                            showHomeScreen = false
                            showMenuScreen = false
                            showCalendarScreen = false // Calendar 화면 숨김
                        },
                        onEventClick = {
                            showEventScreen = true
                            showMapScreen = false
                            showHomeScreen = false
                            showMenuScreen = false
                            showCalendarScreen = false // Calendar 화면 숨김
                        },
                        onHomeClick = {
                            showHomeScreen = true
                            showMapScreen = false
                            showEventScreen = false
                            showMenuScreen = false
                            showCalendarScreen = false // Calendar 화면 숨김
                            fetchEventList() // 홈 화면 클릭 시 이벤트 목록 가져오기
                        },
                        onMenuClick = {
                            fetchUserSettingInfo {
                                showMenuScreen = true
                                showHomeScreen = false
                                showEventScreen = false
                                showMapScreen = false
                                showCalendarScreen = false // Calendar 화면 숨김
                            }
                        },
                        onCalendarClick = { // Calendar 버튼 클릭 이벤트 추가
                            showCalendarScreen = true
                            showHomeScreen = false
                            showEventScreen = false
                            showMapScreen = false
                            showMenuScreen = false
                        }
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    when {
                        showMapScreen -> MapScreen {
                            showMapScreen = false
                        }
                        showEventScreen -> EventScreen()
                        showHomeScreen -> HomeScreen()
                        showMenuScreen -> {
                            MenuScreen(userInfo)
                        }
                        showCalendarScreen -> CalendarScreen() // Calendar 화면 표시
                    }
                }
            }
        } else {
            Scaffold { innerPadding ->
                MainScreenLogin(
                    onLogin = { id, name, department ->
                        userId = id
                        userName = name
                        selectedDepartment = department
                        isLoggedIn = true
                        login(userId, userName, selectedDepartment) {
                            showHomeScreen = true
                        }
                    },
                    innerPadding = innerPadding
                )
            }
        }
    }

    @Composable
    fun HomeScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // 스크롤 가능하게 설정
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "이벤트 목록",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 이벤트 목록을 가져옵니다.
            events.forEach { event ->
                EventItem(event)
            }
        }
    }

    @Composable
    fun MenuScreen(userInfo: UserInfo?) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            // 사용자 정보 표시
            Text(text = "학과: ${userInfo?.major ?: "입력되지 않음"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "학번: ${userInfo?.student_code ?: "입력되지 않음"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "이름: ${userInfo?.name ?: "입력되지 않음"}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { /* 설정 화면 이동 */ }) {
                Text("설정")
            }
            Button(onClick = { /* 문의하기 화면 이동 */ }) {
                Text("문의하기")
            }
            Button(onClick = { /* 만족도 조사 화면 이동 */ }) {
                Text("만족도 조사")
            }
        }
    }

    @Composable
    fun MainScreenLogin(onLogin: (String, String, String) -> Unit, innerPadding: PaddingValues) {
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            val departments = listOf("컴퓨터소프트웨어공학과", "정보보호학과", "의료IT공학과", "AI·빅데이터학과", "사물인터넷학과", "메타버스&게임학과")
            var selectedDepartment by remember { mutableStateOf("학과 선택") }
            var expanded by remember { mutableStateOf(false) }

            Box {
                Text(
                    text = selectedDepartment,
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(16.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    departments.forEach { department ->
                        DropdownMenuItem(
                            text = { Text(department) },
                            onClick = {
                                selectedDepartment = department
                                expanded = false
                            }
                        )
                    }
                }
            }

            var studentNumber by remember { mutableStateOf(TextFieldValue()) }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "학번:", style = MaterialTheme.typography.bodyMedium)
                BasicTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, Color.Gray)
                        .padding(8.dp)
                )
            }

            var name by remember { mutableStateOf(TextFieldValue()) }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "이름:", style = MaterialTheme.typography.bodyMedium)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, Color.Gray)
                        .padding(8.dp)
                )
            }

            Button(onClick = {
                if (studentNumber.text.isNotEmpty() && name.text.isNotEmpty() && selectedDepartment != "학과 선택") {
                    onLogin(studentNumber.text, name.text, selectedDepartment)
                } else {
                    println("학번, 이름, 학과를 입력해주세요.")
                }
            }) {
                Text("로그인")
            }
        }
    }
    @Composable
    fun BottomNavigationBar(
        onMapClick: () -> Unit,
        onEventClick: () -> Unit,
        onHomeClick: () -> Unit,
        onMenuClick: () -> Unit,
        onCalendarClick: () -> Unit
    ) {
        BottomAppBar {
            IconButton(onClick = { startQrScan() }) {
                Icon(painter = painterResource(id = R.drawable.ic_qr), contentDescription = "QR")
            }
            Spacer(modifier = Modifier.weight(1f)) // Spacer 추가

            IconButton(onClick = { onMapClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_map), contentDescription = "Map")
            }
            Spacer(modifier = Modifier.weight(1f)) // Spacer 추가

            IconButton(onClick = { onHomeClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_home), contentDescription = "Home")
            }
            Spacer(modifier = Modifier.weight(1f)) // Spacer 추가

            IconButton(onClick = { onCalendarClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_calendar), contentDescription = "Calendar")
            }
            Spacer(modifier = Modifier.weight(1f)) // Spacer 추가

            IconButton(onClick = { onMenuClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_menu), contentDescription = "Menu")
            }
        }
    }

    @Composable
    fun CalendarScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.calendar),
                contentDescription = "Calendar Image",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    private fun fetchUserSettingInfo(onSuccess: () -> Unit) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        Log.d("QR_SCAN", "Access Token: $accessToken") // 토큰 로그 추가

        val url = "http://54.180.7.191:9999/user/setting/info" // 실제 API URL로 변경

        Log.d("FetchUserSettingInfo", "Access Token: $accessToken") // Access Token 로그

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "$accessToken") // Authorization 헤더 추가
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FetchUserSettingInfo", "Request failed: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("FetchUserSettingInfo", "Response body: $responseBody")

                    // JSON 파싱
                    try {
                        val jsonArray = JSONArray(responseBody)
                        val jsonObject = jsonArray.getJSONObject(0)
                        userInfo = UserInfo(
                            id = jsonObject.getInt("id"),
                            student_code = jsonObject.getString("student_code"),
                            major = jsonObject.getString("major"),
                            name = jsonObject.getString("name"),
                            participant_count = jsonObject.getInt("participant_count"),
                            createdAt = jsonObject.getString("createdAt")
                        )

                        Log.d("FetchUserSettingInfo", "User Info: $userInfo")

                        // UI 업데이트
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500) // 0.5초 딜레이
                            onSuccess() // 사용자 정보 로드 후 UI 업데이트
                        }
                    } catch (e: Exception) {
                        Log.e("FetchUserSettingInfo", "JSON parsing error: ${e.message}")
                    }
                } else {
                    Log.e("FetchUserSettingInfo", "Error: ${response.code} - ${response.message}")
                }
            }
        })
    }

    private fun login(userId: String, userName: String, department: String, onSuccess: () -> Unit) {
        val url = "http://54.180.7.191:9999/user/login" // 실제 API URL로 변경
        val json = """{"student_code":"$userId","name":"$userName","major":"$department"}""" // JSON 형식

        // MediaType 정의
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        // RequestBody 생성
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody != null) {
                        val responseData = responseBody.string()
                        println("Response: $responseData")

                        // JSON 파싱
                        val jsonResponse = JSONObject(responseData)
                        val status = jsonResponse.getInt("code")

                        if (status == 200) {
                            // 로그인 성공 시 토큰 처리
                            val token = jsonResponse.getJSONObject("token")
                            val accessToken = token.getString("access_token")
                            val refreshToken = token.getString("refresh_token")

                            // 로컬 스토리지에 저장 (예: SharedPreferences 사용)
                            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putString("access_token", accessToken)
                                putString("refresh_token", refreshToken)
                                apply()
                            }
                            println("Access Token: $accessToken")
                            println("Refresh Token: $refreshToken")

                            // 이벤트 목록을 가져오고 홈 화면으로 이동
                            fetchEventList() // 홈 화면으로 이동하며 이벤트 목록 가져오기
                            onSuccess() // 로그인 성공 시 onSuccess 호출
                        } else {
                            println("로그인 실패: ${jsonResponse.getString("message")}")
                        }
                    } else {
                        println("Error: Response body is null")
                    }
                } else {
                    println("Error: ${response.code} - ${response.message}")
                }
            }
        })
    }


    @Composable
    fun MapScreen(onBack: () -> Unit) {
        var selectedFloor by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "층 선택",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            val floors = listOf("1층", "2층", "3층")
            floors.forEach { floor ->
                Button(
                    onClick = { selectedFloor = floor },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = floor)
                }
            }

            selectedFloor?.let {
                val imageResId = when (it) {
                    "1층" -> R.drawable.floor1_map
                    "2층" -> R.drawable.floor2_map
                    "3층" -> R.drawable.floor3_map
                    else -> null
                }

                imageResId?.let { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "$it 지도",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }

        }
    }

    @Composable
    fun EventScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "학술제 참여하고 경품 받자!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val events = listOf(
                Event("화요일 10:30 ~ 화요일 11:00", "개회식", true),
                Event("화요일 11:00 ~ 수요일 12:00", "게임 경진대회", true),
                Event("화요일 11:00 ~ 화요일 12:00", "SW 프로젝트 발표 본선", true),
                Event("화요일 14:00 ~ 화요일 14:20", "졸업생 토크콘서트 [장경호]", false),
                Event("화요일 14:00 ~ 화요일 14:20", "졸업생 토크콘서트 [전시온]", false)
            )

            events.forEach { event ->
                EventItem(event)
            }
        }
    }
    @Composable
    fun EventItem(event: Event) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, Color.Gray)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 작은 글씨로 날짜 및 시간 표시
                Text(text = event.eventTime, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))

                // 큰 글씨로 이벤트 이름 표시
                Text(text = event.eventName, style = MaterialTheme.typography.bodyLarge)
            }

            if (event.showLogo) {
                Image(
                    painter = painterResource(id = R.drawable.sch_logo),
                    contentDescription = "SCH 로고",
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Box(modifier = Modifier.size(40.dp))
            }
        }
    }

    // QR 코드 스캔 기능
    private fun startQrScan() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, CaptureActivity::class.java)
            startActivityForResult(intent, QR_SCAN_REQUEST_CODE)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScan()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QR_SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            val eventCode = data?.getStringExtra("SCAN_RESULT") ?: return
            sendAttendance(eventCode)
        }
    }

    private fun sendAttendance(eventCode: String) {
        try {
            val url = "http://54.180.7.191:9999/user/attendance" // 실제 API URL로 변경
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val accessToken = sharedPreferences.getString("access_token", null)

            val json = """{"event_code":"$eventCode"}""" // JSON 형식
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", accessToken ?: "")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("QR_SCAN", "네트워크 요청 실패: ${e.message}") // 로그 추가
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        // 요청 성공 시 홈 화면으로 이동하며 이벤트 목록 가져오기
                        fetchEventList() // 이벤트 목록 가져오기
                        // 홈 화면으로 이동하는 추가 로직이 필요할 수 있습니다.
                    } else {
                        Log.e("QR_SCAN", "Error: ${response.code} - ${response.message}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("QR_SCAN", "Error: ${e.message}")
        }
    }
    private fun fetchEventList() {
        val url = "http://54.180.7.191:9999/user/event/list" // 실제 API URL로 변경
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", accessToken ?: "")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FETCH_EVENT_LIST", "Request failed: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("FETCH_EVENT_LIST", "Response body: $responseBody")

                    // JSON 파싱
                    try {
                        val jsonArray = JSONArray(responseBody)
                        val fetchedEvents = mutableListOf<Event>()

                        for (i in 0 until jsonArray.length()) {
                            val eventObject = jsonArray.getJSONObject(i)
                            val eventName = eventObject.getString("event_name")
                            val participants = eventObject.getJSONArray("participants")

                            // participants 배열에 값이 들어있는지 확인
                            val showLogo = participants.length() > 0

                            // 이벤트 생성 (eventTime을 추가)
                            val eventTime = when (eventName) {
                                "개회식" -> "11월 10일 15:00 ~ 15:30"
                                "게임 경진대회" -> "11월 10일 15:30 ~ 16:00"
                                "프로젝트 발표" -> "11월 10일 15:30 ~ 16:00"
                                "졸업생 토크콘서트 01" -> "11월 10일 15:30 ~ 16:00"
                                "졸업생 토크콘서트 02" -> "11월 10일 15:30 ~ 16:00"
                                "졸업생 토크콘서트 03" -> "11월 10일 15:30 ~ 16:00"
                                "졸업생 토크콘서트 04" -> "11월 10일 15:30 ~ 16:00"
                                "졸업생 토크콘서트 05" -> "11월 10일 15:30 ~ 16:00"
                                else -> "시간 미정" // 기본 시간 설정
                            }

                            fetchedEvents.add(Event(eventName, eventTime, showLogo))
                        }

                        // UI 업데이트 전에 Activity가 활성 상태인지 확인
                        if (!isFinishing && !isDestroyed) {
                            events = fetchedEvents // 이벤트 목록 업데이트
                        }
                    } catch (e: Exception) {
                        Log.e("FETCH_EVENT_LIST", "JSON parsing error: ${e.message}")
                    }
                } else {
                    Log.e("FETCH_EVENT_LIST", "Error: ${response.code} - ${response.message}")
                }
            }
        })
    }
    companion object {
        private const val QR_SCAN_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_CODE = 1002
    }
}





