package test;



public class Main {
    public static void main(String[] args) {

    }

    /*public void stop() {
        try {
            Thread.sleep(6*1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
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
