package com.group1.car_rental.service;

import com.group1.car_rental.entity.User;
import com.group1.car_rental.entity.UserProfile;
import com.group1.car_rental.repository.UserProfileRepository;
import com.group1.car_rental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String email, String fullName, String rawPassword, String phone, String role, String timezone) {
        // Validate inputs
        if (email == null || !email.contains("@") || email.length() > 190) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (fullName == null || fullName.trim().isEmpty() || fullName.length() > 120) {
            throw new IllegalArgumentException("Tên đầy đủ không hợp lệ");
        }
        if (phone != null && !phone.matches("^((\\+84|0)[35789]\\d{8}|(\\d{4}\\s\\d{3}\\s\\d{3})|(\\d{2}\\s\\d{3}\\s\\d{3}\\s\\d{3}))?$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        if (role == null || (!"CUSTOMER".equals(role) && !"HOST".equals(role))) {
            throw new IllegalArgumentException("Loại tài khoản không hợp lệ");
        }

        // Check duplicate email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException("Email đã tồn tại");
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Create and save user
        User user = new User(email, encodedPassword, role);
        if (phone != null) {
            user.setPhone(phone.trim());
        }
        if (timezone != null) {
            user.setTimezone(timezone);
        }
        user.setUpdatedAt(java.time.Instant.now());
        User savedUser = userRepository.save(user);

        // Create and save profile
        UserProfile profile = new UserProfile(fullName.trim(), savedUser);
        profileRepository.save(profile);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User getCurrentUserWithProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails)) {
            throw new IllegalStateException("User not authenticated");
        }
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        return userRepository.findByEmailWithProfile(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public void updateProfile(String fullName, String phone) {
        // Validate
        if (fullName == null || fullName.trim().isEmpty() || fullName.length() > 120) {
            throw new IllegalArgumentException("Tên đầy đủ không hợp lệ");
        }
        if (phone != null && phone.length() > 30) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }

        User user = getCurrentUserWithProfile();
        user.getProfile().setFullName(fullName.trim());
        if (phone != null) {
            user.setPhone(phone.trim());
        }
        user.setUpdatedAt(java.time.Instant.now());
        user.getProfile().setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String message) {
            super(message);
        }
    }
}
