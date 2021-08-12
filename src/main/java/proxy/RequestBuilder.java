package proxy;

import codec.JsonCodec;

import java.util.Map;

public interface RequestBuilder {
    /**
     * 设置根URL
     * @param url url字符串
     * @return builder本身
     */
    RequestBuilder baseUrl(String url);

    /**
     * 设置编解码器
     * @param codec 必须是实现了JsonCodec接口
     * @return builder本身
     */
    RequestBuilder codec(JsonCodec codec);

    /**
     * 添加一个请求首部
     * @param key 请求首部的key
     * @param value 请求首部的value
     * @return builder本身
     */
    RequestBuilder header(String key, String value);

    /**
     * 添加一个或多个请求首部
     * @param map 由请求首部组成的map
     * @return builder本身
     */
    RequestBuilder headers(Map<String, String> map);

    /**
     * 设置multipart模式
     * @param isMultipart true则开启multipart，默认为false
     * @return builder本身
     */
    RequestBuilder multipart(boolean isMultipart);
}
