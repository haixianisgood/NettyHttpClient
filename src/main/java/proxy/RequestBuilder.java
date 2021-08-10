package proxy;

import codec.JsonCodec;

import java.util.Map;

public interface RequestBuilder {
    RequestBuilder baseUrl(String url);
    RequestBuilder codec(JsonCodec codec);
    RequestBuilder header(String key, String value);
    RequestBuilder headers(Map<String, String> map);
}
