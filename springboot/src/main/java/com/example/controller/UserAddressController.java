package com.example.controller;

import com.example.common.Result;
import com.example.entity.UserAddress;
import com.example.service.UserAddressService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/userAddress")
public class UserAddressController {

    @Resource
    private UserAddressService userAddressService;

    @PostMapping("/add")
    public Result add(@RequestBody UserAddress userAddress) {
        userAddressService.add(userAddress);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody UserAddress userAddress) {
        userAddressService.updateById(userAddress);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        userAddressService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/selectByUserId/{userId}")
    public Result selectByUserId(@PathVariable Integer userId) {
        List<UserAddress> list = userAddressService.selectByUserId(userId);
        return Result.success(list);
    }

    @PutMapping("/setDefault/{id}/{userId}")
    public Result setDefault(@PathVariable Integer id, @PathVariable Integer userId) {
        userAddressService.setDefault(id, userId);
        return Result.success();
    }
}
