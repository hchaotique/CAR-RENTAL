package com.group1.car_rental.controller;

import com.group1.car_rental.dto.CarsDto;
import com.group1.car_rental.dto.CarsForm;
import com.group1.car_rental.service.CarsService;
import com.group1.car_rental.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/cars")
public class CarManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CarManagementController.class);

    private final CarsService carsService;
    private final UserService userService;

    public CarManagementController(CarsService carsService, UserService userService) {
        this.carsService = carsService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public String showCarList(Model model) {
        Long ownerId = getCurrentUserId();
        List<CarsDto> vehicles = carsService.getVehiclesByOwner(ownerId);
        model.addAttribute("vehicles", vehicles);
        return "cars/list_car";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("carForm", new CarsForm());
        return "cars/add";
    }

    @PostMapping("/add")
    public String addCar(@Valid @ModelAttribute("carForm") CarsForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        Long ownerId = getCurrentUserId();
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during car creation: {}", bindingResult.getAllErrors());
            return "cars/add";
        }
        try {
            carsService.createVehicle(form, ownerId);
            redirectAttributes.addFlashAttribute("success", "Thêm xe thành công!");
            return "redirect:/cars/list";
        } catch (Exception e) {
            logger.error("Error creating car: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi thêm xe: " + e.getMessage());
            return "cars/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Long ownerId = getCurrentUserId();
        try {
            CarsDto car = carsService.getVehicleByIdAndOwner(id, ownerId);
            CarsForm form = new CarsForm();
            form.setId(car.getId());
            form.setMake(car.getMake());
            form.setModel(car.getModel());
            form.setYear(car.getYear());
            form.setTransmission(car.getTransmission());
            form.setFuelType(car.getFuelType());
            form.setSeats(car.getSeats());
            form.setDailyPrice(car.getDailyPrice());
            form.setImageUrl(car.getImageUrl());
            form.setCity(car.getCity());
            model.addAttribute("carForm", form);
            return "cars/edit";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("carForm", new CarsForm());
            return "cars/edit";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateCar(@PathVariable Long id,
                            @Valid @ModelAttribute("carForm") CarsForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        Long ownerId = getCurrentUserId();
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during car update: {}", bindingResult.getAllErrors());
            return "cars/edit";
        }
        try {
            carsService.updateVehicle(id, form, ownerId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật xe thành công!");
            return "redirect:/cars/list";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "cars/edit";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long ownerId = getCurrentUserId();
        try {
            carsService.deleteVehicle(id, ownerId);
            redirectAttributes.addFlashAttribute("success", "Xóa xe thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cars/list";
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
        return userService.getCurrentUserWithProfile().getId();
    }
}
