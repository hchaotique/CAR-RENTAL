package com.group1.car_rental.repository;

import com.group1.car_rental.entity.AvailabilityCalendar;
import com.group1.car_rental.entity.AvailabilityCalendar.AvailabilityCalendarId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilityCalendarRepository extends JpaRepository<AvailabilityCalendar, AvailabilityCalendarId> {

    @Query("SELECT a FROM AvailabilityCalendar a WHERE a.id.listingId = :listingId AND a.id.day BETWEEN :startDate AND :endDate")
    List<AvailabilityCalendar> findByListingIdAndDateRange(@Param("listingId") Long listingId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM AvailabilityCalendar a WHERE a.id.listingId = :listingId AND a.id.day = :day")
    AvailabilityCalendar findByListingIdAndDay(@Param("listingId") Long listingId, @Param("day") LocalDate day);
}
