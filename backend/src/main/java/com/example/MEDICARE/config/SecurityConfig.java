package com.example.MEDICARE.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(401);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                res.getWriter().write("{\"message\":\"Please sign in to continue.\"}");
            }))
            .authorizeHttpRequests(auth -> auth
                // ---- public: sign-in, patient sign-up, email verification ----
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**").permitAll()

                // ---- admin only ----
                .requestMatchers("/api/staff/**").hasRole("ADMIN")
                .requestMatchers("/api/nurses/**").hasRole("ADMIN")
                .requestMatchers("/api/reports/summary").hasRole("ADMIN")
                .requestMatchers("/api/reports/doctor").hasRole("DOCTOR")

                // ---- patient self-service ----
                .requestMatchers("/api/patients/me/**").hasRole("PATIENT")
                .requestMatchers("/api/patients/**").hasAnyRole("ADMIN", "DOCTOR") // doctors are scoped in the controller

                .requestMatchers(HttpMethod.GET, "/api/doctors/**").hasAnyRole("ADMIN", "DOCTOR", "PATIENT")
                .requestMatchers("/api/appointments/**").hasAnyRole("ADMIN", "DOCTOR", "PATIENT")
                .requestMatchers("/api/medical-records/**").hasAnyRole("ADMIN", "DOCTOR")

                // ---- pharmacy: doctors may look up items and administer; everything else is admin ----
                .requestMatchers(HttpMethod.GET, "/api/pharmacy/categories", "/api/pharmacy/items").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/pharmacy/administrations").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers("/api/pharmacy/**").hasRole("ADMIN")

                // ---- wards ----
                .requestMatchers(HttpMethod.GET, "/api/wards/**", "/api/admissions/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/admissions/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers("/api/wards/**").hasRole("ADMIN")

                // ---- payments: admin + the patient who owns them (scoped in the controller) ----
                .requestMatchers("/api/payments/**").hasAnyRole("ADMIN", "PATIENT")

                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
