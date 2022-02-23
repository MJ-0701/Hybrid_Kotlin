package com.dealer.allcar

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.dealer.allcar.library.AppIconNameChanger
import com.example.hybrid_kotlin.WebViewInterface
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.iid.FirebaseInstanceId
import com.kakao.ad.tracker.KakaoAdTracker.getInstance
import com.kakao.ad.tracker.KakaoAdTracker.isInitialized
import com.kakao.sdk.user.UserApiClient
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {




    private var webView: WebView? = null

    private val TAG = "MainActivity"

    private val url = "테스트 URL"

    private val TYPE_IMAGE = "image/*"
    private val INPUT_FILE_REQUEST_CODE = 1

    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mFilePathCallback: ValueCallback<Array<Uri?>?>? = null
    private var mCameraPhotoPath: String? = null

    var mContent: Context? = null
    var actFlag = false

    private var gdata: GlobalApplication? = null
    private var _firstFlag = true
    private val REQ_CODE = 111


    private var mWebViewInterface: WebViewInterface? = null
    private var mBackTime : Long = 0
    private var historyList : WebBackForwardList? = null

    private fun getHashKey() {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        if (packageInfo == null) Log.e("KeyHash", "KeyHash:null")
        for (signature in packageInfo!!.signatures) {
            try {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            } catch (e: NoSuchAlgorithmException) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=$signature", e)
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawActivity()
        //makeRecommandLink();
        //handleRecommandLink();
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean { // 백버튼 동작


       if (keyCode == KeyEvent.KEYCODE_BACK) {
           if(System.currentTimeMillis() > mBackTime + 1000){
               onBackPressed()
               return true
           }
        }
        return super.onKeyDown(keyCode, event)
    }




    fun isDarkUiMode(configuration: Configuration): Boolean {
        return configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun isDarkMode(activity: Activity): Boolean {
        return activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun drawActivity() {
        setContentView(R.layout.activity_main)
        gdata = application as GlobalApplication
        actFlag = true
        mContent = this
        webView = findViewById<View>(R.id.webView) as WebView
        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.domStorageEnabled = true
        webView!!.settings.setSupportMultipleWindows(true)
        webView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        webView!!.settings.textZoom = 100
        webView!!.settings.allowFileAccess = false
        webView!!.settings.allowFileAccessFromFileURLs = false
        webView!!.settings.allowUniversalAccessFromFileURLs = false
        mWebViewInterface = WebViewInterface(this@MainActivity, webView)
        webView!!.addJavascriptInterface(mWebViewInterface!!, "Android")
        webView!!.addJavascriptInterface(iconChanger(), "IconChanger")
        val intent = intent
        val bundle = intent.extras
        getHashKey()
        var webViewURL: String? = url
        if (bundle != null) {
            if (bundle.getString("url") != null && !bundle.getString("url")
                    .equals("", ignoreCase = true)
            ) {
                webViewURL = bundle.getString("url")
            }
        }
        webView!!.loadUrl(webViewURL!!)
        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val newWebView = WebView(this@MainActivity)
                val webSettings = newWebView.settings
                webSettings.javaScriptEnabled = true
                val dialog = Dialog(this@MainActivity)
                dialog.setContentView(newWebView)
                dialog.show()
                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        dialog.dismiss()
                    }
                }
                (resultMsg.obj as WebViewTransport).webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }
        webView!!.webViewClient = WebViewClientClass()
        webView!!.settings.loadWithOverviewMode = true
        webView!!.settings.useWideViewPort = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this)
        }
    }

    override fun onBackPressed() {

        val mHome : String = "http://test.modeal.net/m/page/home/index.php"
        val hUrl : String = "/m/page/home/index.php"

        var curTime = System.currentTimeMillis()
        var gapTime = curTime - mBackTime

        if( webView!!.canGoBack() ){
            Log.e("뒤로가기","체크")
            webView!!.goBack()

            if(webView!!.url.toString().contains(hUrl)) {
                Log.e("mBack",(mBackTime).toString())
                Log.e("sysss",System.currentTimeMillis().toString())
                mBackTime = curTime // 홈화면으로 오면 mBack 에 Sys 타임 대입

                if(0 <= gapTime && 2000 >=gapTime){ // 2초 이내 다시 누를시 종료
                    AlertDialog.Builder(this)
                        .setTitle("프로그램 종료")
                        .setMessage("프로그램을 종료하시겠습니까?")
                        .setPositiveButton(
                            "예"
                        ) { dialog, which -> Process.killProcess(Process.myPid()) }
                        .setNegativeButton("아니오", null).show()
                }

         }
        }else{ // 홈화면에서도 webView!!.canGoBack() 이 되므로 여기 안탐
            mBackTime = curTime
        }


    }

    private fun sendPushSetting() {
        val pushFlag = areNotificationsEnabled(this@MainActivity, "모두가 딜러")
        val PushFlagStr = if (pushFlag) "true" else "false"
        val pushStr = "appPushCheck('$PushFlagStr');"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView!!.evaluateJavascript(pushStr, null)
        } else {
            webView!!.loadUrl("javascript:$pushStr")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir  /* directory */
        )
    }


    override protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE && resultCode == RESULT_OK) {
            Log.e("KAKAO RESULT", "IN")
            val name = data?.getStringExtra("name")
            val email = data?.getStringExtra("email")
            val nickname = data?.getStringExtra("nickname")
            //String photoUrl = data.getStringExtra("photoUrl");
            val kkId = data?.getStringExtra("kkId")
            val kkAccessToken = data?.getStringExtra("kkAccessToken")
            val ageRange = data?.getStringExtra("ageRange")
            val account = data?.getStringExtra("account")
            val address = data?.getStringExtra("address")
            val birthday = data?.getStringExtra("birthday")
            val gender = data?.getStringExtra("gender")
            val phoneNumber = data?.getStringExtra("phoneNumber")
           // val jsStr = "kakaoLoginData( '$kkId','$name','$email','$ageRange','$nickname','$account','$nickname');" // 두번째
            val jsStr = "kakaoLoginDataV2( '$kkId', '$kkId','$email','$phoneNumber','$nickname');"
            Log.e("id","$kkId")
            // val jsStr = "kakaoLoginData( '$kkId','$name','$email');" // ㅊㅓ음
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView!!.evaluateJavascript(jsStr, null)
            } else {
                Log.e("KAKAO RESULT", "called")
                webView!!.loadUrl("javascript:$jsStr")
            }
            return
        }
        if (requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mFilePathCallback == null) {
                    super.onActivityResult(requestCode, resultCode, data)
                    return
                }
                val results = arrayOf(getResultUri(data))
                mFilePathCallback!!.onReceiveValue(results)
                mFilePathCallback = null
            } else {
                if (mUploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data)
                    return
                }
                val result = getResultUri(data)

                //Log.d(getClass().getName(), "openFileChooser : "+result);
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        } else {
            if (mFilePathCallback != null) mFilePathCallback!!.onReceiveValue(null)
            if (mUploadMessage != null) mUploadMessage!!.onReceiveValue(null)
            mFilePathCallback = null
            mUploadMessage = null
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().startSync()
        }
        actFlag = true
    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync()
        }
        actFlag = false
    }

    


    override fun onStop() {
        Log.e("onStop", "onStop")
        super.onStop()
        actFlag = false
    }

    private fun getIMEINumber(mContext: Context): String {
        return Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private inner class WebViewClientClass : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            val refreshedToken = FirebaseInstanceId.getInstance().token
            val versionName = BuildConfig.VERSION_NAME
            if (_firstFlag) {
                createNotificationChannel()
                val uuid: String = getIMEINumber(this@MainActivity)
                val pref: SharedPreferences = getSharedPreferences("ALLCAR_SETTING", MODE_PRIVATE)
                val loginState = pref.getString("LOGIN_STATE", "N")
                Log.e("LOGIN_STATE", loginState!!)
                val darkModeFlag: String = isDarkMode(this@MainActivity).toString()

                //플레이스토어
                val jsStr =
                    "setVersion('$versionName','and','$refreshedToken','$uuid', 'google', '$loginState','$darkModeFlag');"

                //원스토어
                //String jsStr = "setVersion('" + versionName + "','and','" + refreshedToken + "','" + uuid + "', 'onestore', '" + loginState + "','" + darkModeFlag  + "');";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView?.evaluateJavascript(jsStr, null)
                } else {
                    webView?.loadUrl("javascript:$jsStr")
                }
                sendPushSetting()
                _firstFlag = false

            }
            if (gdata?.getPageIndex() !== "") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView?.evaluateJavascript("setPush(" + gdata?.getPageIndex() + ");", null)
                } else {
                    webView?.loadUrl("javascript:setPush(" + gdata?.getPageIndex() + ");")
                }
                gdata?.setPageIndex("")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.getInstance().sync()
            } else {
                CookieManager.getInstance().flush()
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith("tel:")) {
                val call_phone = Intent(Intent.ACTION_CALL)
                call_phone.data = Uri.parse(url)
                startActivity(call_phone)
            } else if (url.startsWith("sms:")) {
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                startActivity(i)
            } else if (url.startsWith("mailto:")) {
                //mailto:ironnip@test.com
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                startActivity(i)
            } else if (url.startsWith("intent:")) {


                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val existPackage: Intent? =
                        getPackageManager().getLaunchIntentForPackage(intent.getPackage()!!)
                    if (existPackage != null) {
                        startActivity(intent)
                    } else {
                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        marketIntent.data = Uri.parse("market://details?id=" + intent.getPackage())
                        startActivity(marketIntent)
                    }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()

                }
            } else {
                if (url.contains("/KAKAO_LOGIN_APP")) {
                    Log.e("KAKAO_LOGIN_APP", "KAKAO_LOGIN_APP")
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivityForResult(intent, REQ_CODE)

                    //실제 카카오톡 로그인 기능을 실행할 LoginActivity 를 실행시킨다.
                    return true //리턴 true 하면, 웹뷰에서 실제로 위 URL 로 이동하지는 않는다.
                } else if (url.contains("/KAKAO_UNLINK_APP")) {
                    onClickUnlink()
                } else if (url.contains("/LOGIN_COMPLETE")) {
                    Log.e("LOGIN_COMPLETE", "LOGIN_COMPLETE")
                    val sharedPreference: SharedPreferences =
                        getSharedPreferences("ALLCAR_SETTING", MODE_PRIVATE)
                    val editor = sharedPreference.edit()
                    editor.putString("LOGIN_STATE", "Y")
                    editor.commit()
                } else if (url.contains("/LOGOUT_COMPLETE")) {
                    Log.e("LOGOUT_COMPLETE", "LOGOUT_COMPLETE")
                    val sharedPreference: SharedPreferences =
                        getSharedPreferences("ALLCAR_SETTING", MODE_PRIVATE)
                    val editor = sharedPreference.edit()
                    editor.putString("LOGIN_STATE", "N")
                    editor.commit()
                } else {
                    view.loadUrl(url)
                }
            }
            return true
        }

        override fun onReceivedSslError(
            view: WebView, handler: SslErrorHandler,
            error: SslError
        ) {
            //handler.proceed(); // 기존에 에러가나도 무조건 진행
            val dialog = SslAlertDialog(handler, this@MainActivity)
            dialog.show()
        }

        override fun onReceivedError(
            view: WebView, errorCode: Int,
            description: String, failingUrl: String
        ) {
            Log.d("Error", "Error code: $errorCode/$description")
        }
    }

    // 푸시 알림 확인
    fun areNotificationsEnabled(context: Context, channelId: String?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (NotificationManagerCompat.from(context)
                    .areNotificationsEnabled() == false
            ) return false
            if (channelId != null && !channelId.isEmpty()) {
                val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel = manager.getNotificationChannel(channelId)
                return channel.importance != NotificationManager.IMPORTANCE_NONE
            }
            false
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun createNotificationChannel() {
        val channelId = "모두가 딜러"
        val channelName = "모두가 딜러 알림"
        val channelDescription = "모두가 딜러(특가,이벤트 등)"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = channelDescription
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(100, 200, 100, 200)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun onClickUnlink() {
        val appendMessage = getString(R.string.com_kakao_confirm_unlink)
        AlertDialog.Builder(this)
            .setMessage(appendMessage)
            .setPositiveButton(
                getString(R.string.com_kakao_ok_button)
            ) { dialog, which ->

                UserApiClient.instance.unlink { error ->

                    if (error != null) {
                        Log.e(TAG, "연결 끊기 실패", error)
                    }
                    else {
                        Log.i(TAG, "연결 끊기 성공. SDK에서 토큰 삭제 됨")
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton(
                getString(R.string.com_kakao_cancel_button)
            ) { dialog, which -> dialog.dismiss() }.show()
    }

    private fun redirectMainActivity() {
        webView!!.loadUrl(url)
    }

    inner class iconChanger {

        @JavascriptInterface
        fun changeIcon(capitalCode: String) { // 웹뷰내의 페이지에서 호출하는 함수
            Log.e("iconChanger", "called")
            var activeName: String
            var disableNames: MutableList<String> = ArrayList()
            Log.e("IconChanger", "to $capitalCode")

            if ("capitalAA" == capitalCode) {
                Log.e("IconChanger", capitalCode)
                activeName = "com.dealer.allcar.MainActivityAACapital"
                disableNames.add("com.dealer.allcar.MainActivityDefault")
                disableNames.add("com.dealer.allcar.MainActivityALLPASS")
                setAppIcon(activeName, disableNames)
            } else if ("ALLPASS" == capitalCode) {
                Log.e("IconChanger", capitalCode)
                activeName = "com.dealer.allcar.MainActivityALLPASS"
                disableNames.add("com.dealer.allcar.MainActivityDefault")
                disableNames.add("com.dealer.allcar.MainActivityAACapital")
                setAppIcon(activeName, disableNames)
            } else {
                Log.e("IconChanger", "default")
                activeName = "com.dealer.allcar.MainActivityDefault"
                disableNames.add("com.dealer.allcar.MainActivityAACapital")
                disableNames.add("com.dealer.allcar.MainActivityALLPASS")
                setAppIcon(activeName, disableNames)
            }
        }
    }

    fun setAppIcon(activeName: String?, disableNames: List<String?>?) {
        AppIconNameChanger.Builder(this@MainActivity)
            .activeName(activeName) // String
            .disableNames(disableNames) // List<String>
            .packageName(BuildConfig.APPLICATION_ID)
            .build()
            .setNow()
    }
}