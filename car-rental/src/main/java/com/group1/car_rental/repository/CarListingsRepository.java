package com.group1.car_rental.repository;

import com.group1.car_rental.entity.CarListings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarListingsRepository extends JpaRepository<CarListings, Long> {
}
