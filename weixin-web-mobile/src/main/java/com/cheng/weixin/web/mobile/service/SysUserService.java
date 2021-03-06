package com.cheng.weixin.web.mobile.service;

import com.cheng.weixin.common.utils.StringFormat;
import com.cheng.weixin.rpc.user.entity.Account;
import com.cheng.weixin.rpc.user.service.RpcUserService;
import com.cheng.weixin.web.mobile.result.user.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Desc: 用户
 * Author: hp
 * Date: 2016/9/30
 */
@Service
public class SysUserService {
    @Autowired
    private RpcUserService userService;

    public UserDetail getUserDetail() {
        UserDetail userDetail = new UserDetail();
        Account account = userService.getAccount("1");
        userDetail.setId(account.getId());
        userDetail.setNickname(account.getNickname());
        userDetail.setAccountLevel(account.getAccountLevelId());
        userDetail.setBalance(StringFormat.format(account.getBalance()));
        userDetail.setBonusPointRecord(account.getBonusPointUsable());
        userDetail.setCouponRecord("1"); // TODO 优惠券个数
        userDetail.setUsername(account.getUsername());

        return userDetail;
    }
}
