package com.mingshz.tools.yapi

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter

/**
 * 服务器生成者
 * @author CJ
 */
class ServerGenerator(
        /**
         * 特指服务器的名字
         */
        private val alias: String,
        private val home: File,
        private val stream: InputStream,
        private val fixedSchema: String?,
        private val baseUri: String = ""
) {
    private val uri = alias
    fun work() {

//        val wildcardRegex = Pattern.compile("\\{[a-z0-9A-Z]+}")
        val wildcardRegex = Regex("\\{[a-z0-9A-Z]+}")
        val toPatternWildcard = ".+"

        val writer = OutputStreamWriter(
                FileOutputStream(File(home, "$alias.nginx"))
                , "UTF-8"
        )

        writer.use {
            it.write(
                    """
    server {
        listen 80;
        server_name cdn_$uri.* cdn_${uri}_source.*;

        location / {
            proxy_pass http://$alias:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP ${'$'}remote_addr;
            proxy_set_header        X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
            proxy_set_header        Host ${'$'}http_host;
        }
    }

    server {
        listen 80;
        server_name $uri.*;

        location ~ ^/MP_verify_(\w+)\.txt${'$'} {
            add_header Content-Type text/plain;
            return 200 '${'$'}1';
        }

        location = / {
            proxy_pass http://$alias:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP ${'$'}remote_addr;
            proxy_set_header        X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
            proxy_set_header        Host ${'$'}http_host;
        }
        location =  {
            proxy_pass http://$alias:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP ${'$'}remote_addr;
            proxy_set_header        X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
            proxy_set_header        Host ${'$'}http_host;
        }
        location = /index.html {
            proxy_pass http://$alias:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP ${'$'}remote_addr;
            proxy_set_header        X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
            proxy_set_header        Host ${'$'}http_host;
        }
    """
            )

            val w = it
            val objectMapper = ObjectMapper()
            objectMapper.readTree(stream)
//                    排除掉没有list的元素
                    .filter { it.has("list") }
                    .flatMap {
                        val list = it["list"]
//                        排除掉没有path的元素
                        list.filter { it.has("path") }
                                .map {
                                    val path = it["path"]
                                    val start = "location"
//                            path.textValue().match
//                            println(wildcardRegex.containsMatchIn(path.textValue()))
                                    val now = if (wildcardRegex.containsMatchIn(path.textValue())) {
                                        start.plus(" ~ ").plus(baseUri).plus(path.textValue().replace(wildcardRegex, toPatternWildcard))
                                    } else {
                                        start.plus(" = ").plus(baseUri).plus(path.textValue())
                                    }
                                    now
                                }
                    }
                    .distinct()
                    .forEach {
                        w.write(
                                """
        $it {
            proxy_pass http://server:8080;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP ${'$'}remote_addr;
            proxy_set_header        X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
            proxy_set_header        Host ${'$'}http_host;
        }
"""
                        )
                    }

            val schema = fixedSchema ?: "${'$'}scheme"

            // 最后加入
            w.write(
                    """
        location / {
#             permanent
            rewrite ^(.+)${'$'} $schema://cdn_${'$'}host${'$'}request_uri;
        }

    }
"""
            )

            w.flush()
        }

    }
}