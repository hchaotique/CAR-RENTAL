package com.group1.car_rental.repository;

import com.group1.car_rental.entity.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewsRepository extends JpaRepository<Reviews, Long> {

    List<Reviews> findByBookingId(Long bookingId);

    List<Reviews> findByFromUserId(Long fromUserId);

    List<Reviews> findByToUserId(Long toUserId);
}
