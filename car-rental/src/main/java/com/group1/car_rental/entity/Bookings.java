package com.group1.car_rental.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
public class Bookings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "listing_id", nullable = false)
    private CarListings listing;

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "pickup_location", columnDefinition = "geography")
    private Point pickupLocation;

    @Column(name = "dropoff_location", columnDefinition = "geography")
    private Point dropoffLocation;

    @Column(nullable = false, length = 24)
    private String status = "PENDING_HOST";

    @Column(name = "policy_snapshot", columnDefinition = "nvarchar(max)")
    private String policySnapshot;

    @Column(name = "price_snapshot", columnDefinition = "nvarchar(max)")
    private String priceSnapshot;

    @Column(name = "hold_token")
    private UUID holdToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Integer version = 0;

    public Bookings(CarListings listing, User guest, Instant startAt, Instant endAt) {
        this.listing = listing;
        this.guest = guest;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public CarListings getListing() { return listing; }
    public User getGuest() { return guest; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Point getPickupLocation() { return pickupLocation; }
    public Point getDropoffLocation() { return dropoffLocation; }
    public String getStatus() { return status; }
    public String getPolicySnapshot() { return policySnapshot; }
    public String getPriceSnapshot() { return priceSnapshot; }
    public UUID getHoldToken() { return holdToken; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Integer getVersion() { return version; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setListing(CarListings listing) { this.listing = listing; }
    public void setGuest(User guest) { this.guest = guest; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public void setPickupLocation(Point pickupLocation) { this.pickupLocation = pickupLocation; }
    public void setDropoffLocation(Point dropoffLocation) { this.dropoffLocation = dropoffLocation; }
    public void setStatus(String status) { this.status = status; }
    public void setPolicySnapshot(String policySnapshot) { this.policySnapshot = policySnapshot; }
    public void setPriceSnapshot(String priceSnapshot) { this.priceSnapshot = priceSnapshot; }
    public void setHoldToken(UUID holdToken) { this.holdToken = holdToken; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Integer version) { this.version = version; }
}
