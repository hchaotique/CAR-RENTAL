package com.group1.car_rental.controller;

import com.group1.car_rental.entity.Cars;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.group1.car_rental.service.CarsService;
import com.group1.car_rental.dto.CarsDto;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CarsService CarsService;

    @GetMapping("/")
    public String home(Model model) {
        List<CarsDto> cars = CarsService.getAllCars();
        model.addAttribute("cars", cars);
        return "home/index";
    }
}
