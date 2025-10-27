package com.group1.car_rental.controller;

import com.group1.car_rental.entity.*;
import com.group1.car_rental.repository.*;
import com.group1.car_rental.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.group1.car_rental.entity.Cars;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private CarsRepository carsRepository;

    @Autowired
    private CarListingsRepository carListingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    // Helper method to get current authenticated user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        // Assuming username is stored as email
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Helper method to validate KYC status
    private void validateKycStatus(User user) {
        UserProfile profile = user.getProfile();
        if (profile == null || !"VERIFIED".equals(profile.getKycStatus())) {
            throw new RuntimeException("KYC verification required");
        }
    }

    // Helper method to check booking authorization
    private void checkBookingAuthorization(Bookings booking, User currentUser, String action) {
        switch (action) {
            case "VIEW":
                // Guest can view their own bookings, host can view bookings for their cars
                if (!booking.getGuest().getId().equals(currentUser.getId()) &&
                    !booking.getListing().getVehicle().getOwner().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Unauthorized to view this booking");
                }
                break;
            case "CONFIRM":
            case "REJECT":
                // Only host can confirm/reject
                if (!"HOST".equals(currentUser.getRole()) ||
                    !booking.getListing().getVehicle().getOwner().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Only the host can " + action.toLowerCase() + " this booking");
                }
                break;
            case "CHECKIN":
            case "CHECKOUT":
                // Guest or host can check-in/check-out
                if (!booking.getGuest().getId().equals(currentUser.getId()) &&
                    !booking.getListing().getVehicle().getOwner().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Unauthorized to " + action.toLowerCase() + " this booking");
                }
                break;
            case "CANCEL_GUEST":
                // Only guest can cancel their own booking
                if (!booking.getGuest().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Only the guest can cancel this booking");
                }
                break;
            default:
                throw new RuntimeException("Unknown action: " + action);
        }
    }

    // 1. Check Availability
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam Long listingId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        boolean available = bookingService.checkAvailability(listingId, startDate, endDate);

        // Get listing details for price and location info
        CarListings listing = carListingsRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Calculate estimated price
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int estimatedPrice = (int) (days * listing.getPrice24hCents());

        // Extract coordinates from homeLocation (assuming SRID 4326 = WGS84 lat/lon)
        Double latitude = null;
        Double longitude = null;
        if (listing.getHomeLocation() != null) {
            // Note: In production, you'd extract lat/lon from the geometry point
            // For demo, we'll return placeholder coordinates
            latitude = 10.762622; // Ho Chi Minh City coordinates as example
            longitude = 106.660172;
        }

        return ResponseEntity.ok(Map.of(
            "available", available,
            "listingId", listingId,
            "startDate", startDate,
            "endDate", endDate,
            "estimatedPrice", estimatedPrice,
            "currency", "VND",
            "dailyRate", listing.getPrice24hCents(),
            "days", days,
            "homeLocation", Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "city", listing.getHomeCity()
            )
        ));
    }

    // 2. Hold Slot
    @PostMapping("/hold")
    public ResponseEntity<Map<String, Object>> holdSlot(
            @RequestParam Long listingId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        User currentUser = getCurrentUser();
        validateKycStatus(currentUser);

        // Only customers can create holds
        if (!"CUSTOMER".equals(currentUser.getRole())) {
            throw new RuntimeException("Only customers can create booking holds");
        }

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        UUID holdToken = bookingService.holdSlot(listingId, startDate, endDate, idempotencyKey, currentUser.getId());

        return ResponseEntity.ok(Map.of(
            "holdToken", holdToken,
            "expiresInMinutes", 15
        ));
    }

    // 3. Create Booking
    @ResponseBody
    @PostMapping(value = "/create", produces = "application/json")
    public ResponseEntity<Bookings> createBooking(
            @RequestBody Bookings booking,
            @RequestParam UUID holdToken,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        User currentUser = getCurrentUser();
        validateKycStatus(currentUser);

        // Only customers can create bookings (or hosts booking other cars)
        if (!"CUSTOMER".equals(currentUser.getRole()) && !"HOST".equals(currentUser.getRole())) {
            throw new RuntimeException("Invalid role for booking creation");
        }

        // Ensure the booking guest is the current user
        if (!booking.getGuest().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot create booking for another user");
        }

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        Bookings createdBooking = bookingService.createBooking(booking, holdToken, idempotencyKey);
        return ResponseEntity.ok(createdBooking);
    }

    // 4. Authorize Payment
    @ResponseBody
    @PostMapping("/{bookingId}/authorize", produces = "application/json")
    public ResponseEntity<Payments> authorizePayment(
            @PathVariable Long bookingId,
            @RequestParam String provider,
            @RequestParam String providerRef,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        // Retrieve the actual booking
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        // Allow guest or host to authorize payment
        checkBookingAuthorization(booking, currentUser, "CHECKIN");

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        Payments payment = bookingService.authorizePayment(booking, provider, providerRef, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    // 5. Confirm Booking (Host)
    @ResponseBody
    @PostMapping("/{bookingId}/confirm", produces = "application/json")
    public ResponseEntity<String> confirmBooking(
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        // Retrieve booking for authorization check
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        checkBookingAuthorization(booking, currentUser, "CONFIRM");

        // Additional security: verify car ownership
        carsRepository.findByIdAndOwner(booking.getListing().getVehicle().getId(), currentUser)
            .orElseThrow(() -> new RuntimeException("You do not own this vehicle"));

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        bookingService.confirmBooking(bookingId, idempotencyKey);
        return ResponseEntity.ok("Booking confirmed");
    }

    // 6. Reject Booking (Host)
    @ResponseBody
    @PostMapping("/{bookingId}/reject", produces = "application/json")
    public ResponseEntity<String> rejectBooking(
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        // Retrieve booking for authorization check
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        checkBookingAuthorization(booking, currentUser, "REJECT");

        // Additional security: verify car ownership
        carsRepository.findByIdAndOwner(booking.getListing().getVehicle().getId(), currentUser)
            .orElseThrow(() -> new RuntimeException("You do not own this vehicle"));

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        bookingService.rejectBooking(bookingId, idempotencyKey);
        return ResponseEntity.ok("Booking rejected");
    }

    // 6. Start Trip (Check-in)
    @ResponseBody
    @PostMapping("/{bookingId}/checkin", produces = "application/json")
    public ResponseEntity<String> checkIn(
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        // Retrieve booking for authorization check
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        checkBookingAuthorization(booking, currentUser, "CHECKIN");

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        bookingService.startTrip(bookingId, idempotencyKey);
        return ResponseEntity.ok("Trip started");
    }

    // 7. Complete Trip (Check-out)
    @ResponseBody
    @PostMapping("/{bookingId}/checkout", produces = "application/json")
    public ResponseEntity<String> checkOut(
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        // Retrieve booking for authorization check
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        checkBookingAuthorization(booking, currentUser, "CHECKOUT");

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        bookingService.completeTrip(bookingId, idempotencyKey);
        return ResponseEntity.ok("Trip completed");
    }

    // 8. Cancel Booking
    @ResponseBody
    @PostMapping("/{bookingId}/cancel", produces = "application/json")
    public ResponseEntity<String> cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr,
            @RequestParam(defaultValue = "false") boolean isHostCancellation) {

        // Retrieve booking for authorization check
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();

        if (isHostCancellation) {
            checkBookingAuthorization(booking, currentUser, "REJECT"); // Host cancel
            // Additional security: verify car ownership
            carsRepository.findByIdAndOwner(booking.getListing().getVehicle().getId(), currentUser)
                .orElseThrow(() -> new RuntimeException("You do not own this vehicle"));
        } else {
            checkBookingAuthorization(booking, currentUser, "CANCEL_GUEST"); // Guest cancel
        }

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);
        bookingService.cancelBooking(bookingId, idempotencyKey, isHostCancellation);
        return ResponseEntity.ok("Booking cancelled");
    }

    // Get My Bookings (Guest)
    @ResponseBody
    @GetMapping("/my-bookings", produces = "application/json")
    public ResponseEntity<List<Bookings>> getMyBookings() {
        User currentUser = getCurrentUser();
        List<Bookings> bookings = bookingsRepository.findByGuestId(currentUser.getId());
        return ResponseEntity.ok(bookings);
    }

    // Get Host Bookings (Host)
    @ResponseBody
    @GetMapping("/host-bookings", produces = "application/json")
    public ResponseEntity<List<Bookings>> getHostBookings() {
        User currentUser = getCurrentUser();
        if (!"HOST".equals(currentUser.getRole())) {
            throw new RuntimeException("Only hosts can access host bookings");
        }
        List<Bookings> bookings = bookingsRepository.findByListingVehicleOwnerId(currentUser.getId());
        return ResponseEntity.ok(bookings);
    }

    // Get Single Booking Details
    @ResponseBody
    @GetMapping("/{bookingId}", produces = "application/json")
    public ResponseEntity<Bookings> getBooking(@PathVariable Long bookingId) {
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        checkBookingAuthorization(booking, currentUser, "VIEW");

        return ResponseEntity.ok(booking);
    }

    // Calculate Price Quote (without holding)
    @ResponseBody
    @GetMapping("/price-quote", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getPriceQuote(
            @RequestParam Long listingId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) List<Long> addonIds) {

        // Get listing details
        CarListings listing = carListingsRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));

        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int baseAmount = (int) (days * listing.getPrice24hCents());

        // Calculate addon costs
        int addonTotal = 0;
        if (addonIds != null && !addonIds.isEmpty()) {
            // In production, you'd fetch addons and calculate costs
            // For demo, we'll use placeholder pricing
            addonTotal = addonIds.size() * 50000; // 50k VND per addon per day
        }

        int subtotal = baseAmount + addonTotal;
        int platformFee = (int) (baseAmount * 0.1); // 10% platform fee
        int tax = 0; // No tax for demo
        int total = subtotal + platformFee + tax;

        return ResponseEntity.ok(Map.of(
            "listingId", listingId,
            "startDate", startDate,
            "endDate", endDate,
            "days", days,
            "breakdown", Map.of(
                "baseAmount", baseAmount,
                "addonTotal", addonTotal,
                "subtotal", subtotal,
                "platformFee", platformFee,
                "tax", tax,
                "total", total
            ),
            "currency", "VND",
            "policy", listing.getCancellationPolicy()
        ));
    }

    @GetMapping("/confirm")
    public String confirmBooking(Model model, @RequestParam Long carId,
                                 @RequestParam String pickupDate,
                                 @RequestParam String returnDate,
                                 @RequestParam Integer days,
                                 @RequestParam Integer rentalPrice,
                                 @RequestParam Integer serviceFee,
                                 @RequestParam Integer tax,
                                 @RequestParam Integer totalPrice) {
        Cars car = carsRepository.findById(carId).orElse(null);
        model.addAttribute("car", car);
        model.addAttribute("pickupDate", pickupDate);
        model.addAttribute("returnDate", returnDate);
        model.addAttribute("days", days);
        model.addAttribute("rentalPrice", rentalPrice);
        model.addAttribute("serviceFee", serviceFee);
        model.addAttribute("tax", tax);
        model.addAttribute("totalPrice", totalPrice);
        return "booking/confirm";
    }
}
