package com.group1.car_rental.config;

import com.group1.car_rental.service.DbUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DbUserDetailsService dbUserDetailsService;

    public SecurityConfig(DbUserDetailsService dbUserDetailsService) {
        this.dbUserDetailsService = dbUserDetailsService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth


                .requestMatchers("/", "/register", "/login", "/search", "/css/**", "/js/**", "/images/**").permitAll()

                // Đường dẫn cho HOST (Quản lý xe của chính họ)
                .requestMatchers("/cars/list", "/cars/add", "/cars/edit/**", "/cars/delete/**").hasRole("HOST")

                // Đường dẫn cho HOST (Quản lý xe của chính họ)
                .requestMatchers("/rentals/**").hasRole("CUSTOMER")
                 // Đường dẫn cho ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                

                .requestMatchers("/complaints/submit").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .userDetailsService(dbUserDetailsService);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}