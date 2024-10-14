package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private var userInfo by mutableStateOf<UserInfo?>(null)

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

        if (isLoggedIn) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        onMapClick = {
                            showMapScreen = true
                            showEventScreen = false
                            showHomeScreen = false
                            showMenuScreen = false
                        },
                        onEventClick = {
                            showEventScreen = true
                            showMapScreen = false
                            showHomeScreen = false
                            showMenuScreen = false
                        },
                        onHomeClick = {
                            showHomeScreen = true
                            showMapScreen = false
                            showEventScreen = false
                            showMenuScreen = false
                        },
                        onMenuClick = {
                            fetchUserSettingInfo {
                                showMenuScreen = true
                                showHomeScreen = false
                                showEventScreen = false
                                showMapScreen = false
                            }
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
                        showHomeScreen -> EventScreen() // 홈 화면을 이벤트 화면으로 설정
                        showMenuScreen -> {
                            MenuScreen(userInfo) // 사용자 정보를 메뉴 화면에 표시합니다.
                        }
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
        onMenuClick: () -> Unit
    ) {
        BottomAppBar {
            IconButton(onClick = { /* QR 화면으로 이동 */ }) {
                Icon(painter = painterResource(id = R.drawable.ic_qr), contentDescription = "QR")
            }
            IconButton(onClick = { onMapClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_map), contentDescription = "Map")
            }
            IconButton(onClick = { onHomeClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_home), contentDescription = "Home")
            }
            IconButton(onClick = { /* Calendar 화면으로 이동 */ }) {
                Icon(painter = painterResource(id = R.drawable.ic_calendar), contentDescription = "Calendar")
            }
            IconButton(onClick = { onMenuClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_menu), contentDescription = "Menu")
            }
        }
    }
    private fun fetchUserSettingInfo(onSuccess: () -> Unit) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)

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
    //
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

            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = "뒤로가기")
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
                Text(text = event.time, style = MaterialTheme.typography.bodyMedium)
                Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
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

    data class Event(val time: String, val title: String, val showLogo: Boolean)
}

