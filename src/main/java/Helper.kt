import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Conference(
    val name: String,
    val city: String,
    val country: String,
    val date: String,
    val logo: String,
    val website: String,
    val status: String
) {

    fun isCanceled() = status == "canceled"
}

private const val ENDPOINT =
    "c6b15f54c9fed96750e5828b2f001249/raw/d7fc5e1b711107583959663056e6643f24ccae81/conferences.json"

interface NetworkInterface {
    @GET(ENDPOINT)
    suspend fun fetchFromNetwork(): List<Conference>

    @GET(ENDPOINT)
    fun fetchFromNetworkNonSuspend(): Call<List<Conference>>
}

val networkInterface: NetworkInterface by lazy {
    val interceptor = HttpLoggingInterceptor()
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
    val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
        .build()
    Retrofit.Builder().apply {
        addConverterFactory(GsonConverterFactory.create())
        addCallAdapterFactory(CoroutineCallAdapterFactory())
        baseUrl(BASE_URL)
        client(okHttpClient)
    }
        .build()
        .create(NetworkInterface::class.java)
}

private const val BASE_URL = "https://gist.githubusercontent.com/cmota/"
