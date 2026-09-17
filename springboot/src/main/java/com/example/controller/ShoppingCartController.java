package com.example.controller;

import com.example.common.Result;
import com.example.entity.ShoppingCart;
import com.example.service.ShoppingCartService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Resource
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCart shoppingCart) {
        shoppingCartService.add(shoppingCart);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ShoppingCart shoppingCart) {
        shoppingCartService.updateById(shoppingCart);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        shoppingCartService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        shoppingCartService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ShoppingCart shoppingCart) {
        List<ShoppingCart> list = shoppingCartService.selectAll(shoppingCart);
        return Result.success(list);
    }
}
