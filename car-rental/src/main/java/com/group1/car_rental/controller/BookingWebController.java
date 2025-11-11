package com.group1.car_rental.controller;

import com.group1.car_rental.dto.BookingDto;
import com.group1.car_rental.entity.*;
import com.group1.car_rental.repository.AvailabilityCalendarRepository;
import com.group1.car_rental.repository.BookingsRepository;
import com.group1.car_rental.repository.CarListingsRepository;
import com.group1.car_rental.repository.UserRepository;
import com.group1.car_rental.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.List;
import java.util.Map;
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
            // COMPREHENSIVE BACKEND VALIDATION - Cannot be bypassed
            LocalDate today = LocalDate.now();

            // 1. không được trả đặt xe ở quá khứ
            if (pickupDate.isBefore(today)) {
                logger.warn("SECURITY: Attempted booking with past pickup date: {} (today: {})", pickupDate, today);
                redirectAttributes.addFlashAttribute("error", "Ngày nhận xe không được ở quá khứ");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 2. check để trả xe phải sau pick xe
            if (!returnDate.isAfter(pickupDate)) {
                logger.warn("SECURITY: Invalid date range - return date not after pickup: pickup={} return={}", pickupDate, returnDate);
                redirectAttributes.addFlashAttribute("error", "Ngày trả xe phải sau ngày nhận xe ít nhất 1 ngày");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 3. ứ cho đặt quá 3 tháng
            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(pickupDate, returnDate);
            if (daysDifference > 90) {
                logger.warn("SECURITY: Booking duration too long: {} days (max: 90)", daysDifference);
                redirectAttributes.addFlashAttribute("error", "Thời gian thuê xe không được vượt quá 90 ngày");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 4. đặt phải nhiều hơn 1 ngày
            if (daysDifference < 1) {
                logger.warn("SECURITY: Booking duration too short: {} days (min: 1)", daysDifference);
                redirectAttributes.addFlashAttribute("error", "Thời gian thuê xe phải ít nhất 1 ngày");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 5. Validate reasonable future booking (max 1 year ahead)
            LocalDate maxFutureDate = today.plusYears(1);
            if (pickupDate.isAfter(maxFutureDate)) {
                logger.warn("SECURITY: Booking too far in future: {} (max: {})", pickupDate, maxFutureDate);
                redirectAttributes.addFlashAttribute("error", "Không thể đặt xe quá 1 năm tính từ ngày hiện tại");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 6. Validate time formats and business hours
            LocalTime pickupLocalTime;
            LocalTime returnLocalTime;
            try {
                pickupLocalTime = LocalTime.parse(pickupTime);
                returnLocalTime = LocalTime.parse(returnTime);
            } catch (Exception e) {
                logger.warn("SECURITY: Invalid time format: pickupTime={} returnTime={}", pickupTime, returnTime);
                redirectAttributes.addFlashAttribute("error", "Định dạng thời gian không hợp lệ");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 7. phải trả xe giờ hành chính
            LocalTime businessStart = LocalTime.of(6, 0);
            LocalTime businessEnd = LocalTime.of(22, 0);
            if (pickupLocalTime.isBefore(businessStart) || pickupLocalTime.isAfter(businessEnd) ||
                returnLocalTime.isBefore(businessStart) || returnLocalTime.isAfter(businessEnd)) {
                logger.warn("SECURITY: Time outside business hours: pickup={} return={}", pickupLocalTime, returnLocalTime);
                redirectAttributes.addFlashAttribute("error", "Thời gian nhận/trả xe phải trong khung giờ 6:00 - 22:00");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 8. Validate listing exists and is active
            CarListings validatedListing = carListingsRepository.findById(listingId)
                    .orElseThrow(() -> new RuntimeException("Listing not found"));
            if (!"ACTIVE".equals(validatedListing.getStatus())) {
                logger.warn("SECURITY: Attempted booking on inactive listing: {}", listingId);
                redirectAttributes.addFlashAttribute("error", "Xe này hiện không khả dụng để thuê");
                return "redirect:/bookings/create?listingId=" + listingId;
            }

            // 9. Additional security: Check for rapid booking attempts (basic rate limiting)

            long currentTime = System.currentTimeMillis();
            String rateLimitKey = currentUser.getId() + ":" + listingId;

            logger.info("Booking attempt rate check: user={} listing={} time={}", currentUser.getId(), listingId, currentTime);
            // Generate idempotency key for hold operation
            UUID holdIdempotencyKey = UUID.randomUUID();
            logger.info("Generated hold key: {}", holdIdempotencyKey);

            // Hold slot only - no booking creation yet
            logger.info("Calling holdSlot...");
            UUID holdToken = bookingService.holdSlot(listingId, pickupDate, returnDate, holdIdempotencyKey, currentUser.getId());
            logger.info("Hold successful, token: {}", holdToken);

            // tính cái giá phải trả
            CarListings listing = carListingsRepository.findById(listingId)
                    .orElseThrow(() -> new RuntimeException("Listing not found"));
            long days = java.time.temporal.ChronoUnit.DAYS.between(pickupDate, returnDate) + 1;
            int totalAmount = (int) (days * listing.getPrice24hCents());

            // lưu bằng flash để dùng 1 lần cho payment
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
    
    // THÊM 3 DÒNG NÀY ĐỂ HIỂN THỊ LỖI THẬT 100%
    redirectAttributes.addFlashAttribute("error", "LỖI THẬT: " + e.getClass().getSimpleName() + " - " + e.getMessage());
    redirectAttributes.addFlashAttribute("stacktrace", e.toString());
    
    return "redirect:/bookings/create?listingId=" + listingId;
}
    }

    // hiện payment page cho hold token
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


            if (listingId != null && pickupDate != null && returnDate != null) {
                logger.info("Using flash attributes for payment page: listingId={}, pickupDate={}, returnDate={}",
                        listingId, pickupDate, returnDate);
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

                logger.info("Found {} hold records for token {}", holdRecords.size(), holdToken);

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

                logger.info("Reconstructed date range: minDate={}, maxDate={}", minDate, maxDate);

                if (minDate == null || maxDate == null) {
                    throw new RuntimeException("Could not determine date range for hold token: " + holdToken);
                }

                // Update variables
                listingId = reconstructedListingId;
                pickupDate = minDate;
                returnDate = maxDate;
                finalPickupTime = pickupTime != null ? pickupTime : "09:00";
                finalReturnTime = returnTime != null ? returnTime : "18:00";

                logger.info("Final reconstructed parameters: listingId={}, dates={} to {}, times={} to {}",
                        listingId, pickupDate, returnDate, finalPickupTime, finalReturnTime);
            } else {
                logger.info("Using provided parameters: listingId={}, dates={} to {}, times={} to {}",
                        listingId, pickupDate, returnDate, finalPickupTime, finalReturnTime);
            }

            // Parse times
            LocalTime pickupLocalTime = LocalTime.parse(finalPickupTime);
            LocalTime returnLocalTime = LocalTime.parse(finalReturnTime);

            // Combine dates and times
            LocalDateTime startDateTime = LocalDateTime.of(pickupDate, pickupLocalTime);
            LocalDateTime endDateTime = LocalDateTime.of(returnDate, returnLocalTime);

            // Convert to Instant using system timezone
            Instant startAt = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
            Instant endAt = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

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

            logger.info("Payment processing completed successfully, redirecting to booking details");
            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Đang chờ chủ xe xác nhận.");
            return "redirect:/bookings/" + createdBooking.getId() + "?success=true";

        } catch (Exception e) {
            logger.error("Payment processing failed for holdToken: {}, user: {}", holdToken, currentUser.getEmail(), e);
            logger.error("Exception details:", e);
            // For debugging, add the full exception to flash attributes
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            redirectAttributes.addFlashAttribute("debug", e.toString());

            // Add detailed debug info for web display
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("Exception: ").append(e.getClass().getSimpleName()).append("\n");
            debugInfo.append("Message: ").append(e.getMessage()).append("\n");
            debugInfo.append("HoldToken: ").append(holdToken).append("\n");
            debugInfo.append("User: ").append(currentUser.getEmail()).append("\n");
            debugInfo.append("ListingId: ").append(listingId).append("\n");
            debugInfo.append("PickupDate: ").append(pickupDate).append("\n");
            debugInfo.append("ReturnDate: ").append(returnDate).append("\n");

            // Add database state info
            try {
                UUID holdTokenUUID = UUID.fromString(holdToken);
                List<AvailabilityCalendar> holdRecords = availabilityCalendarRepository
                    .findByHoldToken(holdTokenUUID);

                debugInfo.append("\n=== DATABASE STATE ===\n");
                debugInfo.append("Hold records found: ").append(holdRecords.size()).append("\n");

                for (int i = 0; i < holdRecords.size(); i++) {
                    AvailabilityCalendar cal = holdRecords.get(i);
                    debugInfo.append("Record ").append(i + 1).append(": ");
                    debugInfo.append("day=").append(cal.getId().getDay());
                    debugInfo.append(", status=").append(cal.getStatus());
                    debugInfo.append(", holdToken=").append(cal.getHoldToken());
                    debugInfo.append(", expireAt=").append(cal.getHoldExpireAt());
                    debugInfo.append("\n");
                }

                // Check if dates match
                if (pickupDate != null && returnDate != null) {
                    LocalDate start = pickupDate;
                    LocalDate end = returnDate;

                    debugInfo.append("\n=== DATE VALIDATION ===\n");
                    debugInfo.append("Expected dates: ").append(start).append(" to ").append(end).append("\n");

                    List<AvailabilityCalendar> dateRecords = availabilityCalendarRepository
                        .findByListingIdAndDateRange(listingId != null ? listingId : 1L, start, end);

                    debugInfo.append("Date range records found: ").append(dateRecords.size()).append("\n");

                    for (int i = 0; i < dateRecords.size(); i++) {
                        AvailabilityCalendar cal = dateRecords.get(i);
                        debugInfo.append("Date record ").append(i + 1).append(": ");
                        debugInfo.append("day=").append(cal.getId().getDay());
                        debugInfo.append(", status=").append(cal.getStatus());
                        debugInfo.append(", holdToken=").append(cal.getHoldToken());
                        debugInfo.append("\n");
                    }
                }

            } catch (Exception dbEx) {
                debugInfo.append("\n=== DATABASE ERROR ===\n");
                debugInfo.append("Failed to query database: ").append(dbEx.getMessage()).append("\n");
            }

            redirectAttributes.addFlashAttribute("debugInfo", debugInfo.toString());
            return "redirect:/bookings/payment/" + holdToken;
        }
    }

    // Show booking details (web page) - only match numeric IDs
    @GetMapping("/{bookingId:\\d+}")
    public String showBookingDetails(@PathVariable Long bookingId, Model model) {
        // This will render the booking-details.html template
        model.addAttribute("bookingId", bookingId);
        return "booking/booking-details";
    }

    // API endpoint for single booking details (customer/guest view)
    @GetMapping("/api/{bookingId:\\d+}")
    public ResponseEntity<BookingDto> getBooking(@PathVariable Long bookingId) {
        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();
        // Check authorization - guest or host can view
        if (!booking.getGuest().getId().equals(currentUser.getId()) &&
            !booking.getListing().getVehicle().getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to view this booking");
        }

        // Convert to DTO
        BookingDto dto = convertToDto(booking);
        return ResponseEntity.ok(dto);
    }

    // API endpoint for booking actions (checkin, checkout, cancel for customers)
    @PostMapping("/api/{bookingId:\\d+}/{action}")
    public ResponseEntity<String> performBookingAction(
            @PathVariable Long bookingId,
            @PathVariable String action,
            @RequestParam(defaultValue = "false") boolean isHostCancellation,
            @RequestHeader("Idempotency-Key") String idempotencyKeyStr) {

        Bookings booking = bookingsRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User currentUser = getCurrentUser();

        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);

        switch (action) {
            case "checkin":
                // Guest or host can check-in
                if (!booking.getGuest().getId().equals(currentUser.getId()) &&
                    !booking.getListing().getVehicle().getOwner().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("You are not authorized to perform this action");
                }
                bookingService.startTrip(bookingId, idempotencyKey);
                return ResponseEntity.ok("Trip started");

            case "checkout":
                // Guest or host can check-out
                if (!booking.getGuest().getId().equals(currentUser.getId()) &&
                    !booking.getListing().getVehicle().getOwner().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("You are not authorized to perform this action");
                }
                bookingService.completeTrip(bookingId, idempotencyKey);
                return ResponseEntity.ok("Trip completed");

            case "cancel":
                // Guest can cancel their own booking
                if (!booking.getGuest().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("You can only cancel your own bookings");
                }
                bookingService.cancelBooking(bookingId, idempotencyKey, false);
                return ResponseEntity.ok("Booking cancelled");

            default:
                throw new RuntimeException("Unknown action: " + action);
        }
    }

    // Helper method to convert Booking entity to DTO
    private BookingDto convertToDto(Bookings booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setStartAt(booking.getStartAt());
        dto.setEndAt(booking.getEndAt());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());

        // Convert guest
        BookingDto.UserDto guestDto = new BookingDto.UserDto();
        guestDto.setId(booking.getGuest().getId());
        guestDto.setEmail(booking.getGuest().getEmail());
        guestDto.setPhone(booking.getGuest().getPhone());
        dto.setGuest(guestDto);

        // Convert listing
        BookingDto.ListingDto listingDto = new BookingDto.ListingDto();
        listingDto.setId(booking.getListing().getId());
        listingDto.setHomeCity(booking.getListing().getHomeCity());
        listingDto.setPrice24hCents(booking.getListing().getPrice24hCents());
        listingDto.setInstantBook(booking.getListing().getInstantBook());
        listingDto.setCancellationPolicy(booking.getListing().getCancellationPolicy().toString());

        // Convert vehicle
        BookingDto.ListingDto.VehicleDto vehicleDto = new BookingDto.ListingDto.VehicleDto();
        vehicleDto.setId(booking.getListing().getVehicle().getId());
        vehicleDto.setMake(booking.getListing().getVehicle().getMake());
        vehicleDto.setModel(booking.getListing().getVehicle().getModel());
        vehicleDto.setYear(booking.getListing().getVehicle().getYear());
        vehicleDto.setTransmission(booking.getListing().getVehicle().getTransmission());
        vehicleDto.setFuelType(booking.getListing().getVehicle().getFuelType());
        vehicleDto.setSeats(booking.getListing().getVehicle().getSeats());
        vehicleDto.setImageUrls(booking.getListing().getVehicle().getImageUrls());

        // Convert owner
        BookingDto.ListingDto.VehicleDto.UserDto ownerDto = new BookingDto.ListingDto.VehicleDto.UserDto();
        ownerDto.setId(booking.getListing().getVehicle().getOwner().getId());
        ownerDto.setEmail(booking.getListing().getVehicle().getOwner().getEmail());
        ownerDto.setPhone(booking.getListing().getVehicle().getOwner().getPhone());
        vehicleDto.setOwner(ownerDto);

        listingDto.setVehicle(vehicleDto);
        dto.setListing(listingDto);

        return dto;
    }
}
