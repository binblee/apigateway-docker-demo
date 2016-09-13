# Aliyun API Gateway and Docker Application Demo

For Yunqi Document: [API网关遇上容器服务](https://yq.aliyun.com/articles/60530)

## Backend Service Providing API

Use [Openresty](https://openresty.org/) to create a simple API Service. 

Add below code in [nginx.conf](openresty/nginx.conf):

```
...
http {
    server {
        listen 8080;
        location / {
            default_type text/html;
            content_by_lua '
                ngx.say("<p>hello, world</p>")
            ';
        }
    }
}
```

test it in local environment:

```
curl http://localhost:8080/
```

## Build Docker and deploy it to Aliyun Container Service

Please refer [Dockerfile](openresty/Dockerfile) for how to build Docker image.

Use [docker-compose.yml] to deploy it to Aliyun Container Service.
