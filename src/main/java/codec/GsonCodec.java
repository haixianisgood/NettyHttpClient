package codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

/**
 * 通过gson实现json的编解码
 */
public class GsonCodec implements JsonCodec{
    private Gson gson = new Gson();
    public GsonCodec() {
    }

    @Override
    public String encode(Object object) {
        return gson.toJson(object);
    }

    @Override
    public Object decode(String json, Type type) {
        return gson.fromJson(json, type);
    }
}
