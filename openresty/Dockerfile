FROM openresty/openresty:latest
RUN mkdir -p /work/conf work/logs
COPY nginx.conf /work/conf
WORKDIR /work
ENTRYPOINT ["/usr/local/openresty/bin/openresty", "-g", "daemon off;","-p","/work/","-c","/work/conf/nginx.conf"]