package com.group1.car_rental.repository;

import com.group1.car_rental.entity.Bookings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingsRepository extends JpaRepository<Bookings, Long> {

    @Query("SELECT b FROM Bookings b WHERE b.listing.id = :listingId AND b.startAt <= :endDate AND b.endAt >= :startDate")
    List<Bookings> findOverlappingBookings(@Param("listingId") Long listingId,
                                          @Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    @Query("SELECT b FROM Bookings b WHERE b.holdToken = :holdToken")
    Bookings findByHoldToken(@Param("holdToken") UUID holdToken);

    List<Bookings> findByGuestId(Long guestId);

    List<Bookings> findByListingVehicleOwnerId(Long ownerId);
}
