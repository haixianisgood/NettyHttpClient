package handler;

import callback.HttpCallback;
import codec.JsonCodec;
import exceptions.HttpEncodeException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.FullHttpResponse;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 用于处理http请求返回的响应的handler
 * @param <T> 响应的类型
 */
public class ResponseHandler<T> extends ChannelInboundHandlerAdapter {
    //回调接口
    private final HttpCallback<T> callback;

    //json编解码器
    private final JsonCodec codec;

    //响应的实际类型
    private Type resultType;

    public ResponseHandler(JsonCodec codec, HttpCallback<T> callback, Type resultType) {
        this.codec = codec;
        this.callback = callback;
        this.resultType = resultType;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(!(msg instanceof FullHttpResponse)) {
            super.channelRead(ctx, msg);
            return;
        }

        FullHttpResponse httpResponse = (FullHttpResponse) msg;
        try {
            //编码失败，HTTP报文错误
            if(httpResponse.decoderResult().isFailure()) {
                onFailed(httpResponse.status().code(), httpResponse.status().reasonPhrase(), new HttpEncodeException("Http request encoding failed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFailed(404, "response send error", e);
        }

        if(httpResponse.status().code() == 200) {
            String content = httpResponse.content().toString(StandardCharsets.UTF_8);
            //System.out.println("request success");
            onSuccess(content);
            ctx.channel().closeFuture();
        } else {
            onFailed(httpResponse.status().code(), httpResponse.status().reasonPhrase(), new Exception(httpResponse.status().reasonPhrase()));
        }

    }

    /**
     * 请求发送成功的回调
     * @param content 响应报文的content
     */
    @SuppressWarnings("unchecked")
    private void onSuccess(String content) {
        T response = (T) codec.decode(content, resultType);
        callback.onSuccess(response);
    }

    /**
     * 请求发送失败的回调
     * @param code 失败码
     * @param info 可能的失败原因
     * @param e 捕捉到的异常
     */
    private void onFailed(int code, String info, Exception e) {
        callback.onFailed(code, info, e);
    }


}
