package proxy;

import callback.HttpCallback;
import codec.GsonCodec;
import codec.JsonCodec;
import handler.ResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NettyRequest<T> implements Request<T> {
    private static final EventLoopGroup loopGroup = new NioEventLoopGroup();
    private ChannelPipeline pipeline;
    private URL url;
    private HttpMethod httpMethod;
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private ChannelInitializer<SocketChannel> initializer;
    private String content;
    private HttpCallback<T> httpCallback;
    private Type resultType;
    private JsonCodec codec = new GsonCodec();

    /**
     * 解析URL，发送HTTP请求
     * @param httpRequest netty的http请求类
     */
    public void doRequest(FullHttpRequest httpRequest) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(loopGroup)
                .channel(NioSocketChannel.class)
                .handler(initializer)
                .option(ChannelOption.SO_KEEPALIVE, true);

        try {
            ChannelFuture f = bootstrap.connect(url.getHost(), url.getPort()).addListener((ChannelFutureListener) future -> {
                try {
                    if (future.isSuccess()) {
                        pipeline.writeAndFlush(httpRequest);
                    } else {
                        httpCallback.onFailed(404, "can not connect", new Exception("can not connect"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    httpCallback.onFailed(404, "connect error", e);
                }
            });
            f.channel().closeFuture();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步发送请求
     * @param httpCallback 发送请求后对响应的回调
     */
    @Override
    public void requestAsync(HttpCallback<T> httpCallback) {
        channelInitializer(httpCallback);
        FullHttpRequest httpRequest;
        try {
            //URI uri = new URI(url.toString());
            httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, url.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        httpRequest.headers().add(headers);
        httpRequest.content().writeCharSequence(content, StandardCharsets.UTF_8);
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        httpRequest.headers().add(HttpHeaderNames.CONTENT_LENGTH, httpRequest.content().readableBytes());

        //System.out.println(httpRequest);
        doRequest(httpRequest);
    }

    /**
     * 把回调添加到handler中，当收到回复时，会执行回调
     * @param callback 对于结果的回调
     */
    protected void channelInitializer(HttpCallback<T> callback) {
        this.httpCallback = callback;
        initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                pipeline = ch.pipeline();
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpObjectAggregator(2048 * 1024));
                pipeline.addLast(new ResponseHandler<>(codec, callback, resultType));
                pipeline.addLast(new ChunkedWriteHandler());//后续开发文件传输使用
            }
        };
    }

    //设置URL
    protected NettyRequest<T> url(String url) throws Exception {
        this.url = new URL(url);
        return this;
    }

    //设置请求方法
    protected NettyRequest<T> httpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    //设置请求头部
    protected NettyRequest<T> headers(HttpHeaders headers) {
        this.headers.add(headers);
        return this;
    }

    //设置请求头部
    protected NettyRequest<T> header(String key, Object value) {
        this.headers.add(key, value);
        return this;
    }

    //设置content
    protected NettyRequest<T> content(String json) {
        /*byte[] buf = new byte[content.readableBytes()];
        content.readBytes(buf);
        System.out.println("byte buf content "+new String(buf, StandardCharsets.UTF_8));*/
        this.content = json;
        return this;
    }

    //返回content转化为对象的类型
    protected NettyRequest<T> resultType(Type type) throws NoSuchFieldException {
        this.resultType = type;
        return this;
    }

    //设置编解码器
    protected NettyRequest<T> codec(JsonCodec codec) {
        this.codec = codec;
        return this;
    }




}


