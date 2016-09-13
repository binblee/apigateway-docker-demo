# API网关遇上容器服务

在API经济和微服务的背景下，如何对服务的API进行管理是大家都很感兴趣的话题。本文通过利用阿里云的容器服务和API网关，构建一个完整的基于Docker的具有API管理功能的服务。

## API管理
假定我们需要这么一个经典的后端服务，访问如下API接口的时候返回Hello World：

```
$ curl http://apisvc.hostxx/api
<p>Hello World</p>
```

这个服务推出后广受欢迎，但是烦恼总是伴随幸福不期而至：

- 对API进行计费怎么做？
- 外界访问API的流量太高了，如何进行流量控制？
- 外界访问API的并发连接太多了，能不能把这许多连接合成一个长连接访问服务？
- 如何对API进行保护，让只有授权的应用才能访问API？
- ... ...

这实际上涉及到了API管理的内容，并且很多和业务逻辑无关。是否可以利用云上的PaaS服务来完成呢？

答案是，可以。利用阿里云的API网关就可以完成上面所有任务。当然，API网关的功能不止这些，如果想了解更详细的信息，请移步官网[API网关](https://help.aliyun.com/product/29462.html)。

## 定义后端服务

为了简单起见，我们的应用利用[openresty](http://openresty.org/)实现，运行在[阿里云容器服务](https://help.aliyun.com/product/25972.html)上。

### Openresty应用
Openresty是基于NGINX和LuaJIT的Web应用框架。实现我们的这个经典服务只需要nginx.conf配置文件做如下声明即可：

```
http {
    server {
        listen 8080;
        location /api {
            default_type text/html;
            content_by_lua '
                ngx.say("<p>hello, world</p>")
            ';
        }
    }
}
```

在本地启动openresty后访问：

```
$ /usr/local/openresty/bin/openresty -p . -c conf/nginx.conf
$ curl http://localhost:8080/api
<p>hello, world</p>
```
后端服务就成了。

### 构建Docker镜像

构建Docker镜像的Dockerfile内容如下：

```
FROM openresty/openresty:latest
RUN mkdir -p /work/conf work/logs
COPY nginx.conf /work/conf
WORKDIR /work
ENTRYPOINT ["/usr/local/openresty/bin/openresty", "-g", "daemon off;","-p","/work/","-c","/work/conf/nginx.conf"]
```

容器启动时为其增加了```daemon off```设置，这让nginx进程一直在前台运行，保证容器不退出。

### 部署到阿里云容器服务

部署到阿里云容器服务需要定义docker-compose.yml，这个文件和标准的docker compose部署模版文件完全兼容，只不过增加了一些阿里云特有的label。比如：

```aliyun.lb.port_8080: http://${slbname}:8080```

这个label的意思是把openresty应用对外通过负载均衡8080端口暴露出来。

```
version: '2'
services:
  hello:
    image: registry.cn-hangzhou.aliyuncs.com/jingshanlb/openresty-helloworld
    restart: always
    ports:
      - 8080:8080
    labels:
      aliyun.lb.port_8080: http://${slbname}:8080
      aliyun.scale: "2"
```

我们的测试集群是运行在经典网络中，开通一个内网阿里云负载均衡，并添加http 8080到8080的监听器。上面的部署文件讲hello服务通过这个负载均衡对外提供服务，访问端点为负载均衡的阿里云内网地址，如下图：

![slb_ip](https://yqfile.alicdn.com/7a87ee91d8660158a6ba14af7d5c2d535347cf19.png)

这个地址会用来配置API网关中对后端的访问，请记好。

## 在API网关中定义对后端的访问

### 创建API
我们首先要在API网关中定义API，并绑定和后端的映射关系。进入[API网关控制台](https://apigateway.console.aliyun.com/)，首先创建一个API分组：

![apigw_1](https://yqfile.alicdn.com/0c6af5f222e2513d8c1292c0672f98bab919c808.png)

在这个分组中创建API：

![apigw_2](https://yqfile.alicdn.com/3fe0cc9ef4f523eb8cf821ec956c90156cb09227.png)

对后端的访问地址为如下形式：

```
http://<负载均衡IP地址>:8080/api
```

API对外路径定义为```/api```，如下图所示：

![apigw_3_classic](https://yqfile.alicdn.com/7ce8cc1239324763707215164cb2b4e167ff052b.png)

把API发布成功后可以进入```调试API```进行测试：

![apigw_4](https://yqfile.alicdn.com/e7a387ac36b5bf18c5a594580c8a4227e7a5f363.png)

庆祝一下，我们的服务对外提供API了。

在应用程序能够访问我们的API之前，我们还需要在API网关控制台上生成应用访问所需要的token。

在控制台创建一个新的应用：

![apigw_6](https://yqfile.alicdn.com/45698144a9188aa77d95e4672da0a3a9dd448936.png)

授权该应用可以访问我们的经典服务：

![apigw_9](https://yqfile.alicdn.com/b3b6016b97a40f2c8bdd380ff05fac7c7f1ecaa9.png)

进入AppKey页面，记录下```AppKey```和```AppSecret```

![apigw_7](https://yqfile.alicdn.com/13aebbea33ae9be4928cb60cda7d247f1e01b86f.png)

进入分组管理页面，记录下API对外的二级域名：

![apigw_8](https://yqfile.alicdn.com/d9620e210e852a9b4cd551bde56ba993144f288c.png)

这三个信息是应用程序访问API所需的端点和认证信息，我们马上要把它们填入到程序中。

### 访问API的应用示例

下面我们可以构建一个访问API的应用示例。访问SDK下载页面，可以看到很多语言的示例。


![apigw_5](https://yqfile.alicdn.com/7477566d8fb0dfcd8aeb0c0e1af3896ec2038258.png)

本文我们使用Java语言创建应用，生成的最终示例在github上：[https://github.com/binblee/apigateway-docker-demo/tree/master/client](https://github.com/binblee/apigateway-docker-demo/tree/master/client)，大家有兴趣可以参考。

在DemoClient的主程序中声明3个变量表示前面拿到的三个信息：AppKey，AppSecret和分组二级域名（API_HOST)。

```java
public class DemoClient {
    //APP KEY
    private String APP_KEY;
    // APP密钥
    private String APP_SECRET;
    //API域名
    private String API_HOST;
...
```

把三个参数代入到HTTP请求中：

```java

//请求URL
String url = "/api";

Map<String, String> headers = new HashMap<String, String>();
headers.put(HttpHeader.HTTP_HEADER_ACCEPT, ContentType.CONTENT_TYPE_TEXT);

Request request = new Request(Method.GET, HttpSchema.HTTP + API_HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
request.setHeaders(headers);
request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);

//调用服务端
HttpResponse response = Client.execute(request);

```

为了不把ApiKey和ApiSecret等敏感信息硬编码到程序里，我们采用在环境变量中指定的方式：

```java
Map<String, String> env = System.getenv();
APP_KEY = env.get("APP_KEY");
if(APP_KEY == null || APP_KEY.length() == 0){
    System.err.println("Please specify APP_KEY environment variable.");
    return false;
}
```

### 构建并运行 Demo Client

为了方便示例程序的构建，我们使用Gradle。

```
dependencies {
	compile('org.springframework.boot:spring-boot-starter')
    compile('org.apache.httpcomponents:httpclient:4.2.1')
    compile('org.apache.httpcomponents:httpcore:4.2.1')
    compile('commons-lang:commons-lang:2.6')
    compile('org.eclipse.jetty:jetty-util:9.3.7.v20160115')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
```

运行如下命令可以构建一个包含所有依赖的Uber Jar：


```
./gradle build -x test
```

这个命令的意思是构建Jar包，但不执行测试。我们的示例Client的测试代码都需要填写ApiKey和ApiSecret等信息，没有的话测试会失败。为了不影响构建Jar包，我们编译的时候忽略测试。

构建成功后，执行如下命令运行Demo Client：


```
export APP_KEY=<placeholder>
export APP_SECRET=<placeholder>
export API_HOST=<placeholder>
java -jar build/libs/apidemoclient-0.0.1-SNAPSHOT.jar
```

把前面得到的信息分别填入，可以看到如下执行结果：

```
...
17:23:47.329 [main] DEBUG org.apache.http.impl.conn.BasicClientConnectionManager - Releasing connection org.apache.http.impl.conn.ManagedClientConnectionImpl@2b05039f
17:23:47.329 [main] DEBUG org.apache.http.impl.conn.BasicClientConnectionManager - Connection can be kept alive indefinitely
200
Tengine
Mon, 12 Sep 2016 09:23:41 GMT
text/html;charset=UTF-8
chunked
keep-alive
Accept-Encoding
Accept-Encoding
*
POST, GET, OPTIONS
X-Requested-With, X-Sequence, _aop_secret, _aop_signature
172800
717E9CD3-320A-49F0-8F4E-82C3C6CA913E
CONTAINERID=; Expires=Thu, 01-Jan-1970 00:00:01 GMT; path=/
<p>hello, world</p>
```

BINGO，访问应用成功调用了云上的API。

本文的示例代码在此：[https://github.com/binblee/apigateway-docker-demo](https://github.com/binblee/apigateway-docker-demo)，供大家参考。

## 小节

本文演示了如何利用阿里云的API网关管理后台Docker应用API，和读者探讨了如何利用API网关和CaaS的能力为服务提供API管理功能。