package com.group1.car_rental.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "availability_calendar")
@Data
@NoArgsConstructor
public class AvailabilityCalendar {

    @EmbeddedId
    private AvailabilityCalendarId id;

    @Column(nullable = false, length = 10)
    private String status = "FREE";

    @Column(name = "hold_token")
    private UUID holdToken;

    @Column(name = "hold_expire_at")
    private LocalDateTime holdExpireAt;

    public AvailabilityCalendar(AvailabilityCalendarId id) {
        this.id = id;
    }

    // Getters
    public AvailabilityCalendarId getId() { return id; }
    public String getStatus() { return status; }
    public UUID getHoldToken() { return holdToken; }
    public LocalDateTime getHoldExpireAt() { return holdExpireAt; }

    // Setters
    public void setId(AvailabilityCalendarId id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setHoldToken(UUID holdToken) { this.holdToken = holdToken; }
    public void setHoldExpireAt(LocalDateTime holdExpireAt) { this.holdExpireAt = holdExpireAt; }

    @Embeddable
    @Data
    @NoArgsConstructor
    public static class AvailabilityCalendarId implements java.io.Serializable {

        @Column(name = "listing_id", nullable = false)
        private Long listingId;

        @Column(nullable = false)
        private LocalDate day;

        public AvailabilityCalendarId(Long listingId, LocalDate day) {
            this.listingId = listingId;
            this.day = day;
        }

        // Getters for embedded ID
        public Long getListingId() { return listingId; }
        public LocalDate getDay() { return day; }

        // Setters for embedded ID
        public void setListingId(Long listingId) { this.listingId = listingId; }
        public void setDay(LocalDate day) { this.day = day; }
    }
}
