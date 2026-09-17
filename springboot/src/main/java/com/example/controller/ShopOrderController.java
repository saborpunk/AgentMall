package com.example.controller;

import com.example.common.Result;
import com.example.entity.OrderCreateRequest;
import com.example.entity.ShopOrder;
import com.example.service.ShopOrderService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shopOrder")
public class ShopOrderController {

    @Resource
    private ShopOrderService shopOrderService;

    @PostMapping("/create")
    public Result create(@RequestBody OrderCreateRequest request) {
        ShopOrder order = shopOrderService.create(request);
        return Result.success(order);
    }

    @PutMapping("/ship/{id}")
    public Result ship(@PathVariable Integer id) {
        shopOrderService.ship(id);
        return Result.success();
    }

    @PutMapping("/finish/{id}")
    public Result finish(@PathVariable Integer id) {
        shopOrderService.finish(id);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Integer id) {
        shopOrderService.cancel(id);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ShopOrder shopOrder) {
        List<ShopOrder> list = shopOrderService.selectAll(shopOrder);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ShopOrder shopOrder,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ShopOrder> pageInfo = shopOrderService.selectPage(shopOrder, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
