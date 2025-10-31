package com.group1.car_rental.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "car_listings")
@Data
@NoArgsConstructor
public class CarListings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Cars vehicle;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "price_24h_cents", nullable = false)
    private Integer price24hCents;

    @Column(name = "km_limit_24h", nullable = false)
    private Integer kmLimit24h = 200;

    @Column(name = "instant_book", nullable = false)
    private Boolean instantBook = false;

    @Column(name = "cancellation_policy", nullable = false, length = 16)
    private String cancellationPolicy = "MODERATE";

    @Column(nullable = false, length = 20)
    private String status = "PENDING_REVIEW";

    @Column(name = "home_location", nullable = false, columnDefinition = "geography")
    private Point homeLocation;

    @Column(name = "home_city", nullable = false, length = 20)
    private String homeCity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Integer version = 0;

    public CarListings(Cars vehicle, String title, Integer price24hCents, Point homeLocation, String homeCity) {
        this.vehicle = vehicle;
        this.title = title;
        this.price24hCents = price24hCents;
        this.homeLocation = homeLocation;
        this.homeCity = homeCity;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
