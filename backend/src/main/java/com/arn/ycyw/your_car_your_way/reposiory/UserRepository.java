package com.arn.ycyw.your_car_your_way.reposiory;

import com.arn.ycyw.your_car_your_way.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findById(int id);
    Optional<Users> findByVerificationToken(String verificationToken);
}
