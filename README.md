# exporter
## 目的
将 [yapi](https://github.com/YMFE/yapi)  所维护的API导出HTTP服务器支持的配置文件，使相应http服务器直接支持分流相应的请求。

* 静态资源请求，以模块为host反向代理；
* /MP_verify_ 微信自动校验；
* 符合yapi定义的API，反向代理到约定的地址，比如 server:8080(默认值)；
* 根目录，以模块为host反向代理；

## 结果例子
### nginx
```nginx

    server {
        listen 80;
        server_name cdn_manager.* cdn_manager_source.*;

        location / {
            proxy_pass http://manager:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        Host $http_host;
        }
    }

    server {
        listen 80;
        server_name manager.*;

        location ~ ^/MP_verify_(\w+)\.txt$ {
            add_header Content-Type text/plain;
            return 200 '$1';
        }

        location = / {
            proxy_pass http://manager:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        Host $http_host;
        }
        location =  {
            proxy_pass http://manager:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        Host $http_host;
        }
        location = /index.html {
            proxy_pass http://manager:80;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        Host $http_host;
        }
    
        location = /complaint {
            proxy_pass http://server:8080;
#            proxy_set_header        Connection "Keep-Alive";
            proxy_set_header        X-Real-IP $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        Host $http_host;
        }

        location / {
#             permanent
            rewrite ^(.+)$ $scheme://cdn_$host$request_uri;
        }

    }


```