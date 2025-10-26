package com.group1.car_rental.controller;

import com.group1.car_rental.entity.Bookings;
import com.group1.car_rental.entity.Payments;
import com.group1.car_rental.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // 1. Check Availability
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam Long listingId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        boolean available = bookingService.checkAvailability(listingId, startDate, endDate);
        return ResponseEntity.ok(Map.of(
            "available", available,
            "listingId", listingId,
            "startDate", startDate,
            "endDate", endDate
        ));
    }

    // 2. Hold Slot
    @PostMapping("/hold")
    public ResponseEntity<Map<String, Object>> holdSlot(
            @RequestParam Long listingId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        UUID holdToken = bookingService.holdSlot(listingId, startDate, endDate, idempotencyKey);

        return ResponseEntity.ok(Map.of(
            "holdToken", holdToken,
            "expiresInMinutes", 15
        ));
    }

    // 3. Create Booking
    @PostMapping
    public ResponseEntity<Bookings> createBooking(
            @RequestBody Bookings booking,
            @RequestParam UUID holdToken) {

        Bookings createdBooking = bookingService.createBooking(booking, holdToken);
        return ResponseEntity.ok(createdBooking);
    }

    // 4. Authorize Payment
    @PostMapping("/{bookingId}/authorize")
    public ResponseEntity<Payments> authorizePayment(
            @PathVariable Long bookingId,
            @RequestParam String provider,
            @RequestParam String providerRef) {

        // In a real implementation, you'd retrieve the booking from repository
        // For now, we'll assume it's passed or retrieved
        Bookings booking = new Bookings(); // Placeholder
        booking.setId(bookingId);

        Payments payment = bookingService.authorizePayment(booking, provider, providerRef);
        return ResponseEntity.ok(payment);
    }

    // 5. Confirm Booking (Host)
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<String> confirmBooking(@PathVariable Long bookingId) {
        bookingService.confirmBooking(bookingId);
        return ResponseEntity.ok("Booking confirmed");
    }

    // 6. Start Trip (Check-in)
    @PostMapping("/{bookingId}/checkin")
    public ResponseEntity<String> checkIn(@PathVariable Long bookingId) {
        bookingService.startTrip(bookingId);
        return ResponseEntity.ok("Trip started");
    }

    // 7. Complete Trip (Check-out)
    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<String> checkOut(@PathVariable Long bookingId) {
        bookingService.completeTrip(bookingId);
        return ResponseEntity.ok("Trip completed");
    }
}
