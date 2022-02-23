package com.dealer.allcar

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.kakao.auth.IApplicationConfig
import com.kakao.sdk.common.KakaoSdk


class GlobalApplication : Application() {

    private var PageIndex = ""
    fun getPageIndex(): String? {
        return PageIndex
    }

    fun setPageIndex(str: String) {
        PageIndex = str
    }

    @Volatile
    private var obj : GlobalApplication? = null

    @Volatile
    private var currentActivity : Activity? = null

    override fun onCreate() {
        super.onCreate()
        obj = this
        KakaoSdk.init(this, "426fc31648eacdc05b7b1c92f5e773ed")

    }


     fun getGlobalApplicationContext() : GlobalApplication? {
        return obj
    }


    fun getCurrentActivity(): Activity? {
        return this.currentActivity
    }

    fun setCurrentActivity(currentActivity: Activity?) {
        this.currentActivity = currentActivity
    }


    override fun getApplicationContext(): Context {
        return super.getApplicationContext()
    }
}