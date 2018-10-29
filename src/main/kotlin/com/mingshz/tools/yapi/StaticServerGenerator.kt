package com.mingshz.tools.yapi

import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * @author CJ
 */
class StaticServerGenerator(
        private val uris: String,
        private val target: String,
        private val stream: OutputStream
) {
    fun work() {

        val writer = OutputStreamWriter(stream, "UTF-8")

        writer.use {
            it.write("""
    server {
        listen 80;
        server_name $uris;

        location / {
            proxy_pass http://$target;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP ${'$'}remote_addr;
            proxy_set_header        X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
            proxy_set_header        Host ${'$'}http_host;
        }
    }
            """)

            it.flush()
        }
    }

}