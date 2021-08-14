package proxy;

import callback.HttpCallback;
import codec.GsonCodec;
import codec.JsonCodec;
import handler.ResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NettyRequest<T> implements Request<T> {
    private static final String multipartBoundary = "NettyHttpClientBoundary12345678910";
    private static final EventLoopGroup loopGroup = new NioEventLoopGroup();

    private ChannelPipeline pipeline;
    private URL url;
    private HttpMethod httpMethod;
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private ChannelInitializer<SocketChannel> initializer;
    private final ByteBuf content = PooledByteBufAllocator.DEFAULT.directBuffer();
    private HttpCallback<T> httpCallback;
    private Type resultType;
    private JsonCodec codec = new GsonCodec();
    private final List<MultipartFile> multipartFiles = new ArrayList<>();
    private final List<MultipartBody> multipartBodies = new ArrayList<>();
    private boolean isMultipart = false;

    /**
     * 解析URL，发送HTTP请求
     * @param httpRequest netty的http请求类
     */
    protected void doRequest(FullHttpRequest httpRequest) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(loopGroup)
                .channel(NioSocketChannel.class)
                .handler(initializer)
                .option(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = bootstrap.connect(url.getHost(), url.getPort())
                .addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                pipeline.writeAndFlush(httpRequest);
            } else {
                httpCallback.onFailed(404, "can not connect", new Exception("can not connect"));
            }
        });
        f.channel().closeFuture();
    }

    @Override
    public void requestAsync(HttpCallback<T> httpCallback) {
        channelInitializer(httpCallback);

        FullHttpRequest httpRequest = buildRequest();
        //System.out.println(httpRequest);
        doRequest(httpRequest);
    }

    /**
     * 构建HTTP请求
     * @return netty的FullHttpRequest对象，用于承载HTTP报文
     */
    private FullHttpRequest buildRequest() {
        FullHttpRequest httpRequest;
        httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, httpMethod, url.getPath(), content);

        //添加请求首部
        httpRequest.headers().add(headers);

        //判断是否是multipart，再采取不同的操作
        if(isMultipart) {
            //multipart请求
            String contentType = String.format("multipart/form-data;boundary=%s", multipartBoundary);
            httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
            //httpRequest.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            //写入multipart文件
            for(MultipartFile file : multipartFiles) {
                writeMultipart(httpRequest.content(), file);
            }

            //写入序列化对象
            for(MultipartBody body : multipartBodies) {
                writeMultipart(httpRequest.content(), body);
            }
        } else {
            //普通HTTP请求
            httpRequest.content().writeBytes(content);
            httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        }

        //在首部填入content的长度
        httpRequest.headers().add(HttpHeaderNames.CONTENT_LENGTH, httpRequest.content().readableBytes());

        System.out.println(httpRequest);
        return httpRequest;
    }

    /**
     * 把文件写入到http请求的content中
     * @param content netty的ByteBuf，用于存放经过编码后的byte的报文
     * @param file 自定义的文件类，包含有需要写入content的文件
     */
    private void writeMultipart(ByteBuf content, MultipartFile file) {
        //需要写入到content中的固定内容
        String boundaryStart = "--"+multipartBoundary+"\r\n";
        String boundaryEnd = "\r\n--"+multipartBoundary+"--"+"\r\n";
        String contentDisposition = String.format("Content-Disposition:form-data;name=\"%s\";filename=\"%s\"\r\n",
                file.getName(), file.getFile().getName());
        String contentType = String.format("Content-Type:%s\r\n", file.getType());

        //执行写入
        content.writeCharSequence(boundaryStart, StandardCharsets.UTF_8);
        content.writeCharSequence(contentDisposition, StandardCharsets.UTF_8);
        content.writeCharSequence(contentType, StandardCharsets.UTF_8);
        content.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        //写入文件内容
        try {
            FileInputStream inputStream = new FileInputStream(file.getFile());
            FileChannel fileChannel = inputStream.getChannel();
            content.writeBytes(fileChannel, 0, (int) file.getFile().length());
            fileChannel.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //写入末尾分隔符
        content.writeCharSequence(boundaryEnd, StandardCharsets.UTF_8);
    }

    /**
     * 把序列化后对象写入到content中
     * @param content netty的ByteBuf，用于存放经过编码后的byte的报文
     * @param body 经过封装后的需要发送的序列化对象
     */
    private void writeMultipart(ByteBuf content, MultipartBody body) {
        //需要写入到content中的固定内容
        String boundaryStart = "--"+multipartBoundary+"\r\n";
        String boundaryEnd = "\r\n--"+multipartBoundary+"--"+"\r\n";
        String contentDisposition = String.format("Content-Disposition:form-data;name=\"%s\"\r\n", body.getName());
        String contentType = String.format("Content-Type:%s\r\n", "application/json");

        //执行写入操作
        content.writeCharSequence(boundaryStart, StandardCharsets.UTF_8);
        content.writeCharSequence(contentDisposition, StandardCharsets.UTF_8);
        content.writeCharSequence(contentType, StandardCharsets.UTF_8);
        content.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        content.writeCharSequence(body.getJson(), StandardCharsets.UTF_8);
        content.writeCharSequence(boundaryEnd, StandardCharsets.UTF_8);
    }

    /**
     * 创建ChannelInitialize，把回调添加到handler中，当收到回复时，会执行回调
     * @param callback 对结果的回调
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
                //pipeline.addLast(new ChunkedWriteHandler());//后续开发大文件传输使用
            }
        };
    }

    /**
     * 设置http请求的url
     * @param url url字符串
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> url(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 设置请求方法
     * @param httpMethod http请求方法
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> httpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    /**
     * 设置请求头部
     * @param headers http请求首部
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> headers(HttpHeaders headers) {
        this.headers.add(headers);
        return this;
    }

    /**
     * 设置请求头部
     * @param key 请求首部key
     * @param value 请求首部value
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> header(String key, Object value) {
        this.headers.add(key, value);
        return this;
    }

    /**
     * 设置content
     * @param json 序列化后的对象
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> content(String json) {
        this.content.writeCharSequence(json, StandardCharsets.UTF_8);
        return this;
    }

    /**
     * 返回content转化为对象的类型
     * @param type 被NettyRequest所包装的泛型参数的实际类型，即 T
     * @return
     */
    protected NettyRequest<T> resultType(Type type) {
        this.resultType = type;
        return this;
    }

    /**
     * 设置编解码器
     * @param codec 编解码器，用于对需要序列化的对象进行编解码
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> codec(JsonCodec codec) {
        this.codec = codec;
        return this;
    }

    /**
     * 添加multipart内容
     * @param file 需要发送的文件
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> addMultipart(File file) {
        multipartFiles.add(new MultipartFile("", file));
        return this;
    }

    /**
     * 添加multipart内容
     * @param name 参数名
     * @param file 需要发送的文件
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> addMultipart(String name, File file) {
        multipartFiles.add(new MultipartFile(name, file));
        return this;
    }

    /**
     * 添加multipart内容
     * @param name 参数名
     * @param json 需要发送的序列化后的对象
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> addMultipart(String name, String json) {
        multipartBodies.add(new MultipartBody(name, json));
        return this;
    }

    /**
     * 开启multipart
     * @param isMultipart true则开启multipart，默认为false
     * @return NettyRequest本身，使用builder模式创建
     */
    protected NettyRequest<T> multipart(boolean isMultipart) {
        this.isMultipart = isMultipart;
        return this;
    }

    /**
     * 对发送的文件的封装类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class MultipartFile {
        private String name;
        private File file;
        private String type;

        public MultipartFile(String name, File file) {
            this.name = name;
            this.file = file;
            type = HttpHeaderValues.APPLICATION_OCTET_STREAM.toString();
        }
    }

    /**
     * 对请求实体对象序列化的封装类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class MultipartBody {
        private String name;
        private String json;
    }
}


