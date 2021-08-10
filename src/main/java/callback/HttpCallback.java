package callback;

/**
 * 回调接口
 * @param <T> 返回的响应经过解码后的类型
 */
public interface HttpCallback <T> {
    void onSuccess(T response);

    /**
     * 请求失败时的回调方法
     * @param code 错误码
     * @param message 提示信息
     * @param e 异常
     */
    void onFailed(int code, String message, Exception e);
}
