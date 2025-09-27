package com.group1.car_rental.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.group1.car_rental.entity.Vehicle;
import com.group1.car_rental.entity.User;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwner(User owner);

    @Query("SELECT v FROM Vehicle v WHERE LOWER(v.make) LIKE LOWER(CONCAT('%', :location, '%')) OR LOWER(v.model) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Vehicle> searchByLocation(@Param("location") String location);
}
