package com.aero.init;

import com.aero.domain.AppUser;
import com.aero.domain.UserRole;
import com.aero.repo.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (appUserRepository.count() == 0) {
            AppUser admin = AppUser.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@aero-404.pl")
                    .password(passwordEncoder.encode("admin"))
                    .role(UserRole.ADMIN)
                    .build();

            appUserRepository.save(admin);
            System.out.println("✓ Admin user created: admin@aero-404.pl / admin");
        }
    }
}
