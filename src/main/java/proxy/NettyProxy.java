package proxy;

import annotation.header.Header;
import annotation.mapping.RequestMapping;
import annotation.method.*;
import annotation.param.*;
import codec.GsonCodec;
import codec.JsonCodec;
import exceptions.NettyProxyException;
import exceptions.ParamException;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Map;

/**
 * netty生成http请求的代理类，实现了Java代理接口、自定义的builder接口
 * 通过builder模式构建NettyProxy对象
 */
public class NettyProxy implements InvocationHandler, RequestBuilder {
    private String baseUrl = "";
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private JsonCodec codec = new GsonCodec();
    private NettyRequest<?> nettyRequest;
    private final ThreadLocal<String> requestUrl = new InheritableThreadLocal<>();
    private boolean isMultipart = false;
    private boolean isBound = false;
    public NettyProxy() {

    }

    /**
     * 对被代理的接口的方法的操作，根据注解和参数，生成一个netty HTTP请求
     * @param proxy 代理，实现了InvocationHandler接口
     * @param method 被调用的方法
     * @param args 实际参数
     * @return 代理返回的对象
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        requestUrl.set("");

        nettyRequest = new NettyRequest<>();
        nettyRequest.codec(codec)
                .headers(headers)
                .header((HttpHeaderNames.ACCEPT).toString(), "application/json")
                .multipart(isMultipart);

        //获取被调用方法的返回类型，这是统一的泛型，泛型的参数类型才是我们想要的结果的类型
        Type type = method.getGenericReturnType();
        //获取泛型返回类型的实际参数类型
        Type resultType = ((ParameterizedType)type).getActualTypeArguments()[0];
        //设置实际结果的返回类型
        nettyRequest.resultType(resultType);

        //解析被调用的方法上的注解，确定请求方法
        for (Annotation annotation : method.getAnnotations()) {
            parseHttpMethod(annotation);
        }

        //设置HTTP请求相关的参数
        try {
            parseArgs(method, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return nettyRequest;
    }

    /**
     * 解析请求方法
     * @param annotation 接口方法上的注解
     */
    private void parseHttpMethod(Annotation annotation) {
        String url = requestUrl.get();
        if(annotation instanceof Get) {
            nettyRequest.httpMethod(HttpMethod.GET);
            url = url + ((Get)annotation).value();
        }

        if(annotation instanceof Put) {
            nettyRequest.httpMethod(HttpMethod.PUT);
            url = url + ((Put)annotation).value();
        }

        if(annotation instanceof Post) {
            nettyRequest.httpMethod(HttpMethod.POST);
            url = url + ((Post)annotation).value();
            //判断该post方法需要发送的是不是multipart，如果已经通过builder开启，则无视post注解的multipart
            if(!isMultipart) {
                multipart(((Post) annotation).multipart());
            }
        }

        if(annotation instanceof Delete) {
            nettyRequest.httpMethod(HttpMethod.DELETE);
            url = url + ((Delete)annotation).value();
        }

        if(annotation instanceof Header) {
            Header header = (Header) annotation;
            this.header(header.key(), header.val());
        }

        requestUrl.set(url);
    }

    /**
     * 解析HTTP请求参数
     * @param method 被调用的接口方法
     * @param args 参数
     * @throws ParamException 解析过程中出现的参数异常
     */
    private void parseArgs(Method method, Object[] args) throws ParamException{
        Annotation[][] annotations = method.getParameterAnnotations();
        StringBuilder url = new StringBuilder(requestUrl.get());
        for(int i = 0 ; i < args.length; i++) {
            if(annotations[i].length == 0) {
                continue;
            }

            //HTTP请求相关的参数只能有一个注解
            Annotation annotation = annotations[i][0];

            //请求参数序列化作为请求体
            if(annotation instanceof RequestBody) {
                String json = codec.encode(args[i]);
                if(isMultipart) {
                    String name = ((RequestBody)annotation).value();
                    nettyRequest.addMultipart(name, json);
                } else {
                    //System.out.println("json : "+json);
                    nettyRequest.content(json);
                }
            }

            //路径参数
            if(annotation instanceof PathVariable) {
                String var = ((PathVariable)annotation).value();
                url = new StringBuilder(url.toString().replace("{" + var + "}", ((String) args[i])));
            }

            //请求参数
            if(annotation instanceof RequestParam) {
                String var = ((RequestParam)annotation).value();
                url.append(var).append("=").append(args[i].toString()).append("&");
            }

            //上传一个文件
            if(annotation instanceof Upload) {
                if(!(args[i] instanceof File)) {
                    throw  new ParamException("The parameter is not a \"File\"");
                }
                String name = ((Upload)annotation).value();
                try {
                    File file = (File) args[i];
                    nettyRequest.addMultipart(name, file);
                } catch (ClassCastException e) {
                    throw new ParamException("The parameter is not \"File\" class.");
                }
            }

            //上传一个或多个文件
            if(annotation instanceof Uploads) {
                if(!(args[i] instanceof File[])) {
                    throw  new ParamException("The parameter is not a \"File\" array");
                }
                String[] name = ((Uploads)annotation).value();
                File[] files = (File[]) args[i];
                //直接上传，参数名为空
                if (name.length == 0) {
                    for (File file : files) {
                        nettyRequest.addMultipart(file);
                    }
                } else {
                    if(name.length < files.length) {
                        throw new ParamException("missing request parameters");
                    }
                    for (int j = 0; j < name.length; j++) {
                        nettyRequest.addMultipart(name[i], files[i]);
                    }
                }
            }
        }

        //修饰添加请求参数后的URL字符串
        if (url.lastIndexOf("&") == url.length()-1) {
            url = new StringBuilder(url.substring(0, url.length() - 2));
        }

        //System.out.println("full url : "+fullUrl);

        //添加路径变量、请求参数后的完整的HTTP请求的URL字符串
        nettyRequest.url(url.toString());
    }

    /**
     * 实例化一个HTTP请求的接口类，一个Proxy只能实例化一个接口类
     * @param type 需要实例化的接口的类型
     * @return 经处理后的代理类型，代理类型是动态生成的，JVM也不知道其实际类型，不能够进行序列化
     */
    public Object bind(Class<?> type) throws NettyProxyException{

        if(isBound) {
            throw new NettyProxyException("This NettyProxy has been bound");
        } else {
            //bind的时候，同时解析接口类的注解，避免每次都解析，以减少性能开心
            parseClassAnnotation(type);
            this.isBound = true;
            //使用Proxy类来创建对象，能够拦截对象的方法调用
            return Proxy.newProxyInstance(NettyProxy.class.getClassLoader(), new Class[]{type}, this);
        }

    }

    /**
     * 解析接口类上的注解
     * @param type 需要被代理的接口类
     */
    private void parseClassAnnotation(Class<?> type) {
        //解析接口类上的请求路径注解，并添加到baseUrl中
        if (type.isAnnotationPresent(RequestMapping.class)) {
            this.baseUrl = this.baseUrl + type.getAnnotation(RequestMapping.class).value();
        }

        //解析接口类上的请求头部注解，并添加到headers中
        Annotation[] classAnnotations = type.getDeclaredAnnotations();
        for(Annotation annotation : classAnnotations) {
            if(annotation instanceof Header) {
                Header header = (Header) annotation;
                this.header(header.key(), header.val());
            }
        }
    }

    @Override
    public RequestBuilder baseUrl(String url) {
        this.baseUrl = url;
        //System.out.println("base url : "+baseUrl);
        return this;
    }

    @Override
    public RequestBuilder codec(JsonCodec codec) {
        this.codec = codec;
        return this;
    }

    @Override
    public RequestBuilder header(String key, String value) {
        headers.add(key, value);
        return this;
    }

    @Override
    public RequestBuilder headers(Map<String, String> map) {
        for(Map.Entry<String, String> entry : map.entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public RequestBuilder multipart(boolean isMultipart) {
        this.isMultipart = isMultipart;
        return this;
    }
}
