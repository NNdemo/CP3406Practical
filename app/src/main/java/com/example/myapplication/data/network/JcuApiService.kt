package com.example.myapplication.data.network

import android.util.Log
import com.example.myapplication.data.model.JcuClass
import com.example.myapplication.data.model.JcuClassStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.CookieManager
import java.net.HttpCookie
import java.net.URI
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * JCU教育系统API服务
 */
@Singleton
class JcuApiService @Inject constructor() {
    
    private val TAG = "JcuApiService"
    private val LOGIN_URL = "https://studentfirst.jcu.edu.sg/faces/login.xhtml"
    private val DASHBOARD_URL = "https://studentfirst.jcu.edu.sg/faces/dashboard.xhtml"
    private val MAIN_URL = "https://studentfirst.jcu.edu.sg/faces/main.xhtml"
    
    // Cookie管理器，保存登录会话
    private val cookieManager = CookieManager()
    
    // 当前登录状态
    private var isLoggedIn = false
    
    // 登录状态变量 - 作为备份检查
    private var loginStatus = false
    
    // 登录令牌
    private var jsfToken: String? = null
    
    /**
     * 登录JCU教育系统
     */
    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val loginUrl = "https://studentfirst.jcu.edu.sg/faces/login.xhtml"
            Log.d(TAG, "开始登录过程，尝试连接到 $loginUrl")
            System.out.println("JCU登录: 开始登录过程")
            
            // 获取登录页面
            val response = Jsoup.connect(loginUrl)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(30000)
                .followRedirects(true) // 不自动跟随重定向，手动处理
                .execute()
            
            Log.d(TAG, "成功获取登录页面，状态码: ${response.statusCode()}")
            Log.d(TAG, "登录页面内容: ${response.body()}")
            System.out.println("JCU登录: 获取登录页面状态码: ${response.statusCode()}")
            
            if (response.statusCode() != 200) {
                return@withContext LoginResult.Error("获取登录页面失败，状态码: ${response.statusCode()}")
            }
            
            // 解析表单
            val doc = response.parse()
            val forms = doc.select("form")
            Log.d(TAG, "找到${forms.size}个表单")
            System.out.println("JCU登录: 找到${forms.size}个表单")
            
            // 寻找包含登录字段的表单
            var loginForm: Element? = null
            var formId = ""
            var usernameField = ""
            var passwordField = ""
            var submitButton = ""
            var submitButtonValue = "Login"
            var viewState = ""
            var actionUrl = ""
            
            for ((index, form) in forms.withIndex()) {
                val id = form.id()
                val className = form.className()
                val inputs = form.select("input")
                Log.d(TAG, "表单 #$index - ID: '$id', 类名: '$className', 输入字段数: ${inputs.size}")
                System.out.println("JCU登录: 表单 #$index - ID: '$id', 类名: '$className', 输入字段数: ${inputs.size}")
                
                // 尝试找到密码字段
                val passwordInput = form.select("input[type=password]").firstOrNull()
                if (passwordInput != null) {
                    // 找到登录相关字段
                    val usernameInput = form.select("input[type=text]").firstOrNull()
                    val viewStateInput = form.select("input[name='jakarta.faces.ViewState']").firstOrNull()
                    
                    if (usernameInput != null && viewStateInput != null) {
                        Log.d(TAG, "找到可能的登录表单: #$index, ViewState长度: ${viewStateInput.attr("value").length}")
                        System.out.println("JCU登录: 找到可能的登录表单: #$index, ViewState长度: ${viewStateInput.attr("value").length}")
                        loginForm = form
                        formId = id
                        usernameField = usernameInput.attr("name")
                        passwordField = passwordInput.attr("name")
                        viewState = viewStateInput.attr("value")
                        actionUrl = form.attr("action")
                        
                        // 寻找提交按钮
                        val submitButtons = form.select("input[type=submit]")
                        if (submitButtons.isNotEmpty()) {
                            val submitBtn = submitButtons.first()
                            submitButton = submitBtn?.attr("name") ?: ""
                            submitButtonValue = submitBtn?.attr("value") ?: "Login"
                        }
                        break
                    }
                }
            }
            
            if (loginForm == null || formId.isEmpty() || usernameField.isEmpty() || passwordField.isEmpty() || submitButton.isEmpty()) {
                System.out.println("JCU登录: 未能找到登录表单或必要的登录字段")
                return@withContext LoginResult.Error("未能找到登录表单或必要的登录字段")
            }
            
            Log.d(TAG, "找到登录字段 - 用户名: '$usernameField', 密码: '$passwordField', 提交按钮: '$submitButton'")
            System.out.println("JCU登录: 找到登录字段 - 用户名: '$usernameField', 密码: '$passwordField', 提交按钮: '$submitButton'")
            
            // 获取并保存初始Cookie
            val initialCookies = response.cookies()
            cookieManager.cookieStore.removeAll()
            initialCookies.forEach { (name, value) ->
                val cookie = HttpCookie(name, value)
                cookie.domain = "studentfirst.jcu.edu.sg"
                cookie.path = "/"
                cookieManager.cookieStore.add(URI("https://studentfirst.jcu.edu.sg"), cookie)
            }
            
            Log.d(TAG, "获取到初始Cookie: $initialCookies")
            System.out.println("JCU登录: 获取到初始Cookie: $initialCookies")
            
            // 构建请求URL - 如果action是相对路径，需要转换为完整URL
            val requestUrl = if (actionUrl.startsWith("http")) {
                actionUrl
            } else if (actionUrl.startsWith("/")) {
                "https://studentfirst.jcu.edu.sg$actionUrl"
            } else {
                loginUrl
            }
            
            // 准备登录数据 - 严格按照表单中的字段顺序构建
            val formDataMap = LinkedHashMap<String, String>() // 使用LinkedHashMap保持插入顺序
            formDataMap[formId] = formId
            formDataMap[usernameField] = username
            formDataMap[passwordField] = password
            formDataMap[submitButton] = submitButtonValue
            formDataMap["jakarta.faces.ViewState"] = viewState
            
            Log.d(TAG, "准备发送登录请求，表单数据: ${formDataMap.keys}")
            System.out.println("JCU登录: 准备发送登录请求，表单数据: $formDataMap")
            
            // 发送登录请求，不跟随重定向
            val loginConnection = Jsoup.connect(requestUrl)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .data(formDataMap)
                .cookies(initialCookies)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Origin", "https://studentfirst.jcu.edu.sg")
                .header("Referer", loginUrl)
                .method(org.jsoup.Connection.Method.POST)
                .followRedirects(false) // 不自动跟随重定向，手动处理
                .timeout(30000)
            
            // 执行登录请求
            val loginResponse = loginConnection.execute()
            
            Log.d(TAG, "登录请求已发送，响应状态码: ${loginResponse.statusCode()}")
            System.out.println("JCU登录: 登录请求已发送，响应状态码: ${loginResponse.statusCode()}")
            
            // 保存登录响应Cookie
            val responseCookies = loginResponse.cookies()
            Log.d(TAG, "登录响应Cookie: $responseCookies")
            System.out.println("JCU登录: 登录响应Cookie: $responseCookies")
            
            // 合并所有cookie
            val allCookies = HashMap(initialCookies)
            if (responseCookies.isNotEmpty()) {
                allCookies.putAll(responseCookies)
                
                // 更新cookieManager
                responseCookies.forEach { (name, value) ->
                    val cookie = HttpCookie(name, value)
                    cookie.domain = "studentfirst.jcu.edu.sg"
                    cookie.path = "/"
                    cookieManager.cookieStore.add(URI("https://studentfirst.jcu.edu.sg"), cookie)
                }
            }
            
            // 分析响应状态 - 处理重定向
            val responseCode = loginResponse.statusCode()
            val redirectLocation = loginResponse.header("Location")
            
            // 检查是否是重定向（成功登录的标志之一）
            if (responseCode == 302 && redirectLocation != null) {
                Log.d(TAG, "登录后重定向到: $redirectLocation")
                System.out.println("JCU登录: 登录后重定向到: $redirectLocation")
                
                // 判断重定向目标是否表示登录成功
                if (redirectLocation.contains("main.xhtml") || redirectLocation.contains("dashboard.xhtml") || 
                    !redirectLocation.contains("login") || redirectLocation == "/" || redirectLocation.startsWith("http://studentfirst.jcu.edu.sg")) {
                    
                    // 提取Token - 如果存在于重定向URL中
                    val tokenRegex = Regex("jakarta\\.faces\\.Token=([^&]+)")
                    val tokenMatch = tokenRegex.find(redirectLocation)
                    if (tokenMatch != null && tokenMatch.groupValues.size > 1) {
                        val token = tokenMatch.groupValues[1]
                        val decodedToken = URLDecoder.decode(token, "UTF-8")
                        jsfToken = decodedToken
                        Log.d(TAG, "从重定向URL中提取到Token: $decodedToken")
                        System.out.println("JCU登录: 从重定向URL中提取到Token: $decodedToken")
                    }
                    
                    // 跟随重定向到主页
                    val redirectUrl = if (redirectLocation.startsWith("http")) {
                        redirectLocation
                    } else if (redirectLocation.startsWith("/")) {
                        "https://studentfirst.jcu.edu.sg$redirectLocation"
                    } else {
                        "https://studentfirst.jcu.edu.sg/$redirectLocation"
                    }
                    
                    System.out.println("JCU登录: 跟随重定向到: $redirectUrl")
                    
                    try {
                        val redirectResponse = Jsoup.connect(redirectUrl)
                            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                            .cookies(allCookies)
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                            .header("Accept-Language", "zh-CN,zh;q=0.9")
                            .timeout(30000)
                            .followRedirects(true)
                            .execute()
                        
                        val redirectDoc = redirectResponse.parse()
                        val redirectTitle = redirectDoc.title()
                        Log.d(TAG, "重定向页面标题: $redirectTitle")
                        System.out.println("JCU登录: 重定向页面标题: $redirectTitle")
                        
                        // 检查是否有菜单栏（登录成功的标志）
                        val menuBar = redirectDoc.select(".ui-menubar, #j_idt15, ul[role='menubar'], li.ui-menuitem").firstOrNull()
                        if (menuBar != null) {
                            Log.d(TAG, "在重定向页面中找到菜单栏，登录成功")
                            System.out.println("JCU登录: 在重定向页面中找到菜单栏，登录成功")
                            isLoggedIn = true
                            loginStatus = true
                            
                            // 从重定向页面提取新的ViewState/Token
                            val newViewStateInput = redirectDoc.select("input[name='jakarta.faces.ViewState']").firstOrNull()
                            if (newViewStateInput != null) {
                                val newViewState = newViewStateInput.attr("value")
                                jsfToken = newViewState
                                Log.d(TAG, "从重定向页面获取到新的ViewState: $newViewState")
                                System.out.println("JCU登录: 从重定向页面获取到新的ViewState: $newViewState")
                            }
                            
                            // 检查页面中的Token链接
                            redirectDoc.select("a[href*='jakarta.faces.Token=']").firstOrNull()?.let {
                                val href = it.attr("href")
                                val tokenRegex = Regex("jakarta\\.faces\\.Token=([^&]+)")
                                val tokenMatch = tokenRegex.find(href)
                                if (tokenMatch != null && tokenMatch.groupValues.size > 1) {
                                    val token = tokenMatch.groupValues[1]
                                    val decodedToken = URLDecoder.decode(token, "UTF-8")
                                    jsfToken = decodedToken
                                    Log.d(TAG, "从页面链接中提取到Token: $decodedToken")
                                    System.out.println("JCU登录: 从页面链接中提取到Token: $decodedToken")
                                }
                            }
                            
                            // 查找欢迎消息
                            val welcomeText = redirectDoc.select("span[title='welcome user'], .text-jcusemerald-700:contains(Welcome)").text()
                            if (welcomeText.isNotEmpty()) {
                                Log.d(TAG, "找到欢迎信息: $welcomeText")
                                System.out.println("JCU登录: 找到欢迎信息: $welcomeText")
                            }
                            
                            // 更新Cookie
                            val redirectCookies = redirectResponse.cookies()
                            if (redirectCookies.isNotEmpty()) {
                                redirectCookies.forEach { (name, value) ->
                                    val cookie = HttpCookie(name, value)
                                    cookie.domain = "studentfirst.jcu.edu.sg"
                                    cookie.path = "/"
                                    cookieManager.cookieStore.add(URI("https://studentfirst.jcu.edu.sg"), cookie)
                                }
                            }
                            
                            return@withContext LoginResult.Success(redirectDoc.html())
                        } 
                        
                        // 检查是否有欢迎信息
                        val welcomeMessage = redirectDoc.select("span[title='welcome user'], .text-center:contains(Welcome), .text-3xl:contains(Welcome)").text()
                        if (welcomeMessage.isNotEmpty() && !welcomeMessage.contains("Welcome Back to StudentFirst")) {
                            Log.d(TAG, "在重定向页面中找到个性化欢迎信息: $welcomeMessage")
                            System.out.println("JCU登录: 在重定向页面中找到个性化欢迎信息: $welcomeMessage")
                            isLoggedIn = true
                            loginStatus = true
                            return@withContext LoginResult.Success(redirectDoc.html())
                        }
                        
                        // 检查是否含有课程表
                        if (redirectDoc.select("div:contains(List of classes)").isNotEmpty() || 
                            redirectDoc.select("div:contains(Class Schedule)").isNotEmpty()) {
                            Log.d(TAG, "在重定向页面中找到课程表，登录成功")
                            System.out.println("JCU登录: 在重定向页面中找到课程表，登录成功")
                            isLoggedIn = true
                            loginStatus = true
                            return@withContext LoginResult.Success(redirectDoc.html())
                        }
                        
                        // 如果页面没有登录表单，也视为登录成功
                        if (redirectDoc.select("form:has(input[type=password])").isEmpty()) {
                            Log.d(TAG, "重定向页面没有登录表单，登录可能成功")
                            System.out.println("JCU登录: 重定向页面没有登录表单，登录可能成功")
                            isLoggedIn = true
                            loginStatus = true
                            return@withContext LoginResult.Success(redirectDoc.html())
                        }
                        
                        // 如果以上检查都未通过，尝试访问主页进一步确认
                        try {
                            val mainUrl = "https://studentfirst.jcu.edu.sg/faces/main.xhtml"
                            val mainResponse = Jsoup.connect(mainUrl)
                                .cookies(allCookies)
                                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                                .timeout(30000)
                                .followRedirects(true)
                                .execute()
                            
                            val mainDoc = mainResponse.parse()
                            
                            // 检查主页是否有菜单栏或课程表
                            if (mainDoc.select(".ui-menubar, #j_idt15, ul[role='menubar']").isNotEmpty() ||
                                mainDoc.select("div:contains(List of classes)").isNotEmpty() ||
                                mainDoc.select("form:has(input[type=password])").isEmpty()) {
                                
                                Log.d(TAG, "通过访问主页确认登录成功")
                                System.out.println("JCU登录: 通过访问主页确认登录成功")
                                isLoggedIn = true
                                loginStatus = true
                                return@withContext LoginResult.Success(mainDoc.html())
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "访问主页进一步确认时出错: ${e.message}")
                            System.out.println("JCU登录: 访问主页确认时出错: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "跟随重定向时出错: ${e.message}")
                        System.out.println("JCU登录: 跟随重定向时出错: ${e.message}")
                    }
                }
            } else if (responseCode == 200) {
                // 登录可能成功或失败，需要解析响应内容
                val resultDoc = loginResponse.parse()
                val pageTitle = resultDoc.title()
                Log.d(TAG, "登录响应页面标题: $pageTitle")
                System.out.println("JCU登录: 登录响应页面标题: $pageTitle")
                
                // 检查是否有错误信息
                val errorMessages = resultDoc.select(".ui-messages-error, .ui-message-error, [class*='error'], .text-red-500")
                if (errorMessages.isNotEmpty()) {
                    val errorText = errorMessages.text()
                    Log.d(TAG, "登录错误信息: $errorText")
                    System.out.println("JCU登录: 发现错误信息: $errorText")
                    return@withContext LoginResult.Error("登录失败: $errorText")
                }
                
                // 检查是否仍然是登录页面
                val loginFormStillPresent = resultDoc.select("form:has(input[type=password])").isNotEmpty()
                if (loginFormStillPresent) {
                    Log.d(TAG, "登录后仍然显示登录表单，登录失败")
                    System.out.println("JCU登录: 登录后仍显示登录表单，登录失败")
                    return@withContext LoginResult.Error("登录失败: 用户名或密码不正确")
                }
                
                // 如果没有登录表单，检查是否有菜单栏或欢迎信息
                val menuBar = resultDoc.select(".ui-menubar, #j_idt15, ul[role='menubar']").firstOrNull()
                val welcomeMessage = resultDoc.select("span[title='welcome user'], .text-center:contains(Welcome), .text-jcusemerald-700:contains(Welcome)").text()
                
                if (menuBar != null || (welcomeMessage.isNotEmpty() && !welcomeMessage.contains("Welcome Back to StudentFirst"))) {
                    Log.d(TAG, "登录成功，找到成功标志")
                    System.out.println("JCU登录: 登录成功，找到成功标志")
                    isLoggedIn = true
                    loginStatus = true
                    return@withContext LoginResult.Success(resultDoc.html())
                }
                
                // 如果仍然无法确定，尝试访问主页
                try {
                    val mainUrl = "https://studentfirst.jcu.edu.sg/faces/main.xhtml"
                    val mainResponse = Jsoup.connect(mainUrl)
                        .cookies(allCookies)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                        .timeout(30000)
                        .followRedirects(true)
                        .execute()
                    
                    val mainDoc = mainResponse.parse()
                    
                    // 检查主页是否有菜单栏或课程表
                    if (mainDoc.select(".ui-menubar, #j_idt15, ul[role='menubar']").isNotEmpty() ||
                        mainDoc.select("div:contains(List of classes)").isNotEmpty() ||
                        mainDoc.select("form:has(input[type=password])").isEmpty()) {
                        
                        Log.d(TAG, "通过访问主页确认登录成功")
                        System.out.println("JCU登录: 通过访问主页确认登录成功")
                        isLoggedIn = true
                        loginStatus = true
                        return@withContext LoginResult.Success(mainDoc.html())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "访问主页进一步确认时出错: ${e.message}")
                    System.out.println("JCU登录: 访问主页确认时出错: ${e.message}")
                }
            }
            
            // 如果所有检查都未能确认登录成功，返回错误
            Log.d(TAG, "登录失败，无法确定具体原因")
            System.out.println("JCU登录: 登录失败，无法确定具体原因")
            return@withContext LoginResult.Error("登录失败，请确认用户名和密码是否正确")
            
        } catch (e: IOException) {
            Log.e(TAG, "登录过程中发生IO异常: ${e.message}", e)
            System.out.println("JCU登录: IO异常: ${e.message}")
            return@withContext LoginResult.Error("登录过程中发生网络错误: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "登录过程中发生未知错误: ${e.message}", e)
            System.out.println("JCU登录: 未知错误: ${e.message}")
            return@withContext LoginResult.Error("登录过程中发生未知错误: ${e.message}")
        }
    }
    
    /**
     * 获取课程列表
     */
    suspend fun fetchClasses(force: Boolean = false): FetchResult = withContext(Dispatchers.IO) {
        if (!isLoggedIn && !loginStatus) {
            Log.e(TAG, "未登录，无法获取课程")
            return@withContext FetchResult.Error("未登录，无法获取课程")
        }
        
        try {
            // 获取所有可用的cookie
            val cookieString = buildCookieString()
            if (cookieString.isBlank()) {
                Log.e(TAG, "Cookie为空，无法获取课程")
                return@withContext FetchResult.Error("Cookie为空，无法获取课程")
            }
            
            Log.d(TAG, "获取课程信息使用的Cookie: $cookieString")
            System.out.println("JCU获取课程: 使用Cookie: $cookieString")
            
            // 尝试访问主页获取课程信息
            val mainUrl = "https://studentfirst.jcu.edu.sg/faces/main.xhtml"
            val mainUrlWithToken = if (jsfToken != null && jsfToken!!.isNotEmpty()) {
                val encodedToken = URLEncoder.encode(jsfToken, "UTF-8")
                "$mainUrl?jakarta.faces.Token=$encodedToken"
            } else {
                mainUrl
            }
            
            Log.d(TAG, "访问主页获取课程信息: $mainUrlWithToken")
            System.out.println("JCU获取课程: 访问URL: $mainUrlWithToken")
            
            // 使用Jsoup连接
            val connection = Jsoup.connect(mainUrlWithToken)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(30000)
                .followRedirects(true)
            
            // 添加所有Cookie
            val cookieStore = cookieManager.cookieStore
            val cookies = cookieStore.cookies
            val cookieMap = cookies.associate { it.name to it.value }
            connection.cookies(cookieMap)
            
            // 执行请求
            val response = connection.execute()
            
            val currentMainUrl = response.url().toString()
            Log.d(TAG, "访问主页结果 - URL: $currentMainUrl, 状态码: ${response.statusCode()}")
            System.out.println("JCU获取课程: 响应URL: $currentMainUrl, 状态码: ${response.statusCode()}")
            
            if (response.statusCode() != 200) {
                Log.e(TAG, "获取课程页面失败，状态码: ${response.statusCode()}")
                return@withContext FetchResult.Error("获取课程页面失败，状态码: ${response.statusCode()}")
            }
            
            // 更新Cookie，确保使用最新的会话信息
            val newCookies = response.cookies()
            if (newCookies.isNotEmpty()) {
                Log.d(TAG, "更新Cookie: $newCookies")
                System.out.println("JCU获取课程: 新Cookie: $newCookies")
                
                // 更新Cookie存储
                newCookies.forEach { (name, value) ->
                    val cookie = HttpCookie(name, value)
                    cookie.domain = "studentfirst.jcu.edu.sg"
                    cookie.path = "/"
                    cookieManager.cookieStore.add(URI("https://studentfirst.jcu.edu.sg"), cookie)
                }
            }
            
            // 解析HTML
            val html = response.body()
            val doc = Jsoup.parse(html)
            
            // 保存HTML到日志，便于调试
            System.out.println("JCU获取课程: 页面标题: ${doc.title()}")
            val chunkSize = 10000
            for ((index, i) in (0 until html.length step chunkSize).withIndex()) {
                val end = minOf(i + chunkSize, html.length)
                System.out.println("JCU获取课程HTML片段 #$index: ${html.substring(i, end)}")
            }
            
            // 尝试提取新的JSF Token
            doc.select("a[href*='jakarta.faces.Token=']").firstOrNull()?.let {
                val href = it.attr("href")
                val tokenRegex = Regex("jakarta\\.faces\\.Token=([^&]+)")
                val tokenMatch = tokenRegex.find(href)
                if (tokenMatch != null && tokenMatch.groupValues.size > 1) {
                    val token = tokenMatch.groupValues[1]
                    val decodedToken = URLDecoder.decode(token, "UTF-8")
                    jsfToken = decodedToken
                    Log.d(TAG, "从页面链接中提取到新Token: $decodedToken")
                    System.out.println("JCU获取课程: 更新Token: $decodedToken")
                }
            }
            
            // 检查是否有错误消息
            val errorMessages = doc.select(".ui-messages-error, .ui-message-error")
            if (errorMessages.isNotEmpty()) {
                val errorText = errorMessages.text()
                Log.e(TAG, "获取课程信息时出现错误: $errorText")
                return@withContext FetchResult.Error("获取课程信息时出现错误: $errorText")
            }
            
            // 首先尝试提取7天课程表
            var classes = extract7DayClasses(doc)
            
            // 如果没有找到，尝试从详细课程列表中提取
            if (classes.isEmpty()) {
                Log.d(TAG, "从7天课程表中未找到课程，尝试从详细课程列表中提取")
                System.out.println("JCU获取课程: 尝试从详细课程列表提取")
                classes = extractDetailedClasses(doc)
            }
            
            // 如果仍然没有找到，尝试从日期-时间格式提取
            if (classes.isEmpty()) {
                Log.d(TAG, "从详细课程列表中未找到课程，尝试从日期-时间格式提取")
                System.out.println("JCU获取课程: 尝试从日期-时间格式提取")
                classes = extractClassesFromDateTime(doc)
            }
            
            // 检查是否找到了课程
            if (classes.isEmpty()) {
                // 如果页面上仍然有登录表单，可能登录状态已失效
                if (doc.select("form:has(input[type=password])").isNotEmpty()) {
                    Log.e(TAG, "登录状态已失效，需要重新登录")
                    isLoggedIn = false
                    loginStatus = false
                    return@withContext FetchResult.Error("登录状态已失效，需要重新登录")
                }
                
                // 检查是否有明确的"没有课程"信息
                if (doc.select("div:containsOwn(暂无课程), div:containsOwn(No classes)").isNotEmpty()) {
                    Log.d(TAG, "系统明确提示没有课程")
                    return@withContext FetchResult.Success(emptyList())
                }
                
                // 尝试查找其他页面的链接
                Log.d(TAG, "尝试从菜单中获取其他页面链接")
                System.out.println("JCU获取课程: 尝试查找其他页面链接")
                
                val possibleLinks = listOf("Timetable", "Schedule", "Class", "课程表", "课表")
                for (linkText in possibleLinks) {
                    val links = doc.select("a:containsOwn($linkText)")
                    
                    for (link in links) {
                        val href = link.attr("href")
                        if (href.isEmpty()) continue
                        
                        val fullUrl = if (href.startsWith("http")) {
                            href
                        } else if (href.startsWith("/")) {
                            "https://studentfirst.jcu.edu.sg${href}"
                        } else {
                            "https://studentfirst.jcu.edu.sg/${href}"
                        }
                        
                        Log.d(TAG, "尝试访问: $fullUrl")
                        System.out.println("JCU获取课程: 尝试访问其他页面: $fullUrl")
                        
                        try {
                            // 创建连接
                            val linkConnection = Jsoup.connect(fullUrl)
                                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                                .header("Accept-Language", "zh-CN,zh;q=0.9")
                                .timeout(30000)
                                .followRedirects(true)
                            
                            // 使用最新的Cookie
                            val updatedCookieStore = cookieManager.cookieStore
                            val updatedCookies = updatedCookieStore.cookies
                            val updatedCookieMap = updatedCookies.associate { it.name to it.value }
                            linkConnection.cookies(updatedCookieMap)
                            
                            // 执行请求
                            val linkResponse = linkConnection.execute()
                            
                            // 更新Cookie
                            val linkCookies = linkResponse.cookies()
                            if (linkCookies.isNotEmpty()) {
                                linkCookies.forEach { (name, value) ->
                                    val cookie = HttpCookie(name, value)
                                    cookie.domain = "studentfirst.jcu.edu.sg"
                                    cookie.path = "/"
                                    cookieManager.cookieStore.add(URI("https://studentfirst.jcu.edu.sg"), cookie)
                                }
                            }
                            
                            val linkDoc = linkResponse.parse()
                            Log.d(TAG, "${linkText}页面标题: ${linkDoc.title()}")
                            System.out.println("JCU获取课程: ${linkText}页面标题: ${linkDoc.title()}")
                            
                            // 尝试从新页面提取课程信息
                            val newClasses = extract7DayClasses(linkDoc)
                            if (newClasses.isNotEmpty()) {
                                classes = newClasses
                                Log.d(TAG, "从${linkText}页面成功提取到${classes.size}个课程")
                                System.out.println("JCU获取课程: 从${linkText}页面提取到${classes.size}个课程")
                                break
                            }
                            
                            val detailedClasses = extractDetailedClasses(linkDoc)
                            if (detailedClasses.isNotEmpty()) {
                                classes = detailedClasses
                                Log.d(TAG, "从${linkText}页面成功提取到${classes.size}个课程")
                                System.out.println("JCU获取课程: 从${linkText}页面提取到${classes.size}个课程")
                                break
                            }
                            
                            val dateTimeClasses = extractClassesFromDateTime(linkDoc)
                            if (dateTimeClasses.isNotEmpty()) {
                                classes = dateTimeClasses
                                Log.d(TAG, "从${linkText}页面成功提取到${classes.size}个课程")
                                System.out.println("JCU获取课程: 从${linkText}页面提取到${classes.size}个课程")
                                break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "访问${linkText}页面时出错: ${e.message}")
                            System.out.println("JCU获取课程: 访问${linkText}页面出错: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    
                    if (classes.isNotEmpty()) break
                }
            }
            
            // 返回结果
            Log.d(TAG, "成功提取到${classes.size}个课程")
            System.out.println("JCU获取课程: 成功提取到${classes.size}个课程")
            
            // 提取出勤率信息
            val (classAttendanceRate, campusAttendanceRate) = extractAttendanceRates(doc)
            Log.d(TAG, "课程出勤率: $classAttendanceRate, 校园出勤率: $campusAttendanceRate")
            System.out.println("JCU获取课程: 课程出勤率: $classAttendanceRate, 校园出勤率: $campusAttendanceRate")
            
            // 对课程去重
            val uniqueClasses = removeDuplicateClasses(classes)
            Log.d(TAG, "去重后课程数: ${uniqueClasses.size}")
            System.out.println("JCU获取课程: 去重后课程数: ${uniqueClasses.size}")
            
            return@withContext FetchResult.Success(
                classes = uniqueClasses,
                classAttendanceRate = classAttendanceRate,
                campusAttendanceRate = campusAttendanceRate
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取课程数据时出错: ${e.message}")
            e.printStackTrace()
            return@withContext FetchResult.Error("获取课程数据时出错: ${e.message}")
        }
    }
    
    /**
     * 从7天课程表格提取课程数据 (多种选择器)
     */
    private fun extract7DayClasses(doc: Document): List<JcuClass> {
        val classes = mutableListOf<JcuClass>()
        
        try {
            // 尝试多种可能的课程表选择器
            val tableSelectors = listOf(
                "div#j_idt43\\:sf_datatable_sevendaychedule table", // 固定选择器
                "div[id$='_sevendaychedule'] table", // 以_sevendaychedule结尾的div中的表格
                "div[id*='chedule'] table", // 包含chedule的div中的表格
                "table.ui-datatable-data", // 数据表格通用类
                "table.dataTable" // 另一种可能的表格类
            )
            
            var sevendayTable: org.jsoup.select.Elements? = null
            
            // 尝试所有可能的选择器，直到找到表格
            for (selector in tableSelectors) {
                val table = doc.select(selector)
                if (table.isNotEmpty()) {
                    Log.d(TAG, "使用选择器'$selector'找到7天课程表格")
                    sevendayTable = table
                    break
                }
            }
            
            // 如果仍然找不到，尝试查找任何可能包含课程信息的表格
            if (sevendayTable == null || sevendayTable.isEmpty()) {
                // 查找所有表格
                val allTables = doc.select("table")
                Log.d(TAG, "未找到7天课程表格，尝试从${allTables.size}个表格中寻找")
                
                // 尝试识别可能的课程表 - 寻找特征列如日期、时间、地点等
                for (table in allTables) {
                    val headers = table.select("th, td[class*='header']").map { it.text().toLowerCase() }
                    
                    // 检查表头是否包含课程相关关键词
                    val hasCourseInfo = headers.any { it.contains("course") || it.contains("class") || it.contains("code") }
                    val hasDateInfo = headers.any { it.contains("date") || it.contains("day") }
                    val hasTimeInfo = headers.any { it.contains("time") || it.contains("hour") }
                    val hasLocationInfo = headers.any { it.contains("location") || it.contains("room") || it.contains("venue") }
                    
                    if ((hasCourseInfo && hasDateInfo) || (hasDateInfo && hasTimeInfo)) {
                        Log.d(TAG, "找到潜在的课程表: ${headers.joinToString()}")
                        sevendayTable = org.jsoup.select.Elements(table)
                        break
                    }
                }
            }
            
            if (sevendayTable == null || sevendayTable.isEmpty()) {
                Log.d(TAG, "无法找到7天课程表格")
                return classes
            }
            
            // 获取表格中的所有行
            val rows = sevendayTable.select("tbody tr")
            Log.d(TAG, "7天课程表格行数：${rows.size}")
            
            // 如果没有行，可能表格结构不同
            if (rows.isEmpty()) {
                val alternativeRows = sevendayTable.select("tr")
                if (alternativeRows.size > 1) { // 跳过表头
                    Log.d(TAG, "使用替代方式找到的行数：${alternativeRows.size - 1}")
                    for (i in 1 until alternativeRows.size) { // 跳过第一行（表头）
                        val row = alternativeRows[i]
                        processTableRow(row, doc, classes)
                    }
                }
            } else {
                // 正常处理找到的表格行
                for (row in rows) {
                    processTableRow(row, doc, classes)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取7天课程数据时出错: ${e.message}")
            e.printStackTrace()
        }
        
        return classes
    }
    
    /**
     * 处理表格行并提取课程信息
     */
    private fun processTableRow(row: Element, doc: Document, classes: MutableList<JcuClass>) {
        try {
            val cells = row.select("td")
            if (cells.size < 3) return // 至少需要课程代码、日期和时间
            
            // 尝试确定各列的数据
            var courseCode = ""
            var dateStr = ""
            var timeRange = ""
            var location = ""
            
            // 第一种情况：标准格式 [课程代码, 日期, 时间, 地点]
            if (cells.size >= 4) {
                courseCode = cells[0].text().trim()
                dateStr = cells[1].text().trim()
                timeRange = cells[2].text().trim()
                location = cells[3].text().trim()
            } 
            // 第二种情况：三列 [课程代码, 日期和时间组合, 地点]
            else if (cells.size == 3) {
                courseCode = cells[0].text().trim()
                val dateTimeCell = cells[1].text().trim()
                location = cells[2].text().trim()
                
                // 尝试从组合的日期时间中分离
                val parts = dateTimeCell.split(" ", limit = 2)
                if (parts.size == 2) {
                    dateStr = parts[0]
                    timeRange = parts[1]
                } else {
                    // 无法分离，可能格式不正确
                    Log.e(TAG, "无法从'$dateTimeCell'中分离日期和时间")
                    return
                }
            } else {
                Log.e(TAG, "无法识别的表格行格式，单元格数: ${cells.size}")
                return
            }
            
            // 过滤掉明显不是课程的行
            if (courseCode.isEmpty() || dateStr.isEmpty() || timeRange.isEmpty()) {
                return
            }
            
            Log.d(TAG, "解析7天课程行: $courseCode | $dateStr | $timeRange | $location")
            
            try {
                val date = parseDate(dateStr)
                val (startTime, endTime) = parseTimeRange(timeRange, date)
                
                // 查找对应的课程名称
                val courseName = getCourseNameFromCode(doc, courseCode) ?: courseCode
                
                val jcuClass = JcuClass(
                    courseCode = courseCode,
                    courseName = courseName,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    status = JcuClassStatus.UPCOMING
                )
                
                classes.add(jcuClass)
                Log.d(TAG, "从7天课程表格添加课程: $courseCode - $courseName - $dateStr - $timeRange - $location")
            } catch (e: Exception) {
                Log.e(TAG, "解析7天课程表格日期时间时出错: ${e.message}")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理表格行时出错: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 从日期-时间格式提取课程信息 (基于Notepad中提供的HTML示例)
     */
    private fun extractClassesFromDateTime(doc: Document): List<JcuClass> {
        val classes = mutableListOf<JcuClass>()
        
        try {
            // 查找所有课程项 - 按照Python脚本中的逻辑
            val courseElements = doc.select("li.flex.align-items-center.mb-3.ml-3")
            
            for (courseElement in courseElements) {
                // 获取课程标题
                val titleElement = courseElement.previousElementSibling()
                    ?.previousElementSibling()
                    ?.select("span.text-900.font-medium.text-xl.m-2")
                    ?.firstOrNull()
                
                if (titleElement == null) continue
                val courseTitle = titleElement.text().trim()
                
                // 获取课程代码
                val codeElement = courseElement.previousElementSibling()
                    ?.select("span.text-600.text-l.mb-2")
                    ?.firstOrNull()
                
                if (codeElement == null) continue
                val courseCode = codeElement.text().trim()
                
                // 获取课程详情
                val detailsDiv = courseElement.select("div.flex.flex-wrap").firstOrNull()
                if (detailsDiv == null) continue
                
                val detailLabels = detailsDiv.select("label.ui-outputlabel.ui-widget")
                
                for (label in detailLabels) {
                    val detailDiv = label.select("div.border-round-lg").firstOrNull()
                    if (detailDiv == null) continue
                    
                    // 提取div中的文本
                    val divHtml = detailDiv.html()
                    val parts = divHtml.split("<br>")
                    
                    if (parts.size >= 3) {
                        val dateStr = parts[0].trim()
                        val timeRange = parts[1].trim()
                        val location = parts[2].trim()
                        
                        Log.d(TAG, "解析时间框: $dateStr | $timeRange | $location")
                        
                        // 解析状态
                        val status = when {
                            detailDiv.hasClass("border-green-500") -> JcuClassStatus.COMPLETED
                            detailDiv.hasClass("border-red-500") -> JcuClassStatus.ABSENT
                            detailDiv.hasClass("border-white-500") && detailDiv.hasClass("bg-gray-700") -> JcuClassStatus.UPCOMING
                            else -> JcuClassStatus.PLANNED
                        }
                        
                        try {
                            val date = parseDate(dateStr)
                            val (startTime, endTime) = parseTimeRange(timeRange, date)
                            
                            val jcuClass = JcuClass(
                                courseCode = courseCode,
                                courseName = courseTitle,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                status = status
                            )
                            
                            classes.add(jcuClass)
                            Log.d(TAG, "从日期-时间格式添加了课程: $courseCode - $courseTitle - $dateStr - $timeRange - $location - $status")
                        } catch (e: Exception) {
                            Log.e(TAG, "解析日期-时间格式时出错: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "从日期-时间格式提取课程数据时出错: ${e.message}")
            e.printStackTrace()
        }
        
        return classes
    }
    
    /**
     * 获取课程名称
     */
    private fun getCourseNameFromCode(doc: Document, courseCode: String): String? {
        // 首先从课程代码中提取前缀部分（例如从CP3406-LA提取CP3406）
        val codePrefix = courseCode.split("-").firstOrNull() ?: courseCode
        
        // 方法1：尝试从详细课程列表中查找名称
        val courseItems = doc.select("li.flex.align-items-center.mb-1")
        
        for (courseItem in courseItems) {
            val titleSpan = courseItem.select("span.text-900.font-medium.text-xl.m-2").firstOrNull()
                ?: continue
            
            val courseTitle = titleSpan.text().trim()
            
            // 获取下一个元素，应该包含课程代码
            val codeItem = courseItem.nextElementSibling() ?: continue
            val codeSpan = codeItem.select("span.text-600.text-l.mb-2").firstOrNull() ?: continue
            
            val itemCode = codeSpan.text().trim()
            val itemCodePrefix = itemCode.split("-").firstOrNull() ?: itemCode
            
            // 如果找到匹配的课程代码，返回课程名称
            if (itemCode == courseCode || itemCodePrefix == codePrefix) {
                return courseTitle
            }
        }
        
        // 方法2：尝试从列表提取课程名称
        val courseTitles = doc.select("span.text-900.font-medium.text-xl.m-2")
        for (title in courseTitles) {
            val text = title.text().trim()
            // 查找形如"CP3406 - Mobile Computing - Lecture"的标题
            if (text.startsWith(codePrefix)) {
                return text
            }
        }
        
        return null
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String): LocalDate {
        Log.d(TAG, "尝试解析日期: $dateStr")
        
        try {
            // 处理不同的日期格式 "dd-MMM" 或 "MMM dd, yy"
            if (dateStr.contains("-")) {
                // 处理 "dd-MMM" 格式，添加当前年份
                val format = DateTimeFormatter.ofPattern("dd-MMM[-yyyy]", Locale.ENGLISH)
                val dateWithYear = if (dateStr.count { it == '-' } < 2) {
                    "$dateStr-${LocalDate.now().year}"
                } else {
                    dateStr
                }
                return LocalDate.parse(dateWithYear, format)
            } else if (dateStr.contains(",")) {
                // 处理 "MMM dd, yy" 格式 (例如 "Apr 02, 25")
                val format = DateTimeFormatter.ofPattern("MMM dd[,][ ]yy", Locale.ENGLISH)
                var parsedDate = LocalDate.parse(dateStr, format)
                
                // 处理两位数年份
                if (parsedDate.year < 100) {
                    parsedDate = parsedDate.withYear(parsedDate.year + 2000)
                }
                
                return parsedDate
            } else {
                throw IllegalArgumentException("未知日期格式: $dateStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析日期失败: $dateStr, 错误: ${e.message}")
            // 解析失败时返回当前日期
            return LocalDate.now()
        }
    }
    
    /**
     * 解析时间范围
     */
    private fun parseTimeRange(timeRange: String, date: LocalDate): Pair<LocalDateTime, LocalDateTime> {
        Log.d(TAG, "尝试解析时间范围: $timeRange")
        
        try {
            // 处理不同的时间格式 "HH:MM-HH:MM" 或 "HHMM-HHMM"
            if (timeRange.contains(":")) {
                // 处理 "HH:MM-HH:MM" 格式
                val times = timeRange.split("-")
                if (times.size != 2) throw IllegalArgumentException("无效的时间范围格式: $timeRange")
                
                val startTimeParts = times[0].split(":")
                val endTimeParts = times[1].split(":")
                
                if (startTimeParts.size != 2 || endTimeParts.size != 2) {
                    throw IllegalArgumentException("无效的时间格式: $timeRange")
                }
                
                val startHour = startTimeParts[0].toInt()
                val startMinute = startTimeParts[1].toInt()
                val endHour = endTimeParts[0].toInt()
                val endMinute = endTimeParts[1].toInt()
                
                val startTime = LocalDateTime.of(date, LocalTime.of(startHour, startMinute))
                val endTime = LocalDateTime.of(date, LocalTime.of(endHour, endMinute))
                
                return Pair(startTime, endTime)
            } else {
                // 处理 "HHMM-HHMM" 格式
                val times = timeRange.split("-")
                if (times.size != 2) throw IllegalArgumentException("无效的时间范围格式: $timeRange")
                
                val startTimeStr = times[0]
                val endTimeStr = times[1]
                
                if (startTimeStr.length != 4 || endTimeStr.length != 4) {
                    throw IllegalArgumentException("无效的时间格式，需要4位数字: $timeRange")
                }
                
                val startHour = startTimeStr.substring(0, 2).toInt()
                val startMinute = startTimeStr.substring(2, 4).toInt()
                val endHour = endTimeStr.substring(0, 2).toInt()
                val endMinute = endTimeStr.substring(2, 4).toInt()
                
                val startTime = LocalDateTime.of(date, LocalTime.of(startHour, startMinute))
                val endTime = LocalDateTime.of(date, LocalTime.of(endHour, endMinute))
                
                return Pair(startTime, endTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析时间范围失败: $timeRange, 错误: ${e.message}")
            // 解析失败时返回当前时间和一小时后
            val now = LocalDateTime.now()
            return Pair(now, now.plusHours(1))
        }
    }
    
    /**
     * 构建Cookie字符串
     */
    private fun buildCookieString(): String {
        val cookieStore = cookieManager.cookieStore
        val cookies = cookieStore.cookies
        
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }
    
    /**
     * 清除所有Cookie
     */
    fun clearCookies() {
        cookieManager.cookieStore.removeAll()
        isLoggedIn = false
    }
    
    /**
     * 登录结果
     */
    sealed class LoginResult {
        data class Success(val html: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
    
    /**
     * 获取结果
     */
    sealed class FetchResult {
        data class Success(
            val classes: List<JcuClass>,
            val classAttendanceRate: Float = 0f,
            val campusAttendanceRate: Float = 0f
        ) : FetchResult()
        data class Error(val message: String) : FetchResult()
    }

    /**
     * 在登录成功时保存令牌
     */
    private fun saveToken(html: String) {
        // 尝试从超链接中提取Token
        val regex = Regex("jakarta\\.faces\\.Token=([^\"&]+)")
        val matchResult = regex.find(html)
        if (matchResult != null && matchResult.groupValues.size > 1) {
            jsfToken = matchResult.groupValues[1]
            Log.d(TAG, "从HTML中提取到Token: $jsfToken")
            return
        }
        
        // 尝试从表单中提取ViewState
        val viewStateRegex = Regex("name=\"jakarta\\.faces\\.ViewState\" .* value=\"([^\"]+)\"")
        val viewStateMatch = viewStateRegex.find(html)
        if (viewStateMatch != null && viewStateMatch.groupValues.size > 1) {
            val viewState = viewStateMatch.groupValues[1]
            Log.d(TAG, "从HTML中提取到ViewState: $viewState (长度: ${viewState.length})")
            return
        }
        
        Log.d(TAG, "未能从HTML中提取到Token或ViewState")
    }

    /**
     * 从详细课程列表提取课程信息
     */
    private fun extractDetailedClasses(doc: Document): List<JcuClass> {
        val classes = mutableListOf<JcuClass>()
        
        try {
            // 查找可能包含课程信息的容器
            System.out.println("JCU提取课程: 开始从详细列表提取课程")
            
            // 1. 首先查找所有课程项
            val courseItems = doc.select("li.flex.align-items-center.mb-1")
            System.out.println("JCU提取课程: 找到${courseItems.size}个课程项")
            
            if (courseItems.isEmpty()) {
                // 尝试其他选择器
                val alternativeCourseItems = doc.select("div.shadow-2 li, div.surface-card li.flex")
                if (alternativeCourseItems.isNotEmpty()) {
                    System.out.println("JCU提取课程: 找到${alternativeCourseItems.size}个备选课程项")
                }
            }
            
            // 2. 处理找到的每个课程项
            for (courseItem in courseItems) {
                // 获取课程标题/名称
                val titleSpan = courseItem.select("span.text-900, span.font-medium, span.text-xl").firstOrNull()
                if (titleSpan == null) continue
                
                val courseName = titleSpan.text().trim()
                System.out.println("JCU提取课程: 处理课程 - $courseName")
                
                // 提取课程代码
                var courseCode = ""
                
                // 尝试从课程名称中提取代码 (例如 "CP3406 - Mobile Computing - Lecture")
                val codeMatch = "\\b([A-Z]{2,}\\d{4})\\b".toRegex().find(courseName)
                if (codeMatch != null) {
                    courseCode = codeMatch.groupValues[1]
                }
                
                // 如果未能从标题中提取，尝试查找下一个元素中的代码
                if (courseCode.isEmpty()) {
                    val nextElement = courseItem.nextElementSibling()
                    if (nextElement != null) {
                        val codeSpan = nextElement.select("span.text-600, span.text-l").firstOrNull()
                        if (codeSpan != null) {
                            courseCode = codeSpan.text().trim()
                        }
                    }
                }
                
                if (courseCode.isEmpty()) {
                    System.out.println("JCU提取课程: 无法为'$courseName'找到课程代码，尝试继续")
                    continue
                }
                
                // 查找包含课程详情的手风琴元素
                var detailsElement = courseItem.nextElementSibling()?.nextElementSibling()
                var detailsDiv: Element? = null
                
                if (detailsElement != null) {
                    // 查找手风琴或下拉内容区域
                    detailsDiv = detailsElement.select("div.ui-accordion-content, div[role=tabpanel], div.flex.flex-wrap").firstOrNull()
                }
                
                if (detailsDiv == null) {
                    // 尝试对整个文档执行更广泛的搜索
                    val allDetailDivs = doc.select("div:has(div.flex.flex-wrap), div.ui-accordion-content:has(label)")
                    for (div in allDetailDivs) {
                        // 检查是否与当前课程相关
                        val nearbyHeaders = div.parents().select("span:contains($courseCode)")
                        if (nearbyHeaders.isNotEmpty()) {
                            detailsDiv = div
                            break
                        }
                    }
                }
                
                if (detailsDiv == null) {
                    System.out.println("JCU提取课程: 无法为'$courseCode'找到详情区域")
                    continue
                }
                
                // 3. 提取课程时间框
                val timeBoxes = detailsDiv.select("div.border-round-lg, div[class*=border-]:not(.ui-accordion)")
                System.out.println("JCU提取课程: '$courseCode'找到${timeBoxes.size}个时间框")
                
                for (timeBox in timeBoxes) {
                    // 提取时间框内的文本
                    val boxText = timeBox.html()
                    val boxTextLines = boxText.split("<br>", "<br />", limit = 5)
                    
                    if (boxTextLines.size < 3) {
                        System.out.println("JCU提取课程: 时间框文本格式不正确: $boxText")
                        continue
                    }
                    
                    // 提取日期、时间和地点
                    val dateStr = boxTextLines[0].trim()
                    val timeRange = boxTextLines[1].trim()
                    val location = boxTextLines[2].trim()
                    
                    System.out.println("JCU提取课程: 解析时间框 - $dateStr | $timeRange | $location")
                    
                    // 确定课程状态
                    val status = when {
                        timeBox.hasClass("border-green-500") -> JcuClassStatus.COMPLETED
                        timeBox.hasClass("border-red-500") -> JcuClassStatus.ABSENT
                        timeBox.hasClass("border-white-500") || timeBox.hasClass("bg-gray-700") -> JcuClassStatus.UPCOMING
                        else -> JcuClassStatus.PLANNED
                    }
                    
                    try {
                        // 解析日期和时间
                        val date = parseDate(dateStr)
                        val (startTime, endTime) = parseTimeRange(timeRange, date)
                        
                        val jcuClass = JcuClass(
                            courseCode = courseCode,
                            courseName = courseName,
                            startTime = startTime,
                            endTime = endTime,
                            location = location,
                            status = status
                        )
                        
                        classes.add(jcuClass)
                        System.out.println("JCU提取课程: 添加课程 - $courseCode | $courseName | $dateStr | $timeRange | $location | $status")
                    } catch (e: Exception) {
                        System.out.println("JCU提取课程: 解析时间失败 - ${e.message}")
                        Log.e(TAG, "解析课程日期时间时出错: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            
            // 4. 如果未找到课程，尝试从7天课程表格中提取
            if (classes.isEmpty()) {
                System.out.println("JCU提取课程: 从详细列表未找到课程，尝试从7天表格提取")
                
                // 尝试查找7天课程表
                val sevenDayTable = doc.select("table:has(td[role=gridcell]), table.ui-datatable-data").firstOrNull()
                
                if (sevenDayTable != null) {
                    val rows = sevenDayTable.select("tbody tr")
                    System.out.println("JCU提取课程: 找到7天表格，包含${rows.size}行")
                    
                    for (row in rows) {
                        val cells = row.select("td")
                        if (cells.size < 4) continue
                        
                        val courseCode = cells[0].text().trim()
                        val dateStr = cells[1].text().trim()
                        val timeRange = cells[2].text().trim()
                        val location = cells[3].text().trim()
                        
                        System.out.println("JCU提取课程: 从表格行解析 - $courseCode | $dateStr | $timeRange | $location")
                        
                        try {
                            val date = parseDate(dateStr)
                            val (startTime, endTime) = parseTimeRange(timeRange, date)
                            
                            // 尝试查找完整课程名称
                            val courseName = getCourseNameFromCode(doc, courseCode) ?: courseCode
                            
                            val jcuClass = JcuClass(
                                courseCode = courseCode,
                                courseName = courseName,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                status = JcuClassStatus.UPCOMING
                            )
                            
                            classes.add(jcuClass)
                            System.out.println("JCU提取课程: 从表格添加课程 - $courseCode | $courseName | $dateStr | $timeRange | $location")
                        } catch (e: Exception) {
                            System.out.println("JCU提取课程: 解析表格行时间失败 - ${e.message}")
                            Log.e(TAG, "解析表格日期时间时出错: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.out.println("JCU提取课程: 提取详细课程时出错 - ${e.message}")
            Log.e(TAG, "提取详细课程时出错: ${e.message}")
            e.printStackTrace()
        }
        
        System.out.println("JCU提取课程: 完成提取，找到${classes.size}个课程")
        return classes
    }

    /**
     * 获取当前Cookie字符串
     */
    private fun getCookieString(): String {
        return buildCookieString()
    }
    
    /**
     * 从主页提取出勤率信息
     */
    private fun extractAttendanceRates(doc: Document): Pair<Float, Float> {
        var classAttendanceRate = 0f
        var campusAttendanceRate = 0f
        
        try {
            // 查找课程出勤率 - 尝试多种选择器
            val classAttendanceSelectors = listOf(
                "span.text-green-600.text-2xl:has(~ span.font-medium.text-600:contains(Your overall class attendance))",
                "span.text-green-600.text-2xl:has(~ span:contains(class attendance))",
                "span.font-bold.text-green-600.text-2xl",
                "span.ml-1.font-bold.text-green-600.text-2xl",
                "div:contains(class attendance) span.text-green-600, div:contains(class attendance) span.text-2xl"
            )
            
            for (selector in classAttendanceSelectors) {
                val element = doc.select(selector).firstOrNull()
                if (element != null) {
                    val rateText = element.text().trim()
                    // 尝试提取百分比数字（例如从"94.83%"提取"94.83"）
                    val percentRegex = Regex("([\\d.]+)%?")
                    val match = percentRegex.find(rateText)
                    if (match != null) {
                        classAttendanceRate = match.groupValues[1].toFloatOrNull() ?: 0f
                        // 转换为0-1之间的小数
                        classAttendanceRate /= 100f
                        Log.d(TAG, "使用选择器[$selector]提取到课程出勤率: $classAttendanceRate")
                        break
                    }
                }
            }
            
            // 查找校园出勤率 - 尝试多种选择器
            val campusAttendanceSelectors = listOf(
                "span.text-green-600.text-2xl:has(~ span.font-medium.text-600:contains(Your overall campus attendance))",
                "span.text-green-600.text-2xl:has(~ span:contains(campus attendance))",
                "div:contains(campus attendance) span.text-green-600, div:contains(campus attendance) span.text-2xl"
            )
            
            for (selector in campusAttendanceSelectors) {
                val element = doc.select(selector).firstOrNull()
                if (element != null) {
                    val rateText = element.text().trim()
                    // 尝试提取百分比数字
                    val percentRegex = Regex("([\\d.]+)%?")
                    val match = percentRegex.find(rateText)
                    if (match != null) {
                        campusAttendanceRate = match.groupValues[1].toFloatOrNull() ?: 0f
                        // 转换为0-1之间的小数
                        campusAttendanceRate /= 100f
                        Log.d(TAG, "使用选择器[$selector]提取到校园出勤率: $campusAttendanceRate")
                        break
                    }
                }
            }
            
            // 如果还未找到，尝试查找任何包含出勤率百分比的元素
            if (classAttendanceRate == 0f || campusAttendanceRate == 0f) {
                // 查找所有可能包含百分比的span
                val percentSpans = doc.select("span:containsOwn(%)")
                for (span in percentSpans) {
                    val text = span.text().trim()
                    val percentValue = extractPercentage(text)
                    
                    if (percentValue > 0f) {
                        // 检查附近的文本，判断是哪种出勤率
                        val parentText = span.parent()?.text()?.toLowerCase() ?: ""
                        
                        if (classAttendanceRate == 0f && (parentText.contains("class") || parentText.contains("课程"))) {
                            classAttendanceRate = percentValue / 100f
                            Log.d(TAG, "从百分比span提取到课程出勤率: $classAttendanceRate")
                        } else if (campusAttendanceRate == 0f && (parentText.contains("campus") || parentText.contains("校园"))) {
                            campusAttendanceRate = percentValue / 100f
                            Log.d(TAG, "从百分比span提取到校园出勤率: $campusAttendanceRate")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取出勤率信息时出错: ${e.message}")
            e.printStackTrace()
        }
        
        return Pair(classAttendanceRate, campusAttendanceRate)
    }
    
    /**
     * 从文本中提取百分比值
     */
    private fun extractPercentage(text: String): Float {
        val regex = Regex("([\\d.]+)%")
        val match = regex.find(text)
        return if (match != null) {
            match.groupValues[1].toFloatOrNull() ?: 0f
        } else {
            0f
        }
    }

    /**
     * 去除重复的课程
     */
    private fun removeDuplicateClasses(classes: List<JcuClass>): List<JcuClass> {
        // 使用一个Map来保存不重复的课程
        val uniqueClassesMap = mutableMapOf<String, JcuClass>()
        
        for (jcuClass in classes) {
            // 创建一个唯一标识符，基于课程代码、开始时间和结束时间
            val key = "${jcuClass.courseCode}_${jcuClass.startTime}_${jcuClass.endTime}_${jcuClass.location}"
            
            // 如果该课程已存在，只有在状态为COMPLETED或ABSENT时才替换
            // 这确保我们保留带有出勤状态的课程
            if (!uniqueClassesMap.containsKey(key) || 
                jcuClass.status == JcuClassStatus.COMPLETED || 
                jcuClass.status == JcuClassStatus.ABSENT) {
                uniqueClassesMap[key] = jcuClass
            }
        }
        
        return uniqueClassesMap.values.toList()
    }
} 