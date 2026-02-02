package com.arn.ycyw.your_car_your_way.security;

import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.reposiory.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username = ce que l'utilisateur tape (email ou username)
        Users user = userRepository.findByEmail(username) // ou findByUsername si tu préfères
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UsersDetailsAdapter(user);
    }
}
