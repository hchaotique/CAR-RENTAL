package com.group1.car_rental.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.group1.car_rental.service.CarsService;
import com.group1.car_rental.dto.CarsDto;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final CarsService CarsService;

    @GetMapping("/search")
    public String search(@RequestParam String location,
                         @RequestParam(required = false) String pickupDate,
                         @RequestParam(required = false) String pickupTime,
                         @RequestParam(required = false) String returnDate,
                         @RequestParam(required = false) String returnTime,
                         @RequestParam(required = false) String latitude,
                         @RequestParam(required = false) String longitude,
                         Model model) {
        List<CarsDto> searchResults = CarsService.searchByLocation(location);
        model.addAttribute("Cars", searchResults);
        model.addAttribute("location", location);
        model.addAttribute("pickupDate", pickupDate);
        model.addAttribute("pickupTime", pickupTime);
        model.addAttribute("returnDate", returnDate);
        model.addAttribute("returnTime", returnTime);
        return "search";
    }
}
