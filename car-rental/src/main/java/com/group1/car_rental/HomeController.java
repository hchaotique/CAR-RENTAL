package com.group1.car_rental;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.group1.car_rental.service.VehicleService;
import com.group1.car_rental.entity.Vehicle;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final VehicleService vehicleService;

    @GetMapping("/")
    public String home(Model model) {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        model.addAttribute("vehicles", vehicles);
        return "home/index";
    }
}
