package com.group1.car_rental.controller;

import com.group1.car_rental.entity.*;
import com.group1.car_rental.repository.AvailabilityCalendarRepository;
import com.group1.car_rental.repository.BookingsRepository;
import com.group1.car_rental.repository.CarListingsRepository;
import com.group1.car_rental.repository.UserRepository;
import com.group1.car_rental.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.List;
import java.util.UUID;
@Controller
@RequestMapping("/bookings")
public class BookingWebController {

    private static final Logger logger = LoggerFactory.getLogger(BookingWebController.class);

    @Autowired
    private CarListingsRepository carListingsRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvailabilityCalendarRepository availabilityCalendarRepository;

    // Helper method to get current authenticated user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Show booking creation form
    @GetMapping("/create")
    public String showBookingForm(@RequestParam(required = false) Long listingId,
                                  @RequestParam(required = false) LocalDate pickupDate,
                                  @RequestParam(required = false) LocalDate returnDate,
                                  @RequestParam(required = false) String pickupTime,
                                  @RequestParam(required = false) String returnTime,
                                  Model model) {

        // If listingId is not provided, redirect to search page
        if (listingId == null) {
            return "redirect:/search";
        }

        CarListings listing = carListingsRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        model.addAttribute("listing", listing);
        model.addAttribute("pickupDate", pickupDate);
        model.addAttribute("returnDate", returnDate);
        model.addAttribute("pickupTime", pickupTime);
        model.addAttribute("returnTime", returnTime);

        return "booking/create";
    }

    // Process booking creation - only hold slot, redirect to payment
    @PostMapping("/create")
    public String createBooking(@RequestParam Long listingId,
                                @RequestParam LocalDate pickupDate,
                                @RequestParam LocalDate returnDate,
                                @RequestParam String pickupTime,
                                @RequestParam String returnTime,
                                RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        logger.info("=== BOOKING HOLD START ===");
        logger.info("User: {} (ID: {}), Listing: {}, Dates: {} {} to {} {}",
                currentUser.getEmail(), currentUser.getId(), listingId,
                pickupDate, pickupTime, returnDate, returnTime);

        try {
            // Generate idempotency key for hold operation
            UUID holdIdempotencyKey = UUID.randomUUID();
            logger.info("Generated hold key: {}", holdIdempotencyKey);

            // Hold slot only - no booking creation yet
            logger.info("Calling holdSlot...");
            UUID holdToken = bookingService.holdSlot(listingId, pickupDate, returnDate, holdIdempotencyKey, currentUser.getId());
            logger.info("Hold successful, token: {}", holdToken);

            // Calculate price for payment page
            CarListings listing = carListingsRepository.findById(listingId)
                    .orElseThrow(() -> new RuntimeException("Listing not found"));
            long days = java.time.temporal.ChronoUnit.DAYS.between(pickupDate, returnDate) + 1;
            int totalAmount = (int) (days * listing.getPrice24hCents());

            // Store hold info in redirect attributes for payment page
            redirectAttributes.addFlashAttribute("holdToken", holdToken.toString());
            redirectAttributes.addFlashAttribute("listingId", listingId);
            redirectAttributes.addFlashAttribute("pickupDate", pickupDate.toString());
            redirectAttributes.addFlashAttribute("returnDate", returnDate.toString());
            redirectAttributes.addFlashAttribute("pickupTime", pickupTime);
            redirectAttributes.addFlashAttribute("returnTime", returnTime);
            redirectAttributes.addFlashAttribute("totalAmount", totalAmount);
            redirectAttributes.addFlashAttribute("days", days);

            redirectAttributes.addFlashAttribute("success", "Đặt chỗ thành công! Vui lòng thanh toán để hoàn tất.");
            return "redirect:/bookings/payment/" + holdToken;

        } catch (Exception e) {
            logger.error("Booking hold failed", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/bookings/create?listingId=" + listingId;
        }
    }

    // Show payment page for hold token
    @GetMapping("/payment/{holdToken}")
    public String showPaymentPage(@PathVariable String holdToken,
                                  @RequestParam(required = false) Long listingId,
                                  @RequestParam(required = false) String pickupDate,
                                  @RequestParam(required = false) String returnDate,
                                  @RequestParam(required = false) String pickupTime,
                                  @RequestParam(required = false) String returnTime,
                                  @RequestParam(required = false) Integer totalAmount,
                                  @RequestParam(required = false) Long days,
                                  Model model) {

        String pickupDateStr = null;
        String returnDateStr = null;

        try {
            UUID holdTokenUUID = UUID.fromString(holdToken);

            // If we have flash attributes, use them (from redirect after hold)
            if (listingId != null && pickupDate != null && returnDate != null) {
                logger.info("Using flash attributes for payment page");
                pickupDateStr = pickupDate;
                returnDateStr = returnDate;
            } else {
                // Otherwise, reconstruct from hold token in database
                logger.info("Reconstructing payment data from hold token: {}", holdToken);

                // Find availability records with this hold token
                List<AvailabilityCalendar> holdRecords = availabilityCalendarRepository
                        .findByHoldToken(holdTokenUUID);

                if (holdRecords.isEmpty()) {
                    logger.error("No hold records found for token: {}", holdToken);
                    model.addAttribute("error", "Không tìm thấy thông tin đặt chỗ. Hold token có thể đã hết hạn.");
                    return "booking/payment-hold";
                }

                // Get listing ID from first record
                Long reconstructedListingId = holdRecords.get(0).getId().getListingId();
                CarListings listing = carListingsRepository.findById(reconstructedListingId)
                        .orElseThrow(() -> new RuntimeException("Listing not found"));

                // Get date range from records
                LocalDate minDate = holdRecords.stream()
                        .map(cal -> cal.getId().getDay())
                        .min(LocalDate::compareTo)
                        .orElse(null);

                LocalDate maxDate = holdRecords.stream()
                        .map(cal -> cal.getId().getDay())
                        .max(LocalDate::compareTo)
                        .orElse(null);

                if (minDate == null || maxDate == null) {
                    logger.error("Could not determine date range for hold token: {}", holdToken);
                    model.addAttribute("error", "Không thể xác định thời gian đặt chỗ.");
                    return "booking/payment-hold";
                }

                // Calculate days and amount
                long calculatedDays = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate) + 1;
                int calculatedAmount = (int) (calculatedDays * listing.getPrice24hCents());

                // Update variables
                listingId = reconstructedListingId;
                pickupDateStr = minDate.toString();
                returnDateStr = maxDate.toString();
                pickupTime = "09:00"; // Default
                returnTime = "18:00"; // Default
                totalAmount = calculatedAmount;
                days = calculatedDays;

                logger.info("Reconstructed payment data: listing={}, dates={} to {}, amount={}",
                        listingId, pickupDateStr, returnDateStr, totalAmount);
            }

            // Add all data to model
            model.addAttribute("holdToken", holdToken);
            model.addAttribute("listingId", listingId);
            model.addAttribute("pickupDate", pickupDateStr);
            model.addAttribute("returnDate", returnDateStr);
            model.addAttribute("pickupTime", pickupTime != null ? pickupTime : "09:00");
            model.addAttribute("returnTime", returnTime != null ? returnTime : "18:00");
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("days", days);

            // Generate booking reference
            model.addAttribute("bookingRef", "BB" + System.currentTimeMillis() % 10000);

            // Get listing details for display
            if (listingId != null) {
                CarListings listing = carListingsRepository.findById(listingId).orElse(null);
                if (listing != null) {
                    model.addAttribute("listing", listing);
                    model.addAttribute("vehicleName", listing.getVehicle().getMake() + " " + listing.getVehicle().getModel());
                    model.addAttribute("location", listing.getHomeCity());
                }
            }

        } catch (Exception e) {
            logger.error("Error loading payment page for hold token: {}", holdToken, e);
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang thanh toán: " + e.getMessage());
        }

        return "booking/payment-hold";
    }

    // Process payment and create booking
    @PostMapping("/payment/{holdToken}")
    public String processPayment(@PathVariable String holdToken,
                                 @RequestParam(required = false) Long listingId,
                                 @RequestParam(required = false) LocalDate pickupDate,
                                 @RequestParam(required = false) LocalDate returnDate,
                                 @RequestParam(required = false) String pickupTime,
                                 @RequestParam(required = false) String returnTime,
                                 @RequestParam String paymentMethod,
                                 RedirectAttributes redirectAttributes) {

        User currentUser = getCurrentUser();
        logger.info("=== PAYMENT PROCESSING START ===");
        logger.info("User: {} (ID: {}), HoldToken: {}", currentUser.getEmail(), currentUser.getId(), holdToken);

        try {
            // Parse hold token
            UUID holdTokenUUID = UUID.fromString(holdToken);

            // Use provided parameters or defaults
            String finalPickupTime = pickupTime != null ? pickupTime : "09:00";
            String finalReturnTime = returnTime != null ? returnTime : "18:00";

            // If parameters not provided, we need to reconstruct them from the hold
            if (listingId == null || pickupDate == null || returnDate == null) {
                logger.info("Reconstructing booking parameters from hold token: {}", holdToken);

                // Find availability records with this hold token
                List<AvailabilityCalendar> holdRecords = availabilityCalendarRepository
                        .findByHoldToken(holdTokenUUID);

                if (holdRecords.isEmpty()) {
                    throw new RuntimeException("No hold records found for token: " + holdToken);
                }

                // Get listing ID from first record
                Long reconstructedListingId = holdRecords.get(0).getId().getListingId();

                // Get date range from records
                LocalDate minDate = holdRecords.stream()
                        .map(cal -> cal.getId().getDay())
                        .min(LocalDate::compareTo)
                        .orElse(null);

                LocalDate maxDate = holdRecords.stream()
                        .map(cal -> cal.getId().getDay())
                        .max(LocalDate::compareTo)
                        .orElse(null);

                if (minDate == null || maxDate == null) {
                    throw new RuntimeException("Could not determine date range for hold token: " + holdToken);
                }

                // Update variables
                listingId = reconstructedListingId;
                pickupDate = minDate;
                returnDate = maxDate;
                finalPickupTime = pickupTime != null ? pickupTime : "09:00";
                finalReturnTime = returnTime != null ? returnTime : "18:00";

                logger.info("Reconstructed parameters: listingId={}, dates={} to {}",
                        listingId, pickupDate, returnDate);
            }

            // Parse times
            LocalTime pickupLocalTime = LocalTime.parse(finalPickupTime);
            LocalTime returnLocalTime = LocalTime.parse(finalReturnTime);

            // Combine dates and times
            LocalDateTime startDateTime = LocalDateTime.of(pickupDate, pickupLocalTime);
            LocalDateTime endDateTime = LocalDateTime.of(returnDate, returnLocalTime);

            // Convert to Instant
            Instant startAt = startDateTime.toInstant(ZoneOffset.UTC);
            Instant endAt = endDateTime.toInstant(ZoneOffset.UTC);

            // Get listing
            CarListings listing = carListingsRepository.findById(listingId)
                    .orElseThrow(() -> new RuntimeException("Listing not found"));

            // Create booking object
            Bookings booking = new Bookings();
            booking.setListing(listing);
            booking.setGuest(currentUser);
            booking.setStartAt(startAt);
            booking.setEndAt(endAt);
            booking.setStatus("PENDING_HOST");

            // Generate idempotency key for booking creation
            UUID bookingIdempotencyKey = UUID.randomUUID();
            logger.info("Generated booking key: {}", bookingIdempotencyKey);

            // Create booking with hold token
            logger.info("Calling createBooking...");
            Bookings createdBooking = bookingService.createBooking(booking, holdTokenUUID, bookingIdempotencyKey);
            logger.info("Booking created successfully, ID: {}", createdBooking.getId());

            // Now authorize payment (always succeeds with mock provider)
            UUID paymentIdempotencyKey = UUID.randomUUID();
            logger.info("Authorizing payment for booking {}...", createdBooking.getId());

            Payments payment = bookingService.authorizePayment(createdBooking, "MOCK", "mock_payment_" + System.currentTimeMillis(), paymentIdempotencyKey);
            logger.info("Payment authorized successfully");

            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Đang chờ chủ xe xác nhận.");
            return "redirect:/bookings/" + createdBooking.getId();

        } catch (Exception e) {
            logger.error("Payment processing failed", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/bookings/payment/" + holdToken;
        }
    }

    // Show host bookings management page - specific mapping first
    @GetMapping("/host-bookings")
    public String showHostBookings() {
        // This will render the host-bookings.html template
        return "booking/host-bookings";
    }

    // Show booking details (web page) - generic mapping last
    @GetMapping("/{bookingId}")
    public String showBookingDetails(@PathVariable Long bookingId, Model model) {
        // This will render the booking-details.html template
        model.addAttribute("bookingId", bookingId);
        return "booking/booking-details";
    }
}
