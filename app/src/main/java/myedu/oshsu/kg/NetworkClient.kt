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
        .baseUrl(AppConstants.API_BASE_URL)
        .client(OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(failover)
            .addInterceptor(interceptor)
            .addInterceptor(deepSpy)
            .connectTimeout(AppConstants.API_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(AppConstants.API_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build().create(OshSuApi::class.java)

    val githubApi: GitHubApi = Retrofit.Builder().baseUrl(AppConstants.GITHUB_API_BASE_URL)
        .client(OkHttpClient.Builder()
            .connectTimeout(AppConstants.GITHUB_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(AppConstants.GITHUB_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(GitHubApi::class.java)
}
