package test;


import annotation.mapping.RequestMapping;
import annotation.param.Multipart;
import annotation.method.Post;
import annotation.param.RequestBody;
import callback.HttpCallback;
import lombok.Data;
import org.junit.Test;
import proxy.NettyProxy;
import proxy.NettyRequest;

import java.io.File;

public class Main {
    public static void main(String[] args) {

    }

    @RequestMapping("/user")
    interface FileService {


        @Post("/login")
        NettyRequest<Response<Rsp>> login(@RequestBody LoginModel loginModel);
    }

    @RequestMapping("/oss")
    interface MultiService {
        @Post("/image")
        NettyRequest<String> upload(@Multipart File file);
    }

    @Data
    class LoginModel {
        String account;
        String password;
    }

    @Data
    class Response <T> {
        int code;
        T entity;
        String info;
    }

    @Data
    class Rsp{
        private LoginModel userCard;
        private String token;
        private String account;
        private boolean isBind;
    }
    @Test
    public void test() {
        NettyProxy nettyProxy = new NettyProxy();
        nettyProxy.baseUrl("http://localhost:8080");
        FileService fileService = (FileService) nettyProxy.create(FileService.class);

        File file = new File("C:/Users/coura/Pictures/H图5.jpg");
        System.out.println(file.getName());

        /*LoginModel loginModel = new LoginModel();
        loginModel.setAccount("123");
        loginModel.setPassword("123");
        NettyRequest<Response<Rsp>> request = fileService.login(loginModel);
        request.requestAsync(new HttpCallback<Response<Rsp>>() {
            @Override
            public void onSuccess(Response<Rsp> response) {
                System.out.println(response.entity);
            }

            @Override
            public void onFailed(int code, String message, Exception e) {
                System.out.println("request failed");
            }
        });

        stop();*/
        nettyProxy = new NettyProxy();
        nettyProxy.baseUrl("http://localhost:8080");
        MultiService multiService = (MultiService) nettyProxy.create(MultiService.class);

        NettyRequest<String> nettyRequest = multiService.upload(file);
        nettyRequest.requestAsync(new HttpCallback<String>() {
            @Override
            public void onSuccess(String response) {
                System.out.println(response);
            }

            @Override
            public void onFailed(int code, String message, Exception e) {
                System.out.println("multipart failed");
                if (e!=null) {
                    e.printStackTrace();
                }
                System.out.println(code+":"+message);
            }
        });
        stop();

    }
    public void stop() {
        try {
            Thread.sleep(7*1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
    @Test
    public void nettyProxyTest() {
        NettyProxy nettyProxy = new NettyProxy();
        nettyProxy.baseUrl("http://localhost:8080");
        Service service = (Service) nettyProxy.create(Service.class);

        //register(service);
        login(service);

        stop();
    }

    @Test
    public void stringTest() {
        String url = "http://localhost/{account}";
        System.out.println("before : "+url);
        url = url.replace("{account}", "123456");
        System.out.println("after : "+url);
    }

    @Test
    public void uriTest() throws Throwable{
        String url = "http://localhost:8080/ust";
        URL u = new URL(url);
        URI uri = new URI(url.toString());
        System.out.println("Uri : "+uri.toString());
        System.out.println("host: "+u.getHost()+" port "+u.getPort()+" protocol : "+u.getProtocol());
    }

    public void register(Service service) {
        RegisterModel model = new RegisterModel();
        model.setAccount("qaz");
        model.setPassword("99999");
        model.setName("sha bi");

        NettyCall<Response<UserRspModel>> nettyCall = service.register(model);
        nettyCall.requestAsync(new HttpCallback<Response<UserRspModel>>() {
            @Override
            public void onSuccess(Response<UserRspModel> response) {
                System.out.println(response);
            }

            @Override
            public void onFailed(int code, String message, Exception e) {
                System.out.println("error error");
            }
        });

        stop();
    }

    public void login(Service service) {
        LoginModel model = new LoginModel();
        model.setAccount("111");
        model.setPassword("123");

        NettyCall<Response<UserRspModel>> nettyCall = service.login(model);
        nettyCall.requestAsync(new HttpCallback<>() {
            @Override
            public void onSuccess(Response<UserRspModel> response) {
                UserRspModel rspModel = response.getEntity();
                System.out.println("rsp model token "+rspModel.getToken());
            }

            @Override
            public void onFailed(int code, String message, Exception e) {
                System.out.println("error info : " + message);
                e.printStackTrace();
            }
        });
    }*/
}
