package com.cheng.weixin.web.mobile.service;

import com.cheng.weixin.common.utils.StringFormat;
import com.cheng.weixin.common.utils.StringUtils;
import com.cheng.weixin.rpc.cart.model.ProductModel;
import com.cheng.weixin.rpc.cart.service.RpcCartService;
import com.cheng.weixin.rpc.item.entity.Product;
import com.cheng.weixin.rpc.item.service.RpcProductService;
import com.cheng.weixin.rpc.order.entity.DeliveryTime;
import com.cheng.weixin.rpc.order.entity.OrderInfo;
import com.cheng.weixin.rpc.order.entity.Pay;
import com.cheng.weixin.rpc.order.enumType.OrderType;
import com.cheng.weixin.rpc.order.enumType.PayStatus;
import com.cheng.weixin.rpc.order.service.RpcOrderService;
import com.cheng.weixin.rpc.user.entity.*;
import com.cheng.weixin.rpc.user.enumType.BehaviorType;
import com.cheng.weixin.rpc.user.service.RpcUserService;
import com.cheng.weixin.web.mobile.exception.BusinessException;
import com.cheng.weixin.web.mobile.result.order.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Desc:
 * Author: 光灿
 * Date: 2016/9/14
 */
@Service("sysOrderService")
public class SysOrderService {
    @Autowired
    private RpcUserService userService;
    @Autowired
    private RpcOrderService orderService;
    @Autowired
    private RpcCartService cartService;
    @Autowired
    private RpcProductService productService;

    public SubmitOrderInfo buy() {

        SubmitOrderInfo submitOrder = new SubmitOrderInfo();

        // 配送地址
        DeliveryAddress address = userService.getDefaultAddress("1");
        submitOrder.setConsignee(address.getConsignee());
        submitOrder.setMobile(address.getMobile());
        submitOrder.setAddress(address.getAddress());

        // 配送时间
        List<DeliveryTime> time = orderService.getAllDeliveryTime();
        List<OrderDeliveryTime> orderDeliveryTimes = new ArrayList<>();
        for (DeliveryTime deliveryTime : time) {
            orderDeliveryTimes.add(new OrderDeliveryTime(deliveryTime.getId(), deliveryTime.getName()));
        }
        submitOrder.setDeliveryTime(orderDeliveryTimes);

        // 支付方式
        List<Pay> pays = orderService.getAllPay();
        List<OrderPay> orderPays = new ArrayList<>();
        for (Pay pay : pays) {
            orderPays.add(new OrderPay(pay.getId(), pay.getName()));
        }
        submitOrder.setPays(orderPays);

        // 优惠券

        // 余额
        Account account = userService.getAccount("1");
        submitOrder.setAvailableBalance(StringFormat.format(account.getBalance()));

        // 商品详情
        int totalProductNums = 0;
        BigDecimal totalProductPrice = new BigDecimal(0);
        List<ProductModel> productModels = cartService.getChooseProductInfo("1");
        String[] productImgs = new String[productModels.size()];
        for (int i=0; i<productModels.size(); i++) {
            Product product = productService.getDefaultPictureById(productModels.get(i).getId());
            productImgs[i] = product.getDefaultPicture().getPictureUrl();
            totalProductNums+=productModels.get(i).getCount();
            totalProductPrice = totalProductPrice.add(product.getSalePrice().multiply(new BigDecimal(productModels.get(i).getCount())));
        }
        submitOrder.setTotalProductNums(totalProductNums);
        submitOrder.setProductImgs(productImgs);
        submitOrder.setTotalProductPrice(StringFormat.format(totalProductPrice));

        // 运费
        BigDecimal freight = new BigDecimal(5.00);
        submitOrder.setFreight(StringFormat.format(freight));

        // 优惠券
        BigDecimal couponRecord = new BigDecimal(0.00);
        submitOrder.setCouponRecord(StringFormat.format(couponRecord));

        // 总得价格
        submitOrder.setTotalPrice(StringFormat.format(totalProductPrice.add(freight)));

        return submitOrder;
    }

    public void submitOrder() {
        BigDecimal totalProductPrice = new BigDecimal(0);
        List<ProductModel> productModels = cartService.getChooseProductInfo("1");
        for (int i=0; i<productModels.size(); i++) {
            Product product = productService.getById(productModels.get(i).getId());
            totalProductPrice = totalProductPrice.add(product.getSalePrice().multiply(new BigDecimal(productModels.get(i).getCount())));
        }
        cartService.deletedChooseProduct("1");

        // 生成订单
        OrderInfo order = new OrderInfo();
        String oid = RandomStringUtils.randomNumeric(8);
        order.setOid(oid);
        order.setAccountId("1");
        // 配送地址
        DeliveryAddress address = userService.getDefaultAddress("1");
        order.setConsignee(address.getConsignee());
        order.setMobile(address.getMobile());
        order.setVillageId(address.getVillageId());
        order.setAddress(address.getAddress());
        order.setPostCode(address.getPostCode());
        order.setTelephone(address.getTelephone());
        order.setEmail(address.getEmail());

        order.setPayStatus(PayStatus.FREIGHTCOLLECT); // TODO 暂时定为到付
        order.setFlowStatus("已支付");
        order.setDeliveryTimeId("2016年9月29日");
        order.setPayId("1");
        order.setDeliveryTypeId("1");
        //order.setArayacakAddress("刘楼");
        //order.setArayacakDeliveryTime("2016年9月29日");
        order.setOrderType(OrderType.NORMAL);

        // 是否是用余额支付
        if(true) {
            order.setBalanceOffset(BigDecimal.valueOf(6L));
        }


        order.setFreightPayable(new BigDecimal(2)); // 应付运费
        order.setFreightReduce(BigDecimal.ZERO); // 运费优惠
        order.setProductTotalPrice(totalProductPrice); // 商品总金额
        order.setDiscount(BigDecimal.ZERO); // 优惠金额
        order.setCouponReducePrice(BigDecimal.ZERO); // 券优惠
        order.setBonusPointReducePrice(BigDecimal.ZERO); // 积分优惠
        // 应付金额 = 应付运费 - 运费优惠 + 商品总金额 - 优惠金额 - 券优惠 - 积分优惠
        order.setAmountPayable(order.getFreightPayable().subtract(order.getFreightReduce()).add(totalProductPrice)
                .subtract(order.getDiscount()).subtract(order.getCouponReducePrice()).subtract(order.getBonusPointReducePrice()));
        // 已付金额 = 应付运费 - 运费优惠 + 商品总金额 - 优惠金额 - 券优惠 - 积分优惠 - 余额抵扣
        BigDecimal amountPaid = order.getFreightPayable().subtract(order.getFreightReduce()).add(totalProductPrice)
                                .subtract(order.getDiscount()).subtract(order.getCouponReducePrice()).subtract(order.getBonusPointReducePrice());
        if (true) { // 是否是用余额支付
            amountPaid = amountPaid.subtract(BigDecimal.valueOf(6L));
        }
        order.setAmountPaid(amountPaid);
        order.setRemarkCustomer("备注");
        order.setIp("127.0.0.1");
        order.setPayTime(new Date());
        order.setCouponCode("122355");
        order.setFreeAccountLevel(Boolean.FALSE);
        orderService.addOrder(order);

        // 用户金额操作
        userService.updateAccountBalance("1", totalProductPrice);

        // 更新记录（积分记录、券记录、现金记录）
        Behavior behavior = new Behavior();
        behavior.setBehaviorType(BehaviorType.CASH);
        behavior.setNanme("下单");
        behavior.setNanme(oid);
        userService.addBehavior(behavior);

        // 积分记录
        BonusPointRecord bonusPoint = userService.getBonusPointRecord("1");
        BonusPointRecord bonusPointRecord = new BonusPointRecord();
        bonusPointRecord.setTxBonusPoints(10);// TODO 本次订单获取的积分
        bonusPointRecord.setFrozenBonusPoints(10);
        if (bonusPoint != null) {
            bonusPointRecord.setBeforeBonusPoints(bonusPoint.getAfterBonusPoints());
            bonusPointRecord.setAfterBonusPoints(bonusPoint.getAfterBonusPoints()+10);
        }else {
            bonusPointRecord.setBeforeBonusPoints(0);
            bonusPointRecord.setAfterBonusPoints(10);
        }
        bonusPointRecord.setBehaviorId(behavior.getId());
        bonusPointRecord.setTxResult("结果"); //TODO
        userService.addBonusPointRecord(bonusPointRecord);

        // 券记录
        //CouponRecord record  = userService.getCouponRecordByUser("1");
        CouponRecord couponRecord = new CouponRecord();
        //couponRecord.setId(record.getId());
        couponRecord.setCouponCodeId("123");
        couponRecord.setCouponCodeId("1");
        couponRecord.setTxType("支出");
        couponRecord.setBehaviorId(behavior.getId());
        couponRecord.setTxResult("结果");
        userService.addCouponRecord(couponRecord);

        // 现金记录
        CashRecord cash = userService.getCashRecord("1");
        if (cash == null) {
            throw new BusinessException("请先充值");
        }

        CashRecord cashRecord = new CashRecord();
        //cashRecord.setId(cash.getId());
        cashRecord.setTxMoney(totalProductPrice);
        cashRecord.setBeforeMoney(cash.getAfterBonusPoints());
        BigDecimal afterBonusPoints = cash.getAfterBonusPoints().subtract(totalProductPrice);
        if (afterBonusPoints.compareTo(BigDecimal.valueOf(-1L)) == -1) {
            throw new BusinessException("余额不足");
        }
        cashRecord.setAfterBonusPoints(afterBonusPoints);
        cashRecord.setTxType("支出");
        cashRecord.setBehaviorId(behavior.getId());
        cashRecord.setTxResult("结果");
        userService.addCashRecord(cashRecord);
    }


    public List<OrderList> getOrders() {
        List<OrderInfo> orderInfos = orderService.getOrderInfos("1");
        List<OrderList> orders = new ArrayList<>();
        for (OrderInfo order : orderInfos) {
            OrderList orderList = new OrderList();
            orderList.setId(order.getId());
            orderList.setDate(new DateTime(order.getCreateDate()).toString("dd-MM-yyyy HH:mm:ss"));
            orderList.setOrderNum(order.getOid());
            orderList.setNumber(order.getOrderDetails().size()+"");
            orderList.setStatus(order.getFlowStatus().split("-"));
            // 总金额 = 商品总金额 - 余额抵扣 - 优惠金额 + 应付运费
            //orderList.setTotalPrice(StringFormat.format(
            //        order.getProductTotalPrice().subtract(order.getBalanceOffset())
            //        .subtract(order.getDiscount()).add(order.getFreightPayable())));

            orderList.setTotalPrice(StringFormat.format(order.getAmountPayable()));
            String orderStatus = "";
            if (PayStatus.NONPAYMENT.equals(order.getPayStatus())) {
                orderStatus = "INVALID";
            } else if (StringUtils.isBlank(order.getCommentId())) {
                orderStatus = "WAITCOMMENT";
                orderList.setCommentId(order.getCommentId());
            }
            orderList.setOrderStatus(orderStatus);
            orders.add(orderList);
        }
        return orders;
    }

    public OrderDetail getOrderDetail() {
        OrderDetail detail = new OrderDetail();
        OrderInfo orderInfo = orderService.getOrderDetail("1700b55803cc44dfa08bac0028bee46f");
        detail.setId(orderInfo.getId());
        String[] flowStatus = orderInfo.getFlowStatus().split("-");
        detail.setStatus(flowStatus[flowStatus.length-1]);
        detail.setOid(orderInfo.getOid());
        detail.setConsignee(orderInfo.getConsignee());
        detail.setAddress(orderInfo.getAddress());

        Pay pay = orderService.getPay(orderInfo.getPayId());

        detail.setPayWay(pay.getName());
        detail.setProductTotalPrice(StringFormat.format(orderInfo.getProductTotalPrice()));
        detail.setFreightPayable(StringFormat.format(orderInfo.getFreightPayable()));
        detail.setCouponReducePrice(StringFormat.format(orderInfo.getCouponReducePrice()));
        detail.setBalanceOffset(StringFormat.format(orderInfo.getBalanceOffset()));
        // 总金额 = 商品总金额 - 余额抵扣 - 优惠金额 + 应付运费
        //detail.setTotalPrice(StringFormat.format(
        //        orderInfo.getProductTotalPrice().subtract(orderInfo.getBalanceOffset())
        //        .subtract(orderInfo.getDiscount()).add(orderInfo.getFreightPayable())));
        detail.setTotalPrice(StringFormat.format(orderInfo.getAmountPayable()));
        String orderStatus = "";
        if (orderInfo.getPayStatus().equals(PayStatus.NONPAYMENT)) {
            orderStatus = "INVALID";
        } else if (StringUtils.isBlank(orderInfo.getCommentId())) {
            orderStatus = "WAITCOMMENT";
            detail.setCommentId(orderInfo.getCommentId());
        }
        detail.setOrderStatus(orderStatus);
        return detail;
    }

}
