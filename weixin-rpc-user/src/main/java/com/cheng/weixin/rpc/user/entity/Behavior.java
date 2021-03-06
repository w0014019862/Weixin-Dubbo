package com.cheng.weixin.rpc.user.entity;

import com.cheng.common.entity.DataEntity;
import com.cheng.weixin.rpc.user.enumType.BehaviorType;

/**
 * Desc: 该表主要为：积分记录、现金账户记录、券记录中的收入和支出服务。
 *      比如：原因录入：下订单    其附属信息为该动作产生的结果为：单号 200898983094
 * Author: cheng
 * Date: 2016/6/3
 */
public class Behavior extends DataEntity<Behavior> {
    private BehaviorType behaviorType;
    private String nanme;

    public String getNanme() {
        return nanme;
    }

    public void setNanme(String nanme) {
        this.nanme = nanme;
    }

    public BehaviorType getBehaviorType() {
        return behaviorType;
    }

    public void setBehaviorType(BehaviorType behaviorType) {
        this.behaviorType = behaviorType;
    }

}
