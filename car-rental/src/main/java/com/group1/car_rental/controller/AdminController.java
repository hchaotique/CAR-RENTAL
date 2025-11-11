package com.group1.car_rental.controller;

import com.group1.car_rental.entity.User;
import com.group1.car_rental.entity.Cars;
import com.group1.car_rental.entity.Complaint; // <-- THÊM IMPORT
import com.group1.car_rental.service.UserService;
import com.group1.car_rental.service.CarsService;
import com.group1.car_rental.service.ComplaintService; // <-- THÊM IMPORT
import com.group1.car_rental.repository.UserRepository;
import com.group1.car_rental.repository.CarsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor 
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CarsRepository carsRepository;
    private final CarsService carsService;
    private final ComplaintService complaintService; // <-- THÊM DỊCH VỤ MỚI

    // ===================================
    // === QUẢN LÝ NGƯỜI DÙNG ===
    // ===================================

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/list_users"; 
    }

    @PostMapping("/users/toggle-active/{id}")
    public String toggleUserActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // (Giữ nguyên logic cũ)
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

        User currentUser = userService.getCurrentUserWithProfile();
        if (user.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể tự khóa chính mình!");
            return "redirect:/admin/users";
        }

        user.setIsActive(!user.getIsActive()); 
        userRepository.save(user);

        String status = user.getIsActive() ? "kích hoạt" : "vô hiệu hóa";
        redirectAttributes.addFlashAttribute("success", "Đã " + status + " tài khoản: " + user.getEmail());
        return "redirect:/admin/users";
    }

    @PostMapping("/users/change-role/{id}")
    public String changeUserRole(@PathVariable Long id, 
                                 @RequestParam String newRole, 
                                 RedirectAttributes redirectAttributes) {
        // (Giữ nguyên logic cũ)
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

        User currentUser = userService.getCurrentUserWithProfile();
        if (user.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể tự thay đổi vai trò của mình!");
            return "redirect:/admin/users";
        }
        
        if (!"CUSTOMER".equals(newRole) && !"HOST".equals(newRole) && !"ADMIN".equals(newRole)) {
            redirectAttributes.addFlashAttribute("error", "Vai trò không hợp lệ!");
            return "redirect:/admin/users";
        }

        user.setRole(newRole);
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật vai trò cho " + user.getEmail() + " thành " + newRole);
        return "redirect:/admin/users";
    }

    // ===================================
    // === QUẢN LÝ XE ===
    // ===================================

    @GetMapping("/cars")
    public String listAllCars(Model model) {
        List<Cars> allCars = carsRepository.findAll();
        model.addAttribute("cars", allCars);
        return "admin/list_cars";
    }

    @PostMapping("/cars/delete/{id}")
    public String deleteCarAsAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            carsService.deleteVehicleAsAdmin(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa xe (ID: " + id + ") thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa xe: " + e.getMessage());
        }
        return "redirect:/admin/cars";
    }
    
    // ===================================
    // === QUẢN LÝ KHIẾU NẠI (MỚI) ===
    // ===================================

    /**
     * Hiển thị danh sách TẤT CẢ khiếu nại
     */
    @GetMapping("/complaints")
    public String listComplaints(Model model) {
        List<Complaint> complaints = complaintService.getAllComplaints();
        model.addAttribute("complaints", complaints);
        return "admin/list_complaints"; // Cần tạo file: templates/admin/list_complaints.html
    }

    /**
     * Hiển thị chi tiết 1 khiếu nại để giải quyết
     */
    @GetMapping("/complaints/view/{id}")
    public String viewComplaint(@PathVariable Long id, Model model) {
        try {
            Complaint complaint = complaintService.getComplaintById(id);
            model.addAttribute("complaint", complaint);
            return "admin/view_complaint"; // Cần tạo file: templates/admin/view_complaint.html
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/complaints";
        }
    }
    
    /**
     * Xử lý giải quyết khiếu nại
     */
    @PostMapping("/complaints/resolve/{id}")
    public String resolveComplaint(@PathVariable Long id,
                                   @RequestParam String adminNotes,
                                   @RequestParam String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            complaintService.resolveComplaint(id, adminNotes, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật khiếu nại ID: " + id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/complaints/view/" + id;
        }
        return "redirect:/admin/complaints";
    }
}