package proxy;

import callback.HttpCallback;

public interface Request<T>{
    /**
     * 发送异步的HTTP请求
     * @param callback 收到response后执行的回调
     */
    void requestAsync(HttpCallback<T> callback);
}
