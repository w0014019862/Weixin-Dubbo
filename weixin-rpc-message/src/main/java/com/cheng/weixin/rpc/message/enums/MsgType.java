package com.cheng.weixin.rpc.message.enums;

/**
 * Desc: 短信消息类型
 * Author: cheng
 * Date: 2016/7/8
 */
public enum MsgType {
    NOTICE("通知"),
    VALIDATE("验证码"),
    ACTIVITY("活动");

    public String name;
    private MsgType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
