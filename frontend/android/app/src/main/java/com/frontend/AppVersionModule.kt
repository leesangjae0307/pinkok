package com.frontend

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.module.annotations.ReactModule

/**
 * 앱 버전을 자바스크립트에 알려 준다.
 *
 * build.gradle 의 versionName/versionCode 가 그대로 BuildConfig 에 박히므로,
 * 배포 번호를 올리면 화면에도 저절로 따라온다. 설정 화면에 버전을 글자로
 * 적어 두면 올릴 때마다 같이 고쳐야 하고, 실제로 어긋나 있었다.
 *
 * 값이 바뀌지 않으므로 상수로 넘긴다(메서드 호출이 필요 없다).
 */
@ReactModule(name = AppVersionModule.NAME)
class AppVersionModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = NAME

  override fun getConstants(): Map<String, Any> =
      mapOf(
          "versionName" to BuildConfig.VERSION_NAME,
          "versionCode" to BuildConfig.VERSION_CODE,
      )

  companion object {
    const val NAME = "AppVersion"
  }
}
