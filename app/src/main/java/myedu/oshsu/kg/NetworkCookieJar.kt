package myedu.oshsu.kg

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.*

class UniversalCookieJar : CookieJar {
    private val cookieStore = ArrayList<Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) { val names = cookies.map { it.name }; cookieStore.removeAll { it.name in names }; cookieStore.addAll(cookies) }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = ArrayList(cookieStore)
    fun injectSessionCookies(token: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000000'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        cookieStore.removeAll { it.name == "myedu-jwt-token" || it.name == "my_edu_update" || it.name == "have_2fa" }
        cookieStore.add(Cookie.Builder().domain("myedu.oshsu.kg").path("/").name("myedu-jwt-token").value(token).build())
        cookieStore.add(Cookie.Builder().domain("myedu.oshsu.kg").path("/").name("my_edu_update").value(sdf.format(Date())).build())
        cookieStore.add(Cookie.Builder().domain("myedu.oshsu.kg").path("/").name("have_2fa").value("yes").build())
    }
    fun clear() { cookieStore.clear() }
}
