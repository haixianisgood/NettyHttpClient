package proxy;

import annotation.mapping.RequestMapping;
import annotation.method.*;
import annotation.param.*;
import codec.GsonCodec;
import codec.JsonCodec;
import exceptions.ParamException;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

/**
 * netty生成http请求的代理类，实现了Java代理接口、自定义的builder接口
 */
public class NettyProxy implements InvocationHandler, RequestBuilder {
    private String baseUrl = "";
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private JsonCodec codec = new GsonCodec();
    private final NettyRequest<?> nettyCall;
    private final ThreadLocal<String> requestUrl = new InheritableThreadLocal<>();
    private boolean isMultipart = false;

    public NettyProxy() {
        requestUrl.set("");
        nettyCall = new NettyRequest<>();
        nettyCall.codec(codec)
                .headers(headers)
                .header((HttpHeaderNames.ACCEPT).toString(), "application/json");
    }

    /**
     * 对实例化接口的方法的代理操作，根据注解和参数，生成一个netty HTTP请求
     * @param proxy 代理，实现了InvocationHandler接口
     * @param method 被调用的方法
     * @param args 实际参数
     * @return 代理返回的对象
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //获取被调用方法的返回类型，这是统一的泛型，泛型的参数类型才是我们想要的结果的类型
        Type type = method.getGenericReturnType();
        //获取泛型返回类型的实际参数类型
        Type resultType = ((ParameterizedType)type).getActualTypeArguments()[0];
        //设置实际结果的返回类型
        nettyCall.resultType(resultType);

        //解析类和方法上的注解，获取URL路径
        if(method.getDeclaringClass().isAnnotationPresent(RequestMapping.class)) {
            requestUrl.set(baseUrl+method.getDeclaringClass().getAnnotation(RequestMapping.class).value());
        } else {
            return null;
        }

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

        return nettyCall;
    }

    /**
     * 解析请求方法
     * @param annotation 接口方法上的注解
     */
    private void parseHttpMethod(Annotation annotation) {
        String url = requestUrl.get();
        if(annotation instanceof Get) {
            nettyCall.httpMethod(HttpMethod.GET);
            url = url + ((Get)annotation).value();
        }

        if(annotation instanceof Put) {
            nettyCall.httpMethod(HttpMethod.PUT);
            url = url + ((Put)annotation).value();
        }

        if(annotation instanceof Post) {
            nettyCall.httpMethod(HttpMethod.POST);
            url = url + ((Post)annotation).value();
            //判断该post方法需要发送的是不是multipart，如果已经通过builder开启，则无视post注解的multipart
            if(!isMultipart) {
                multipart(((Post) annotation).multipart());
            }
        }

        if(annotation instanceof Delete) {
            nettyCall.httpMethod(HttpMethod.DELETE);
            url = url + ((Delete)annotation).value();
        }

        requestUrl.set(url);
    }

    /**
     * 解析HTTP请求参数
     * @param method 被调用的接口方法
     * @param args 参数
     * @throws Exception 解析过程中出现的异常
     */
    private void parseArgs(Method method, Object[] args) throws Throwable{
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
                    nettyCall.addMultipart(name, json);
                } else {
                    //System.out.println("json : "+json);
                    nettyCall.content(json);
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
                String name = ((Upload)annotation).value();
                try {
                    File file = (java.io.File) args[i];
                    nettyCall.addMultipart(name, file);
                } catch (ClassCastException e) {
                    throw new ParamException("The parameter is not \"File\" class.");
                }
            }

            //上传一个或多个文件
            if(annotation instanceof Uploads) {
                String[] name = ((Uploads)annotation).value();
                File[] files = (File[]) args[i];
                //直接上传，参数名为空
                if (name.length == 0) {
                    for (File file : files) {
                        nettyCall.addMultipart(file);
                    }
                } else {
                    if(name.length < files.length) {
                        throw new ParamException("missing parameters");
                    }
                    for (int j = 0; j < name.length; j++) {
                        nettyCall.addMultipart(name[i], files[i]);
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
        nettyCall.url(url.toString());
    }

    /**
     * 实例化一个HTTP请求的接口类
     * @param type 需要实例化的接口的类型
     * @return 经处理后的代理类型，代理类型是动态生成的，JVM也不知道其实际类型，不能够进行序列化
     */
    public Object create(Class<?> type) {
        return Proxy.newProxyInstance(NettyProxy.class.getClassLoader(), new Class[]{type}, this);
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
        nettyCall.multipart(isMultipart);
        return this;
    }
}
