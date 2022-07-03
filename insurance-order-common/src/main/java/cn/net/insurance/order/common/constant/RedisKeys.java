package cn.net.insurance.order.common.constant;

/**
 * 描述：redis key
 */
public interface RedisKeys {
    String OMS_LOGIN_USER_TOKEN = "oms:%s:user:token:%s";
    String OMS_LOGIN_USER_ID = "oms:%s:user:userId:%s";
    String OMS_REQUEST_SIGN_USER_ID = "oms:%s:req:sign:userId:%s:%s";
}
