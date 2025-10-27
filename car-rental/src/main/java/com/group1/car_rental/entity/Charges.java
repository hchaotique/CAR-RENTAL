package com.group1.car_rental.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "charges")
@Data
@NoArgsConstructor
public class Charges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @Column(name = "line_type", nullable = false, length = 20)
    private String lineType;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Charges(Bookings booking, String lineType, Integer amountCents) {
        this.booking = booking;
        this.lineType = lineType;
        this.amountCents = amountCents;
        this.createdAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public Bookings getBooking() { return booking; }
    public String getLineType() { return lineType; }
    public Integer getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setBooking(Bookings booking) { this.booking = booking; }
    public void setLineType(String lineType) { this.lineType = lineType; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setNote(String note) { this.note = note; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
