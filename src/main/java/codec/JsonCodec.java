package codec;

import java.lang.reflect.Type;

/**
 * json的编解码器接口
 */
public interface JsonCodec {
    String encode(Object object);
    Object decode(String json, Type type);
}
