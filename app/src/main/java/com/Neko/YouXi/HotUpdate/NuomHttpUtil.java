package com.Neko.YouXi.HotUpdate;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

public class NuomHttpUtil {


    /**
     * 糯米
     * 网络请求返回的数据格式
     */
    public static class httpResult {
        public int code;
        public String body;
    }


    /**
     * 糯米
     * 超级好用的get请求，别问为什么，试试就知道了
     *
     * @param url 需要get请求的地址
     * @return 自定义类
     */
    public static httpResult getRequest(String url) {
        httpResult of = new httpResult();
        try {
            HttpResponse result = HttpRequest.get(url)
                    .header(Header.USER_AGENT, "nuom")//头信息，多个头信息多次调用此方法即可
                    .timeout(20000)//超时，毫秒
                    .execute();
            of.code = result.getStatus();
            of.body = result.body();
        } catch (Exception e) {
            of.code = 201;
            of.body = e.toString();
        }
        return of;
    }
}
