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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        logger.debug("Registration attempt for email: {}", form.getEmail());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during registration: {}", bindingResult.getAllErrors());
            return "user/register";
        }

        try {
            // Validate password match
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                logger.warn("Password confirmation mismatch for email: {}", form.getEmail());
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
                return "redirect:/register";
            }

            logger.debug("Calling userService.register for email: {}", form.getEmail());
            // Register user
            userService.register(form.getEmail().trim(), form.getFullName().trim(), form.getPassword(), form.getPhone(), form.getRole());

            // Role-based redirect
            String redirectUrl = "CUSTOMER".equals(form.getRole()) ? "/" : "/cars/add";
            logger.info("Registration successful for email: {}", form.getEmail());
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Chào mừng bạn đến với hệ thống.");
            return "redirect:" + redirectUrl;
        } catch (UserService.DuplicateEmailException e) {
            logger.warn("Duplicate email registration attempt: {}", form.getEmail());
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại");
            return "redirect:/register";
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error during registration for email {}: {}", form.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            logger.error("System error during registration for email {}: {}", form.getEmail(), e.getMessage(), e);
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
        @jakarta.validation.constraints.Email(message = "Email không hợp lệ")
        @jakarta.validation.constraints.NotBlank(message = "Email không được trống")
        private String email;

        @jakarta.validation.constraints.NotBlank(message = "Tên đầy đủ không được để trống")
        @jakarta.validation.constraints.Size(max = 120, message = "Tên đầy đủ quá dài")
        private String fullName;

        @jakarta.validation.constraints.NotBlank(message = "Mật khẩu không được trống")
        @jakarta.validation.constraints.Size(min = 8, max = 60, message = "Mật khẩu 8–60 ký tự")
        private String password;

        @jakarta.validation.constraints.NotBlank(message = "Xác nhận mật khẩu không được trống")
        private String confirmPassword;

        @jakarta.validation.constraints.Pattern(regexp = "^((\\+84|0)[35789]\\d{8}|(\\d{4}\\s\\d{3}\\s\\d{3})|(\\d{2}\\s\\d{3}\\s\\d{3}\\s\\d{3}))?$", message = "Số điện thoại không hợp lệ")
        private String phone;

        @jakarta.validation.constraints.NotBlank(message = "Vui lòng chọn loại tài khoản")
        @jakarta.validation.constraints.Pattern(regexp = "^(CUSTOMER|HOST)$", message = "Loại tài khoản không hợp lệ")
        private String role = "CUSTOMER";

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
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
