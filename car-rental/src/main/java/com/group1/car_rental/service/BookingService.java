package com.group1.car_rental.service;

import com.group1.car_rental.entity.*;
import com.group1.car_rental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private AvailabilityCalendarRepository availabilityCalendarRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private IdempotencyKeysRepository idempotencyKeysRepository;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Autowired
    private ChargesRepository chargesRepository;

    @Autowired
    private OutboxEventsRepository outboxEventsRepository;

    // 1. Check Availability
    public boolean checkAvailability(Long listingId, LocalDate startDate, LocalDate endDate) {
        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(listingId, startDate, endDate);

        return calendars.stream().noneMatch(cal ->
            "HOLD".equals(cal.getStatus()) ||
            "BOOKED".equals(cal.getStatus()) ||
            "BLOCKED".equals(cal.getStatus()));
    }

    // 2. Hold Slot
    @Transactional
    public UUID holdSlot(Long listingId, LocalDate startDate, LocalDate endDate, UUID idempotencyKey) {
        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new RuntimeException("Duplicate request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        // Check availability
        if (!checkAvailability(listingId, startDate, endDate)) {
            throw new RuntimeException("Not available");
        }

        // Generate hold token
        UUID holdToken = UUID.randomUUID();

        // Set HOLD status
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            AvailabilityCalendar cal = availabilityCalendarRepository
                .findByListingIdAndDay(listingId, current);
            if (cal == null) {
                cal = new AvailabilityCalendar(new AvailabilityCalendar.AvailabilityCalendarId(listingId, current));
            }
            cal.setStatus("HOLD");
            cal.setHoldToken(holdToken);
            cal.setHoldExpireAt(LocalDateTime.now().plusMinutes(15));
            availabilityCalendarRepository.save(cal);
            current = current.plusDays(1);
        }

        return holdToken;
    }

    // 3. Create Booking
    @Transactional
    public Bookings createBooking(Bookings booking, UUID holdToken) {
        // Validate hold token
        Bookings existingHold = bookingsRepository.findByHoldToken(holdToken);
        if (existingHold != null) {
            throw new RuntimeException("Hold token already used");
        }

        // Validate availability with hold token
        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(booking.getListing().getId(),
                booking.getStartAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                booking.getEndAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1));

        for (AvailabilityCalendar cal : calendars) {
            if (!"HOLD".equals(cal.getStatus()) || !holdToken.equals(cal.getHoldToken())) {
                throw new RuntimeException("Invalid hold token");
            }
        }

        // Set booking status
        if (booking.getListing().getInstantBook()) {
            booking.setStatus("INSTANT_CONFIRMED");
        } else {
            booking.setStatus("PENDING_HOST");
        }

        booking.setHoldToken(holdToken);
        booking.setCreatedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());

        Bookings savedBooking = bookingsRepository.save(booking);

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", savedBooking.getId(),
            "BOOKING_CREATED", "{\"bookingId\": " + savedBooking.getId() + "}"));

        return savedBooking;
    }

    // 4. Authorize Payment
    @Transactional
    public Payments authorizePayment(Bookings booking, String provider, String providerRef) {
        Payments payment = new Payments(booking, "AUTH", booking.getPriceSnapshot() != null ? 10000 : 0, provider); // Placeholder amount
        payment.setProviderRef(providerRef);
        payment.setStatus("SUCCEEDED");
        payment.setCreatedAt(Instant.now());

        Payments savedPayment = paymentsRepository.save(payment);

        // Update booking status
        if ("INSTANT_CONFIRMED".equals(booking.getStatus()) || "PAYMENT_AUTHORIZED".equals(booking.getStatus())) {
            booking.setStatus("PAYMENT_AUTHORIZED");
            bookingsRepository.save(booking);
        }

        return savedPayment;
    }

    // 5. Confirm Booking (Host action)
    @Transactional
    public void confirmBooking(Long bookingId) {
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"PENDING_HOST".equals(booking.getStatus())) {
            throw new RuntimeException("Invalid status for confirmation");
        }

        booking.setStatus("PAYMENT_AUTHORIZED");
        bookingsRepository.save(booking);

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "BOOKING_CONFIRMED", "{\"bookingId\": " + bookingId + "}"));
    }

    // 6. Start Trip (Check-in)
    @Transactional
    public void startTrip(Long bookingId) {
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"PAYMENT_AUTHORIZED".equals(booking.getStatus())) {
            throw new RuntimeException("Invalid status for check-in");
        }

        booking.setStatus("IN_PROGRESS");
        bookingsRepository.save(booking);
    }

    // 7. Complete Trip (Check-out)
    @Transactional
    public void completeTrip(Long bookingId) {
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"IN_PROGRESS".equals(booking.getStatus())) {
            throw new RuntimeException("Invalid status for check-out");
        }

        booking.setStatus("COMPLETED");
        bookingsRepository.save(booking);

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "BOOKING_COMPLETED", "{\"bookingId\": " + bookingId + "}"));
    }
}
