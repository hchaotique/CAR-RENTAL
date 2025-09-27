package com.group1.car_rental.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.group1.car_rental.service.VehicleService;
import com.group1.car_rental.dto.VehicleDto;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final VehicleService vehicleService;

    @GetMapping("/search")
    public String search(@RequestParam String location,
                         @RequestParam(required = false) String pickupDate,
                         @RequestParam(required = false) String pickupTime,
                         @RequestParam(required = false) String returnDate,
                         @RequestParam(required = false) String returnTime,
                         @RequestParam(required = false) String latitude,
                         @RequestParam(required = false) String longitude,
                         Model model) {
        List<VehicleDto> searchResults = vehicleService.searchByLocation(location);
        model.addAttribute("vehicles", searchResults);
        model.addAttribute("location", location);
        model.addAttribute("pickupDate", pickupDate);
        model.addAttribute("pickupTime", pickupTime);
        model.addAttribute("returnDate", returnDate);
        model.addAttribute("returnTime", returnTime);
        return "search";
    }
}
