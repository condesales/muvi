package com.muvi.remote

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

class LetterboxdApiFactory(private val apiKey: String,
                           private val apiSecret: String,
                           private val clock: Clock,
                           private val enableHttpLogging: Boolean) {

    fun remoteLetterboxdApi(): LetterboxdApi {
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(AddApiKeyQueryParameterInterceptor(apiKey, clock))
                .addNetworkInterceptor(AddAuthorizationHeaderInterceptor(apiSecret))

        if (enableHttpLogging) {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClient.addInterceptor(httpLoggingInterceptor)
        }

        return Retrofit.Builder()
                .client(okHttpClient.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl("https://api.letterboxd.com/api/v0/")
                .build()
                .create(LetterboxdApi::class.java)
    }
}

private class AddApiKeyQueryParameterInterceptor(private val apiKey: String, private val clock: Clock) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.newBuilder()
                .addQueryParameter("apikey", apiKey)
                .addQueryParameter("nonce", UUID.randomUUID().toString())
                .addQueryParameter("timestamp", TimeUnit.MILLISECONDS.toSeconds(clock.currentTimeMillis()).toString())
                .build()
        val request = chain.request().newBuilder().url(url).build()
        return chain.proceed(request)
    }
}

private const val HEADER_AUTH = "Authorization"

private class AddAuthorizationHeaderInterceptor(private val apiSecret: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        val url = chain.request().url

        val signature = generateSignature(chain.request().method, url, chain.request().body)
        builder.url(url)
        builder.addHeader(HEADER_AUTH, "Signature $signature")

        return chain.proceed(builder.build())
    }

    private fun generateSignature(httpMethod: String, url: HttpUrl,
                                  requestBody: RequestBody?): String {
        return if (requestBody == null) {
            generateSignature(httpMethod, url.toString(), "")
        } else {
            generateSignature(httpMethod, url.toString(),
                    (requestBody as FormBody).toUrlEncodedString())
        }
    }

    private fun generateSignature(httpMethod: String, url: String, urlEncodedBody: String): String {
        val preHashed = String.format(Locale.US, "%s\u0000%s\u0000%s",
                httpMethod.toUpperCase(Locale.US), url, urlEncodedBody)
        return HmacSha256.generateHash(apiSecret, preHashed).toLowerCase(Locale.US)
    }

    private fun FormBody.toUrlEncodedString(): String {
        val pairs = ArrayList<String>()
        for (i in 0 until size) {
            pairs.add(encodedName(i) + "=" + encodedValue(i))
        }
        return pairs.joinToString(separator = "&")
    }
}
