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

    @Enumerated(EnumType.STRING)
    @Column(name = "home_city", nullable = false, length = 20)
    private CityEnum homeCity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Integer version = 0;

    public CarListings(Cars vehicle, String title, Integer price24hCents, Point homeLocation, CityEnum homeCity) {
        this.vehicle = vehicle;
        this.title = title;
        this.price24hCents = price24hCents;
        this.homeLocation = homeLocation;
        this.homeCity = homeCity;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public Cars getVehicle() { return vehicle; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getPrice24hCents() { return price24hCents; }
    public Integer getKmLimit24h() { return kmLimit24h; }
    public Boolean getInstantBook() { return instantBook; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public String getStatus() { return status; }
    public Point getHomeLocation() { return homeLocation; }
    public CityEnum getHomeCity() { return homeCity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Integer getVersion() { return version; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setVehicle(Cars vehicle) { this.vehicle = vehicle; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice24hCents(Integer price24hCents) { this.price24hCents = price24hCents; }
    public void setKmLimit24h(Integer kmLimit24h) { this.kmLimit24h = kmLimit24h; }
    public void setInstantBook(Boolean instantBook) { this.instantBook = instantBook; }
    public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
    public void setStatus(String status) { this.status = status; }
    public void setHomeLocation(Point homeLocation) { this.homeLocation = homeLocation; }
    public void setHomeCity(CityEnum homeCity) { this.homeCity = homeCity; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Integer version) { this.version = version; }
}
