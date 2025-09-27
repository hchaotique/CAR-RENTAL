package com.group1.car_rental.controller;

import com.group1.car_rental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registrationForm") RegistrationForm form,
                           RedirectAttributes redirectAttributes) {
        try {
            // Validate password match
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu không khớp");
                return "redirect:/register";
            }

            // Log form data
            System.out.println("Registering user: email=" + form.getEmail() + ", fullName=" + form.getFullName() + ", password length=" + form.getPassword().length());

            // Register user
            userService.register(form.getEmail(), form.getPassword(), form.getFullName());

            // Success
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (UserService.DuplicateEmailException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống. Vui lòng thử lại.");
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpServletRequest request) {
        model.addAttribute("error", request.getParameter("error"));
        return "user/login";
    }

    @GetMapping("/profile")
    public String showProfileForm(Model model) {
        model.addAttribute("user", userService.getCurrentUserWithProfile());
        model.addAttribute("profileForm", new ProfileUpdateForm());
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("profileForm") @Valid ProfileUpdateForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getCurrentUserWithProfile());
            return "user/profile";
        }
        try {
            userService.updateProfile(form.getFullName(), form.getPhone());
            redirectAttributes.addFlashAttribute("success", "Cập nhật thành công");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("user", userService.getCurrentUserWithProfile());
            model.addAttribute("error", e.getMessage());
            return "user/profile";
        } catch (Exception e) {
            model.addAttribute("user", userService.getCurrentUserWithProfile());
            model.addAttribute("error", "Lỗi hệ thống. Vui lòng thử lại.");
            return "user/profile";
        }
    }

    public static class RegistrationForm {
        private String email;
        private String password;
        private String confirmPassword;
        private String fullName;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    public static class ProfileUpdateForm {
        @jakarta.validation.constraints.NotBlank(message = "Tên đầy đủ không được để trống")
        @jakarta.validation.constraints.Size(max = 120, message = "Tên đầy đủ quá dài")
        private String fullName;

        @jakarta.validation.constraints.Size(max = 30, message = "Số điện thoại quá dài")
        private String phone;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
