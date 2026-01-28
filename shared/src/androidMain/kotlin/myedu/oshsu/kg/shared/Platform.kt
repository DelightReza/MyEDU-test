package myedu.oshsu.kg.shared

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val version: String = Build.VERSION.RELEASE
}

actual fun getPlatform(): Platform = AndroidPlatform()
