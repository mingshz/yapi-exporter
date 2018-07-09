package com.mingshz.tools.yapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.CookieStore
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.config.SocketConfig
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author CJ
 */
class ApiDownloader(
        private val home: File,
        username: String,
        password: String,
        private val url: String = "http://api.mingshz.com",
        private val cookieStore: CookieStore = BasicCookieStore()
) {
    private fun createClient(): CloseableHttpClient {
        return HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(30000).build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(30000)
                        .setSocketTimeout(30000)
                        .setConnectionRequestTimeout(30000)
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .build()
                ).build()
    }

    init {
        val objectMapper = ObjectMapper()
        val data = mutableMapOf(
                "email" to username,
                "password" to password
        )
        createClient().use {
            val post = HttpPost("$url/api/user/login")
            post.setHeader("Accept", "application/json, text/plain")
            post.entity = EntityBuilder.create()
                    .setContentType(ContentType.create("application/json", "UTF-8"))
                    .setText(objectMapper.writeValueAsString(data))
                    .build()
            it.execute(post).use {
                if (it.statusLine.statusCode != 200)
                    throw IllegalStateException("登录响应错误:" + it.statusLine)
                val result = objectMapper.readTree(it.entity.content)
                if (result["errcode"].intValue() != 0)
                    throw IllegalStateException("登录响应错误:$result")
            }
        }
    }


    fun downloadTo(id: Int, name: String) {
        createClient().use {
            val get = HttpGet("$url/api/plugin/export?type=json&pid=$id&status=all")
            it.execute(get).use {
                val targetPath = File(home,"$name-api.json").toPath()
                Files.copy(it.entity.content, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}