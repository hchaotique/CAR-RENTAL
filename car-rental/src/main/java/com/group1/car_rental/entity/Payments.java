package com.group1.car_rental.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(nullable = false, length = 12)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Payments(Bookings booking, String type, Integer amountCents, String provider) {
        this.booking = booking;
        this.type = type;
        this.amountCents = amountCents;
        this.provider = provider;
        this.createdAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public Bookings getBooking() { return booking; }
    public String getType() { return type; }
    public Integer getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public String getProviderRef() { return providerRef; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setBooking(Bookings booking) { this.booking = booking; }
    public void setType(String type) { this.type = type; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setProviderRef(String providerRef) { this.providerRef = providerRef; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
