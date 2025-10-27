package com.group1.car_rental.service;

import com.group1.car_rental.entity.*;
import com.group1.car_rental.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

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

    @Autowired
    private CarListingsRepository carListingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PayoutsRepository payoutsRepository;

    // Validation methods
    private void validateListingEligibility(Long listingId) {
        CarListings listing = carListingsRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (!"ACTIVE".equals(listing.getStatus())) {
            throw new RuntimeException("Listing is not active");
        }

        if (listing.getPrice24hCents() == null || listing.getPrice24hCents() <= 0) {
            throw new RuntimeException("Listing must have a valid daily price");
        }

        if (listing.getHomeLocation() == null) {
            throw new RuntimeException("Listing must have a valid home location");
        }

        if (listing.getHomeCity() == null) {
            throw new RuntimeException("Listing must have a valid home city");
        }
    }

    private void validateUserEligibility(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Only customers can place bookings");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is not active");
        }

        UserProfile profile = user.getProfile();
        if (profile == null || !"VERIFIED".equals(profile.getKycStatus())) {
            throw new RuntimeException("KYC verification required to place bookings");
        }
    }

    private void validateHostEligibility(CarListings listing) {
        User owner = listing.getVehicle().getOwner();
        if (!"HOST".equals(owner.getRole())) {
            throw new RuntimeException("Listing owner must be a host");
        }

        if (!owner.getIsActive()) {
            throw new RuntimeException("Host account is not active");
        }
    }

    // 1. Check Availability
    public boolean checkAvailability(Long listingId, LocalDate startDate, LocalDate endDate) {
        // Validate listing eligibility first
        validateListingEligibility(listingId);

        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(listingId, startDate, endDate);

        return calendars.stream().noneMatch(cal ->
            "HOLD".equals(cal.getStatus()) ||
            "BOOKED".equals(cal.getStatus()) ||
            "BLOCKED".equals(cal.getStatus()));
    }

    // 2. Hold Slot
    @Transactional
    public UUID holdSlot(Long listingId, LocalDate startDate, LocalDate endDate, UUID idempotencyKey, Long userId) {
        logger.info("Hold placed: token={} listing={} by user={} from {} to {}",
            idempotencyKey, listingId, userId, startDate, endDate);

        // Validate user eligibility first
        validateUserEligibility(userId);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate hold request detected: token={} user={}", idempotencyKey, userId);
            throw new RuntimeException("Duplicate request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        // Check availability (includes listing validation)
        if (!checkAvailability(listingId, startDate, endDate)) {
            logger.warn("Hold failed - not available: listing={} user={} from {} to {}",
                listingId, userId, startDate, endDate);
            throw new RuntimeException("Not available");
        }

        // Generate hold token
        UUID holdToken = UUID.randomUUID();

        // Set HOLD status
        LocalDate current = startDate;
        int daysHeld = 0;
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
            daysHeld++;
        }

        logger.info("Hold successful: token={} listing={} user={} days={} expires={}",
            holdToken, listingId, userId, daysHeld, LocalDateTime.now().plusMinutes(15));

        return holdToken;
    }

    // 3. Create Booking
    @Transactional
    public Bookings createBooking(Bookings booking, UUID holdToken, UUID idempotencyKey) {
        long days = java.time.Duration.between(booking.getStartAt(), booking.getEndAt()).toDays();
        int totalAmount = calculateBookingTotal(booking);

        logger.info("Booking {} created for listing {} (user {}) status={} price={} days={}",
            booking.getId(), booking.getListing().getId(), booking.getGuest().getId(),
            booking.getListing().getInstantBook() ? "INSTANT_CONFIRMED" : "PENDING_HOST",
            totalAmount, days);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate booking creation request: booking={} token={}",
                booking.getId(), idempotencyKey);
            throw new RuntimeException("Duplicate booking creation request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        // Validate user eligibility
        validateUserEligibility(booking.getGuest().getId());

        // Validate host eligibility
        validateHostEligibility(booking.getListing());

        // Validate hold token
        Bookings existingHold = bookingsRepository.findByHoldToken(holdToken);
        if (existingHold != null) {
            logger.error("Hold token already used: token={} existingBooking={}", holdToken, existingHold.getId());
            throw new RuntimeException("Hold token already used");
        }

        // Validate availability with hold token
        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(booking.getListing().getId(),
                booking.getStartAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                booking.getEndAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1));

        for (AvailabilityCalendar cal : calendars) {
            if (!"HOLD".equals(cal.getStatus()) || !holdToken.equals(cal.getHoldToken())) {
                logger.error("Invalid hold token validation: expected={} actual={} status={}",
                    holdToken, cal.getHoldToken(), cal.getStatus());
                throw new RuntimeException("Invalid hold token");
            }
        }

        // Set booking status
        if (booking.getListing().getInstantBook()) {
            booking.setStatus("INSTANT_CONFIRMED");
            logger.info("Booking {} auto-confirmed (instant book)", booking.getId());
        } else {
            booking.setStatus("PENDING_HOST");
            logger.info("Booking {} pending host confirmation", booking.getId());
        }

        booking.setHoldToken(holdToken);
        booking.setCreatedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());

        Bookings savedBooking = bookingsRepository.save(booking);

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", savedBooking.getId(),
            "BOOKING_CREATED", "{\"bookingId\": " + savedBooking.getId() + ", \"totalAmount\": " + totalAmount + ", \"days\": " + days + "}"));

        logger.info("Booking {} successfully created and event published", savedBooking.getId());

        return savedBooking;
    }

    // 4. Authorize Payment
    @Transactional
    public Payments authorizePayment(Bookings booking, String provider, String providerRef, UUID idempotencyKey) {
        int totalAmount = calculateBookingTotal(booking);

        logger.info("Payment AUTH succeeded for booking {}: amount={} provider={} ref={}",
            booking.getId(), totalAmount, provider, providerRef);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate payment authorization request: booking={} token={}",
                booking.getId(), idempotencyKey);
            throw new RuntimeException("Duplicate payment authorization request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        // Check for existing successful AUTH
        List<Payments> existingAuths = paymentsRepository.findByBookingIdAndTypeAndStatus(booking.getId(), "AUTH", "SUCCEEDED");
        if (!existingAuths.isEmpty()) {
            logger.warn("Booking {} already has successful authorization", booking.getId());
            throw new RuntimeException("Booking already has a successful authorization");
        }

        // Calculate total amount from price snapshot or estimate
        // int totalAmount = calculateBookingTotal(booking); // Moved up for logging

        Payments payment = new Payments(booking, "AUTH", totalAmount, provider);
        payment.setProviderRef(providerRef);
        payment.setStatus("SUCCEEDED"); // Assume success for demo
        payment.setCreatedAt(Instant.now());

        Payments savedPayment = paymentsRepository.save(payment);

        // Create charges breakdown after successful AUTH
        createChargesBreakdown(booking, totalAmount);

        // Update booking status
        if ("INSTANT_CONFIRMED".equals(booking.getStatus()) || "PENDING_HOST".equals(booking.getStatus())) {
            booking.setStatus("PAYMENT_AUTHORIZED");
            bookingsRepository.save(booking);
            logger.info("Booking {} status updated to PAYMENT_AUTHORIZED", booking.getId());
        }

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Payment", savedPayment.getId(),
            "PAYMENT_AUTHORIZED", "{\"bookingId\": " + booking.getId() + ", \"amount\": " + totalAmount + ", \"provider\": \"" + provider + "\"}"));

        return savedPayment;
    }

    // Helper method to calculate booking total
    private int calculateBookingTotal(Bookings booking) {
        // Calculate based on dates and listing price
        long days = java.time.Duration.between(booking.getStartAt(), booking.getEndAt()).toDays();
        int baseAmount = (int) (days * booking.getListing().getPrice24hCents());

        // Add addon costs if any
        // For now, return base amount
        return baseAmount;
    }

    // Create charges breakdown
    private void createChargesBreakdown(Bookings booking, int totalAmount) {
        long days = java.time.Duration.between(booking.getStartAt(), booking.getEndAt()).toDays();
        int baseAmount = (int) (days * booking.getListing().getPrice24hCents());

        // BASE charge
        Charges baseCharge = new Charges(booking, "BASE", baseAmount);
        baseCharge.setCurrency("VND");
        baseCharge.setNote("Base rental for " + days + " days");
        chargesRepository.save(baseCharge);

        // PLATFORM_FEE (10% of base)
        int platformFee = (int) (baseAmount * 0.1);
        Charges platformCharge = new Charges(booking, "PLATFORM_FEE", platformFee);
        platformCharge.setCurrency("VND");
        platformCharge.setNote("Platform fee");
        chargesRepository.save(platformCharge);

        // TAX (0 for demo)
        Charges taxCharge = new Charges(booking, "TAX", 0);
        taxCharge.setCurrency("VND");
        taxCharge.setNote("VAT");
        chargesRepository.save(taxCharge);

        // EXTRA charges (addons) - placeholder
        Charges extraCharge = new Charges(booking, "EXTRA", 0);
        extraCharge.setCurrency("VND");
        extraCharge.setNote("Additional services");
        chargesRepository.save(extraCharge);
    }

    // 5. Confirm Booking (Host action)
    @Transactional
    public void confirmBooking(Long bookingId, UUID idempotencyKey) {
        logger.info("Booking {} confirmed by host", bookingId);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate booking confirmation request: booking={} token={}", bookingId, idempotencyKey);
            throw new RuntimeException("Duplicate booking confirmation request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"PENDING_HOST".equals(booking.getStatus())) {
            logger.warn("Invalid status for confirmation: booking={} status={}", bookingId, booking.getStatus());
            throw new RuntimeException("Invalid status for confirmation");
        }

        // Convert HOLD to BOOKED in calendar
        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(booking.getListing().getId(),
                booking.getStartAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                booking.getEndAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1));

        for (AvailabilityCalendar cal : calendars) {
            if ("HOLD".equals(cal.getStatus()) && booking.getHoldToken().equals(cal.getHoldToken())) {
                cal.setStatus("BOOKED");
                cal.setHoldToken(null);
                cal.setHoldExpireAt(null);
                availabilityCalendarRepository.save(cal);
            }
        }

        // Status remains PENDING_HOST until payment is authorized
        logger.info("Booking {} confirmed by host (user {}), awaiting payment",
            bookingId, booking.getListing().getVehicle().getOwner().getId());

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "BOOKING_CONFIRMED", "{\"bookingId\": " + bookingId + ", \"confirmedBy\": \"HOST\"}"));
    }

    // 6. Reject Booking (Host action)
    @Transactional
    public void rejectBooking(Long bookingId, UUID idempotencyKey) {
        logger.info("Booking {} rejected by host", bookingId);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate booking rejection request: booking={} token={}", bookingId, idempotencyKey);
            throw new RuntimeException("Duplicate booking rejection request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"PENDING_HOST".equals(booking.getStatus())) {
            logger.warn("Invalid status for rejection: booking={} status={}", bookingId, booking.getStatus());
            throw new RuntimeException("Invalid status for rejection");
        }

        // Convert HOLD back to FREE in calendar
        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(booking.getListing().getId(),
                booking.getStartAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                booking.getEndAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1));

        for (AvailabilityCalendar cal : calendars) {
            if ("HOLD".equals(cal.getStatus()) && booking.getHoldToken().equals(cal.getHoldToken())) {
                cal.setStatus("FREE");
                cal.setHoldToken(null);
                cal.setHoldExpireAt(null);
                availabilityCalendarRepository.save(cal);
            }
        }

        booking.setStatus("CANCELLED_HOST");
        booking.setUpdatedAt(Instant.now());
        bookingsRepository.save(booking);

        logger.info("Booking {} cancelled by host (user {})",
            bookingId, booking.getListing().getVehicle().getOwner().getId());

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "BOOKING_REJECTED", "{\"bookingId\": " + bookingId + ", \"rejectedBy\": \"HOST\"}"));
    }

    // 6. Start Trip (Check-in)
    @Transactional
    public void startTrip(Long bookingId, UUID idempotencyKey) {
        logger.info("Trip started for booking {}", bookingId);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate check-in request: booking={} token={}", bookingId, idempotencyKey);
            throw new RuntimeException("Duplicate check-in request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"PAYMENT_AUTHORIZED".equals(booking.getStatus())) {
            logger.warn("Invalid status for check-in: booking={} status={}", bookingId, booking.getStatus());
            throw new RuntimeException("Invalid status for check-in");
        }

        booking.setStatus("IN_PROGRESS");
        booking.setUpdatedAt(Instant.now());
        bookingsRepository.save(booking);

        logger.info("Booking {} status updated to IN_PROGRESS", bookingId);

        // Publish outbox event
        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "TRIP_STARTED", "{\"bookingId\": " + bookingId + ", \"startedAt\": \"" + Instant.now() + "\"}"));
    }

    // 7. Complete Trip (Check-out)
    @Transactional
    public void completeTrip(Long bookingId, UUID idempotencyKey) {
        logger.info("Trip completed for booking {}", bookingId);

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate check-out request: booking={} token={}", bookingId, idempotencyKey);
            throw new RuntimeException("Duplicate check-out request");
        }

        // Save idempotency key
        idempotencyKeysRepository.save(new IdempotencyKeys(idempotencyKey));

        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"IN_PROGRESS".equals(booking.getStatus())) {
            logger.warn("Invalid status for check-out: booking={} status={}", bookingId, booking.getStatus());
            throw new RuntimeException("Invalid status for check-out");
        }

        // Create CAPTURE payment
        List<Payments> authPayments = paymentsRepository.findByBookingIdAndTypeAndStatus(booking.getId(), "AUTH", "SUCCEEDED");
        if (authPayments.isEmpty()) {
            logger.error("No successful authorization found for capture: booking={}", bookingId);
            throw new RuntimeException("No successful authorization found for capture");
        }

        // Check for existing CAPTURE
        List<Payments> capturePayments = paymentsRepository.findByBookingIdAndTypeAndStatus(booking.getId(), "CAPTURE", "SUCCEEDED");
        if (!capturePayments.isEmpty()) {
            logger.warn("Booking {} already has successful capture", bookingId);
            throw new RuntimeException("Booking already has a successful capture");
        }

        Payments authPayment = authPayments.get(0);
        int captureAmount = authPayment.getAmountCents();

        Payments capturePayment = new Payments(booking, "CAPTURE", captureAmount, authPayment.getProvider());
        capturePayment.setCurrency("VND");
        capturePayment.setProviderRef("cap_" + authPayment.getProviderRef()); // Mock capture ref
        capturePayment.setStatus("SUCCEEDED");
        capturePayment.setCreatedAt(Instant.now());
        paymentsRepository.save(capturePayment);

        logger.info("Payment CAPTURE succeeded for booking {}: amount={}", bookingId, captureAmount);

        // Create payout for host
        createPayoutForHost(booking);

        booking.setStatus("COMPLETED");
        booking.setUpdatedAt(Instant.now());
        bookingsRepository.save(booking);

        logger.info("Booking {} status updated to COMPLETED", bookingId);

        // Publish outbox events
        outboxEventsRepository.save(new OutboxEvents("Payment", capturePayment.getId(),
            "PAYMENT_CAPTURED", "{\"bookingId\": " + bookingId + ", \"amount\": " + captureAmount + "}"));

        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "BOOKING_COMPLETED", "{\"bookingId\": " + bookingId + ", \"completedAt\": \"" + Instant.now() + "\"}"));
    }

    // Create payout for host after successful capture
    private void createPayoutForHost(Bookings booking) {
        // Calculate payout amount: base + extra - platform_fee - tax
        List<Charges> charges = chargesRepository.findByBookingId(booking.getId());

        int baseAmount = 0;
        int extraAmount = 0;
        int platformFee = 0;
        int taxAmount = 0;

        for (Charges charge : charges) {
            switch (charge.getLineType()) {
                case "BASE":
                    baseAmount = charge.getAmountCents();
                    break;
                case "EXTRA":
                    extraAmount = charge.getAmountCents();
                    break;
                case "PLATFORM_FEE":
                    platformFee = charge.getAmountCents();
                    break;
                case "TAX":
                    taxAmount = charge.getAmountCents();
                    break;
            }
        }

        int payoutAmount = baseAmount + extraAmount - platformFee - taxAmount;

        if (payoutAmount > 0) {
            Payouts payout = new Payouts(booking.getListing().getVehicle().getOwner(), booking, payoutAmount);
            payout.setCurrency("VND");
            payout.setStatus("PENDING");
            payout.setCreatedAt(Instant.now());

            payoutsRepository.save(payout);
        }
    }

    // 8. Cancel Booking
    @Transactional
    public void cancelBooking(Long bookingId, UUID idempotencyKey, boolean isHostCancellation) {
        logger.info("Booking {} cancellation requested by {}", bookingId, isHostCancellation ? "HOST" : "GUEST");

        // Check idempotency
        if (idempotencyKeysRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            logger.warn("Duplicate cancellation request: booking={} token={}", bookingId, idempotencyKey);
            throw new RuntimeException("Duplicate cancellation request");
        }

        // Save idempotency key
        IdempotencyKeys key = new IdempotencyKeys(idempotencyKey);
        idempotencyKeysRepository.save(key);

        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check if already cancelled
        if (booking.getStatus().startsWith("CANCELLED")) {
            logger.info("Booking {} already cancelled, returning idempotently", bookingId);
            return; // Idempotent - already cancelled
        }

        // Validate cancellation rules
        if ("COMPLETED".equals(booking.getStatus())) {
            logger.warn("Cannot cancel completed booking: booking={}", bookingId);
            throw new RuntimeException("Cannot cancel completed booking");
        }

        if ("IN_PROGRESS".equals(booking.getStatus())) {
            logger.warn("Cannot cancel booking in progress: booking={}", bookingId);
            throw new RuntimeException("Cannot cancel booking in progress");
        }

        // Handle financial operations
        int refundAmount = handleCancellationRefund(booking, isHostCancellation);

        // Release calendar dates
        releaseCalendarDates(booking);

        // Cancel any pending payouts
        cancelPendingPayouts(booking);

        // Update booking status
        String cancelStatus = isHostCancellation ? "CANCELLED_HOST" : "CANCELLED_GUEST";
        booking.setStatus(cancelStatus);
        booking.setUpdatedAt(Instant.now());
        bookingsRepository.save(booking);

        logger.info("Booking {} cancelled by {} with refund amount {}",
            bookingId, isHostCancellation ? "HOST" : "GUEST", refundAmount);

        // Publish cancellation event
        outboxEventsRepository.save(new OutboxEvents("Booking", bookingId,
            "BOOKING_CANCELLED", "{\"bookingId\": " + bookingId + ", \"cancelledBy\": \"" +
            (isHostCancellation ? "HOST" : "GUEST") + "\", \"refundAmount\": " + refundAmount + "}"));
    }

    // Handle refund logic based on cancellation policy
    private int handleCancellationRefund(Bookings booking, boolean isHostCancellation) {
        List<Payments> authPayments = paymentsRepository.findByBookingIdAndTypeAndStatus(booking.getId(), "AUTH", "SUCCEEDED");
        List<Payments> capturePayments = paymentsRepository.findByBookingIdAndTypeAndStatus(booking.getId(), "CAPTURE", "SUCCEEDED");

        if (!authPayments.isEmpty()) {
            Payments authPayment = authPayments.get(0);

            if (capturePayments.isEmpty()) {
                // Only AUTH exists - VOID the authorization
                Payments voidPayment = new Payments(booking, "VOID", authPayment.getAmountCents(), authPayment.getProvider());
                voidPayment.setProviderRef("void_" + authPayment.getProviderRef());
                voidPayment.setStatus("SUCCEEDED");
                voidPayment.setCreatedAt(Instant.now());
                paymentsRepository.save(voidPayment);
                return authPayment.getAmountCents(); // Full amount voided
            } else {
                // CAPTURE exists - calculate refund based on policy
                int refundAmount = calculateRefundAmount(booking, isHostCancellation);
                if (refundAmount > 0) {
                    Payments refundPayment = new Payments(booking, "REFUND", refundAmount, authPayment.getProvider());
                    refundPayment.setProviderRef("refund_" + authPayment.getProviderRef());
                    refundPayment.setStatus("SUCCEEDED");
                    refundPayment.setCreatedAt(Instant.now());
                    paymentsRepository.save(refundPayment);
                }
                return refundAmount;
            }
        }
        return 0; // No payments to refund
    }

    // Calculate refund amount based on policy and timing
    private int calculateRefundAmount(Bookings booking, boolean isHostCancellation) {
        if (isHostCancellation) {
            // Host cancellation - always full refund
            return calculateBookingTotal(booking);
        }

        // Guest cancellation - check policy and timing
        String policy = booking.getListing().getCancellationPolicy();
        long hoursUntilStart = java.time.Duration.between(Instant.now(), booking.getStartAt()).toHours();

        switch (policy) {
            case "STRICT":
                return 0; // No refund
            case "MODERATE":
                if (hoursUntilStart > 24) {
                    return calculateBookingTotal(booking); // Full refund
                } else {
                    return calculateBookingTotal(booking) / 2; // 50% refund
                }
            case "FLEXIBLE":
            default:
                return calculateBookingTotal(booking); // Full refund
        }
    }

    // Release calendar dates back to FREE
    private void releaseCalendarDates(Bookings booking) {
        List<AvailabilityCalendar> calendars = availabilityCalendarRepository
            .findByListingIdAndDateRange(booking.getListing().getId(),
                booking.getStartAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                booking.getEndAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1));

        for (AvailabilityCalendar cal : calendars) {
            cal.setStatus("FREE");
            cal.setHoldToken(null);
            cal.setHoldExpireAt(null);
            availabilityCalendarRepository.save(cal);
        }
    }

    // Cancel any pending payouts
    private void cancelPendingPayouts(Bookings booking) {
        // Find and cancel any pending payouts for this booking
        List<Payouts> pendingPayouts = payoutsRepository.findByBookingIdAndStatus(booking.getId(), "PENDING");
        for (Payouts payout : pendingPayouts) {
            payout.setStatus("CANCELLED");
            payoutsRepository.save(payout);
        }
    }
}
