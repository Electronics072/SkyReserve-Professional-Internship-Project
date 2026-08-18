package com.skyreserve.config;

import com.skyreserve.model.Flight;
import com.skyreserve.model.FlightSchedule;
import com.skyreserve.model.Role;
import com.skyreserve.model.User;
import com.skyreserve.repository.FlightRepository;
import com.skyreserve.repository.FlightScheduleRepository;
import com.skyreserve.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner init(UserRepository users,
                           FlightRepository flights,
                           FlightScheduleRepository schedules,
                           PasswordEncoder enc) {
        return args -> {
            if (users.findByEmail("admin@skyreserve.local").isEmpty()) {
                users.save(new User("System Administrator", "admin@skyreserve.local", enc.encode("admin123"), Role.ADMIN));
            }
            if (users.findByEmail("user@skyreserve.local").isEmpty()) {
                users.save(new User("Demo Passenger", "user@skyreserve.local", enc.encode("user123"), Role.USER));
            }
            if (flights.count() == 0) {
                Flight a = flights.save(new Flight("SR101", "SkyReserve Airlines", "Visakhapatnam", "Hyderabad", "08:30", "10:00", 3500, 6500, 30));
                Flight b = flights.save(new Flight("SR205", "SkyReserve Airlines", "Visakhapatnam", "Chennai", "14:00", "15:30", 3800, 7000, 30));
                Flight c = flights.save(new Flight("SR310", "SkyReserve Airlines", "Hyderabad", "Delhi", "18:00", "20:20", 5200, 9000, 30));
                Flight d2 = flights.save(new Flight("SR401", "SkyReserve Airlines", "Mumbai", "Delhi", "07:15", "09:20", 4800, 8200, 30));
                Flight e2 = flights.save(new Flight("SR402", "SkyReserve Airlines", "Mumbai", "Hyderabad", "13:30", "15:05", 4600, 7800, 30));
                Flight f2 = flights.save(new Flight("SR501", "SkyReserve Airlines", "Bengaluru", "Mumbai", "16:10", "17:55", 4300, 7600, 30));
                for (int i = 1; i <= 1; i++) {
                    LocalDate d = LocalDate.now().plusDays(i);
                    schedules.save(new FlightSchedule(a, d));
                    schedules.save(new FlightSchedule(b, d));
                    schedules.save(new FlightSchedule(c, d));
                    schedules.save(new FlightSchedule(d2, d));
                    schedules.save(new FlightSchedule(e2, d));
                    schedules.save(new FlightSchedule(f2, d));
                }
            }
        };
    }
}
