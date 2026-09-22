package za.co.kinplus.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import za.co.kinplus.app.BuildConfig
import za.co.kinplus.app.data.remote.AuthInterceptor
import za.co.kinplus.app.data.remote.KinPlusApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides the networking stack: Gson, an OkHttp client with the Firebase
 * bearer-token interceptor and logging, and the Retrofit-generated API.
 * The base URL comes from BuildConfig (set in local.properties), never
 * hard-coded, so debug and release can target different hosts.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Body logging in debug only; headers (which include the token) in release.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideKinPlusApi(retrofit: Retrofit): KinPlusApi = retrofit.create(KinPlusApi::class.java)
}
