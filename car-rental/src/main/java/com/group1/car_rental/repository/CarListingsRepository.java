package com.group1.car_rental.repository;

import com.group1.car_rental.entity.CarListings;
import com.group1.car_rental.entity.CityEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CarListingsRepository extends JpaRepository<CarListings, Long> {

    // Find active listings by city, ordered by price
    List<CarListings> findByHomeCityAndStatusOrderByPrice24hCentsAsc(CityEnum homeCity, String status);

    // Find available listings in city with date filtering
    @Query("SELECT DISTINCT l FROM CarListings l " +
           "JOIN AvailabilityCalendar a ON a.id.listingId = l.id " +
           "WHERE l.homeCity = :city " +
           "AND l.status = 'ACTIVE' " +
           "AND a.id.day BETWEEN :startDate AND :endDate " +
           "AND a.status = 'FREE' " +
           "GROUP BY l.id, l.title, l.price24hCents, l.homeCity, l.status, l.vehicle, l.description, " +
           "         l.kmLimit24h, l.instantBook, l.cancellationPolicy, l.homeLocation, l.createdAt, l.updatedAt, l.version " +
           "HAVING COUNT(a) = DATEDIFF(day, :startDate, :endDate) + 1")
    List<CarListings> findAvailableByCityAndDates(@Param("city") CityEnum city,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    // Count listings by city and status
    long countByHomeCityAndStatus(CityEnum homeCity, String status);
}
