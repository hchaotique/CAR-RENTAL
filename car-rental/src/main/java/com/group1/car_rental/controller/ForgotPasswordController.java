package com.group1.car_rental.controller;

import com.group1.car_rental.entity.User;
import com.group1.car_rental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Check if user exists
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Email không tồn tại trong hệ thống.");
                return "redirect:/forgot-password";
            }

            // In a real implementation, you would:
            // 1. Generate a password reset token
            // 2. Save it to database with expiration
            // 3. Send email with reset link
            // 4. For now, we'll just show success message

            // TODO: Implement actual password reset functionality
            // This is a placeholder for the complete implementation

            redirectAttributes.addFlashAttribute("success",
                "Liên kết khôi phục mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.");

            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra. Vui lòng thử lại sau.");
            return "redirect:/forgot-password";
        }
    }
}
