package com.aliyun.api.gateway.demo;

import com.aliyun.api.gateway.demo.constant.Constants;
import com.aliyun.api.gateway.demo.constant.ContentType;
import com.aliyun.api.gateway.demo.constant.HttpHeader;
import com.aliyun.api.gateway.demo.constant.HttpSchema;
import com.aliyun.api.gateway.demo.enums.Method;
import com.aliyun.api.gateway.demo.util.MessageDigestUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoClient {
    //APP KEY
    private String APP_KEY;
    // APP密钥
    private String APP_SECRET;
    //API域名
    private String API_HOST;
    //自定义参与签名Header前缀（可选,默认只有"X-Ca-"开头的参与到Header签名）
    private final static List<String> CUSTOM_HEADERS_TO_SIGN_PREFIX = new ArrayList<String>();

    static {
        CUSTOM_HEADERS_TO_SIGN_PREFIX.add("Custom");
    }

    private boolean initEnv(){
        Map<String, String> env = System.getenv();
        APP_KEY = env.get("APP_KEY");
        if(APP_KEY == null || APP_KEY.length() == 0){
            System.err.println("Please specify APP_KEY environment variable.");
            return false;
        }

        APP_SECRET = env.get("APP_SECRET");
        if(APP_SECRET == null || APP_SECRET.length() == 0){
            System.err.println("Please specify APP_SECRET environment variable.");
            return false;
        }

        API_HOST = env.get("API_HOST");
        if(API_HOST == null || API_HOST.length() == 0){
            System.err.println("Please specify API_HOST environment variable.");
            return false;
        }
        return true;
    }

    public void get() throws Exception {
        //请求URL
        String url = "/api";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, ContentType.CONTENT_TYPE_TEXT);

        Request request = new Request(Method.GET, HttpSchema.HTTP + API_HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    public static void main(String args[]) throws Exception{
        DemoClient demoClient = new DemoClient();
        if(demoClient.initEnv()){
            demoClient.get();
        }
    }

    private void print(HttpResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(response.getStatusLine().getStatusCode()).append(Constants.LF);
        for (Header header : response.getAllHeaders()) {
            sb.append(MessageDigestUtil.iso88591ToUtf8(header.getValue())).append(Constants.LF);
        }
        sb.append(readStreamAsStr(response.getEntity().getContent())).append(Constants.LF);
        System.out.println(sb.toString());
    }

    private static String readStreamAsStr(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WritableByteChannel dest = Channels.newChannel(bos);
        ReadableByteChannel src = Channels.newChannel(is);
        ByteBuffer bb = ByteBuffer.allocate(4096);

        while (src.read(bb) != -1) {
            bb.flip();
            dest.write(bb);
            bb.clear();
        }
        src.close();
        dest.close();

        return new String(bos.toByteArray(), Constants.ENCODING);
    }
}
