package com.frontend

import android.view.View
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ReactShadowNode
import com.facebook.react.uimanager.ViewManager

/** AppVersionModule 을 앱에 등록한다. 라이브러리가 아니라 앱 코드라 자동 연결이 안 된다. */
class AppVersionPackage : ReactPackage {

  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> =
      listOf(AppVersionModule(reactContext))

  override fun createViewManagers(
      reactContext: ReactApplicationContext
  ): List<ViewManager<out View, out ReactShadowNode<*>>> = emptyList()
}
