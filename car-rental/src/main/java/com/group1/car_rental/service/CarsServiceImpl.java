package com.group1.car_rental.service;

import com.group1.car_rental.dto.CarListingsDto;
import com.group1.car_rental.dto.CarListingsForm;
import com.group1.car_rental.dto.CarsDto;
import com.group1.car_rental.dto.CarsForm;
import com.group1.car_rental.entity.CarListings;
import com.group1.car_rental.entity.Cars;
import com.group1.car_rental.entity.User;
import com.group1.car_rental.mapper.CarsMapper;
import com.group1.car_rental.repository.CarsRepository;
import com.group1.car_rental.repository.UserRepository;
import com.group1.car_rental.repository.CarListingsRepository;
import lombok.RequiredArgsConstructor;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class CarsServiceImpl implements CarsService {

    private final CarsRepository vehicleRepository;
    private final CarsMapper vehicleMapper;
    private final UserRepository userRepository;
    private final CarListingsRepository carListingsRepository;

    // XÓA HOÀN TOÀN GeometryFactory (không cần nữa)
    // private final GeometryFactory geometryFactory = ...

    @Override
    public List<CarsDto> getAllVehicles() {
        List<Cars> cars = vehicleRepository.findAll();
        return vehicleMapper.toDtoList(cars);
    }

    @Override
    public List<CarsDto> searchByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return getAllVehicles();
        }
        List<Cars> cars = vehicleRepository.searchByLocation(location.trim());
        return vehicleMapper.toDtoList(cars);
    }

    @Override
    public List<CarsDto> searchVehicles(String location, LocalDate startDate, LocalDate endDate, Double maxPrice, String fuelType, Integer seats) {
        List<Cars> cars = vehicleRepository.searchByLocation(location != null ? location.trim() : "");
        return vehicleMapper.toDtoList(cars.stream()
                .filter(car -> maxPrice == null || car.getDailyPrice() <= maxPrice)
                .filter(car -> fuelType == null || car.getFuelType().equalsIgnoreCase(fuelType))
                .filter(car -> seats == null || car.getSeats() >= seats)
                .collect(Collectors.toList()));
    }

    @Override
    public List<CarsDto> getVehiclesByOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Cars> cars = vehicleRepository.findByOwner(owner);
        return vehicleMapper.toDtoList(cars);
    }

    @Override
    public CarsDto getVehicleByIdAndOwner(Long id, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Cars car = vehicleRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Car not found or not owned by user"));
        return vehicleMapper.toDto(car);
    }

    @Override
    public CarsDto getVehicleById(Long id) {
        Cars car = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));
        return vehicleMapper.toDto(car);
    }

    @Override
    @Transactional
    public CarsDto createVehicle(CarsForm form, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Cars car = new Cars();
        car.setOwner(owner);
        car.setMake(form.getMake());
        car.setModel(form.getModel());
        car.setYear(form.getYear());
        car.setTransmission(form.getTransmission());
        car.setFuelType(form.getFuelType());
        car.setSeats(form.getSeats());
        car.setDailyPrice(form.getDailyPrice());
        car.setImageUrls(form.getImageUrls());
        car.setCity(form.getCity());
        car.setVinEncrypted(form.getVinEncrypted());
        car.setPlateMasked(form.getPlateMasked());
        car.setCreatedAt(Instant.now());
        car.setUpdatedAt(Instant.now());
        car.setStatus("ACTIVE");
        Cars savedCar = vehicleRepository.save(car);
        return vehicleMapper.toDto(savedCar);
    }

    @Override
    @Transactional
    public CarsDto updateVehicle(Long id, CarsForm form, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Cars car = vehicleRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Car not found or not owned by user"));
        car.setMake(form.getMake());
        car.setModel(form.getModel());
        car.setYear(form.getYear());
        car.setTransmission(form.getTransmission());
        car.setFuelType(form.getFuelType());
        car.setSeats(form.getSeats());
        car.setDailyPrice(form.getDailyPrice());
        car.setImageUrls(form.getImageUrls());
        car.setCity(form.getCity());
        car.setVinEncrypted(form.getVinEncrypted());
        car.setPlateMasked(form.getPlateMasked());
        car.setUpdatedAt(Instant.now());
        Cars updatedCar = vehicleRepository.save(car);
        return vehicleMapper.toDto(updatedCar);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Cars car = vehicleRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Car not found or not owned by user"));
        vehicleRepository.delete(car);
    }

//     private CarListingsDto mapToCarListingsDto(CarListings listing) {
//     CarListingsDto dto = new CarListingsDto();
//     dto.setId(listing.getId());
//     dto.setVehicleId(listing.getVehicle().getId());
//     dto.setTitle(listing.getTitle());
//     dto.setDescription(listing.getDescription());
//     dto.setPrice24hCents(listing.getPrice24hCents());
//     dto.setKmLimit24h(listing.getKmLimit24h());
//     dto.setInstantBook(listing.getInstantBook());
//     dto.setCancellationPolicy(listing.getCancellationPolicy().name());
//     dto.setStatus(listing.getStatus().name());
//     dto.setHomeCity(listing.getHomeCity());

//     // PHÂN TÍCH WKT ĐỂ LẤY LAT/LNG
//     String wkt = listing.getHomeLocation();
//     if (wkt != null && wkt.startsWith("POINT(")) {
//         try {
//             String coords = wkt.substring(6, wkt.length() - 1); 
//             String[] parts = coords.trim().split("\\s+");
//             if (parts.length == 2) {
//                 dto.setLongitude(Double.parseDouble(parts[0])); // lng
//                 dto.setLatitude(Double.parseDouble(parts[1]));  // lat
//             }
//         } catch (Exception e) {
//             System.err.println("Lỗi parse WKT: " + wkt);
//         }
//     }
//     return dto;
// }

  @Override
@Transactional
public CarListingsDto createCarListing(CarListingsForm form, Long ownerId) {
    User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    Cars car = vehicleRepository.findByIdAndOwner(form.getVehicleId(), owner)
            .orElseThrow(() -> new IllegalArgumentException("Car not found or not owned by user"));

    if (carListingsRepository.existsByVehicleIdAndStatus(form.getVehicleId(), CarListings.ListingStatus.ACTIVE)) {
        throw new IllegalArgumentException("Car already has an active listing");
    }

    if (form.getLongitude() == null || form.getLatitude() == null) {
        throw new IllegalArgumentException("Tọa độ không hợp lệ");
    }

    System.out.println("FORM DATA: lng=" + form.getLongitude() + ", lat=" + form.getLatitude());

    // TẠO WKT THAY VÌ POINT
    String wkt = String.format("POINT(%.6f %.6f)", form.getLongitude(), form.getLatitude());
    System.out.println("WKT: " + wkt);

    // TẠO LISTING MỚI (CHỈ 1 LẦN)
    CarListings listing = new CarListings();
    listing.setVehicle(car);
    listing.setTitle(form.getTitle());
    listing.setDescription(form.getDescription());
    listing.setPrice24hCents(form.getPrice24hCents());
    listing.setKmLimit24h(form.getKmLimit24h());
    listing.setInstantBook(form.getInstantBook());
    listing.setCancellationPolicy(CarListings.CancellationPolicy.valueOf(form.getCancellationPolicy()));
    listing.setStatus(CarListings.ListingStatus.valueOf(form.getStatus()));
    listing.setHomeLocation(wkt);  // WKT string
    listing.setHomeCity(form.getHomeCity());
    listing.setCreatedAt(Instant.now());
    listing.setUpdatedAt(Instant.now());

    CarListings savedListing = carListingsRepository.save(listing);
    return mapToCarListingsDto(savedListing);
}

    @Override
    public void deleteVehicleAsAdmin(Long id) {
        Cars car = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe với ID: " + id));
        vehicleRepository.delete(car);
    }
    @Override
public List<CarListingsDto> getListingsByOwner(Long ownerId) {
    User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    List<CarListings> listings = carListingsRepository.findByVehicleOwnerId(ownerId);
    return listings.stream().map(this::mapToCarListingsDto).collect(Collectors.toList());
}

@Override
public CarListingsDto getListingByIdAndOwner(Long id, Long ownerId) {
    User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    CarListings listing = carListingsRepository.findByIdAndVehicleOwnerId(id, ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found or not owned by user"));
    return mapToCarListingsDto(listing);
}

@Override
@Transactional
public CarListingsDto updateCarListing(Long id, CarListingsForm form, Long ownerId) {
    User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    CarListings listing = carListingsRepository.findByIdAndVehicleOwnerId(id, ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found or not owned by user"));

    // Cập nhật
    listing.setTitle(form.getTitle());
    listing.setDescription(form.getDescription());
    listing.setPrice24hCents(form.getPrice24hCents());
    listing.setKmLimit24h(form.getKmLimit24h());
    listing.setInstantBook(form.getInstantBook());
    listing.setCancellationPolicy(CarListings.CancellationPolicy.valueOf(form.getCancellationPolicy()));
    listing.setStatus(CarListings.ListingStatus.valueOf(form.getStatus()));
    listing.setHomeCity(form.getHomeCity());

    String wkt = String.format("POINT(%.6f %.6f)", form.getLongitude(), form.getLatitude());
    listing.setHomeLocation(wkt);

    listing.setUpdatedAt(Instant.now());

    CarListings saved = carListingsRepository.save(listing);
    return mapToCarListingsDto(saved);
}

@Override
@Transactional
public void deleteCarListing(Long id, Long ownerId) {
    User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    CarListings listing = carListingsRepository.findByIdAndVehicleOwnerId(id, ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found or not owned by user"));
    carListingsRepository.delete(listing);
}
@Override
public List<CarListingsDto> getActiveListings() {
    List<CarListings> listings = carListingsRepository.findByStatus(CarListings.ListingStatus.ACTIVE);
    return listings.stream().map(this::mapToCarListingsDto).collect(Collectors.toList());
}

@Override
public List<CarListingsDto> searchListings(String location, LocalDate startDate, LocalDate endDate,
                                          Double maxPrice, String fuelType, Integer seats) {
    List<CarListings> listings;

    if (location != null && !location.trim().isEmpty()) {
        listings = carListingsRepository.findByHomeCityContainingIgnoreCaseAndStatus(
            location.trim(), CarListings.ListingStatus.ACTIVE);
    } else {
        listings = carListingsRepository.findByStatus(CarListings.ListingStatus.ACTIVE);
    }

    return listings.stream()
            .filter(l -> maxPrice == null || (l.getPrice24hCents() / 100.0) <= maxPrice)
            .filter(l -> fuelType == null || l.getVehicle().getFuelType().equalsIgnoreCase(fuelType))
            .filter(l -> seats == null || l.getVehicle().getSeats() >= seats)
            .map(this::mapToCarListingsDto)
            .collect(Collectors.toList());
}

// Cập nhật mapToCarListingsDto để include info từ Cars
private CarListingsDto mapToCarListingsDto(CarListings listing) {
    CarListingsDto dto = new CarListingsDto();
    dto.setId(listing.getId());
    dto.setVehicleId(listing.getVehicle().getId());
    dto.setTitle(listing.getTitle());
    dto.setDescription(listing.getDescription());
    dto.setPrice24hCents(listing.getPrice24hCents());
    dto.setKmLimit24h(listing.getKmLimit24h());
    dto.setInstantBook(listing.getInstantBook());
    dto.setCancellationPolicy(listing.getCancellationPolicy().name());
    dto.setStatus(listing.getStatus().name());
    dto.setHomeCity(listing.getHomeCity());

    // === THÔNG TIN XE ===
    Cars vehicle = listing.getVehicle();
    if (vehicle != null) {
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setImageUrls(vehicle.getImageUrls() != null ? vehicle.getImageUrls() : new ArrayList<>());
        dto.setDailyPrice(vehicle.getDailyPrice());
    }

    // === TỌA ĐỘ ===
    String wkt = listing.getHomeLocation();
    if (wkt != null && wkt.startsWith("POINT(")) {
        try {
            String coords = wkt.substring(6, wkt.length() - 1).trim();
            String[] parts = coords.split("\\s+");
            if (parts.length == 2) {
                dto.setLongitude(Double.parseDouble(parts[0]));
                dto.setLatitude(Double.parseDouble(parts[1]));
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse WKT: " + wkt);
        }
    }
    return dto;
}
@Override
public CarListingsDto getListingById(Long id) {
    CarListings listing = carListingsRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + id));
    return mapToCarListingsDto(listing);
}
}