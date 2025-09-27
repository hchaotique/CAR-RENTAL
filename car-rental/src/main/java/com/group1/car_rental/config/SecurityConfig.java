package com.group1.car_rental.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> {}) // CSRF enabled by default
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/home/**", "/listings/**", "/search", "/api/health", "/register**", "/login**").permitAll()
        .requestMatchers("/css/**", "/js/**", "/img/**").permitAll() // static
        .anyRequest().authenticated()
      )
      .formLogin(login -> login
        .loginPage("/login")
        .defaultSuccessUrl("/", true)
        .failureUrl("/login?error")
        .permitAll()
      )
      .logout(logout -> logout.permitAll());
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
