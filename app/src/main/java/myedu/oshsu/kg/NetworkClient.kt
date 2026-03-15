package myedu.oshsu.kg

import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    val cookieJar = UniversalCookieJar()
    val interceptor = WindowsInterceptor()
    val deepSpy = DeepSpyInterceptor()
    val failover = FailoverInterceptor()
    
    val api: OshSuApi = Retrofit.Builder()
        .baseUrl("https://api3.myedu.oshsu.kg/")
        .client(OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(failover)
            .addInterceptor(interceptor)
            .addInterceptor(deepSpy)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build().create(OshSuApi::class.java)

    val githubApi: GitHubApi = Retrofit.Builder().baseUrl("https://api.github.com/")
        .client(OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(GitHubApi::class.java)
}
