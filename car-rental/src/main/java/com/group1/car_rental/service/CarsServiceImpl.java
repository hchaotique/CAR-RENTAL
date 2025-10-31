package com.group1.car_rental.service;

import com.group1.car_rental.dto.CarsDto;
import com.group1.car_rental.dto.CarsForm;
import com.group1.car_rental.entity.Cars;
import com.group1.car_rental.entity.User;
import com.group1.car_rental.mapper.CarsMapper;
import com.group1.car_rental.repository.CarsRepository;
import com.group1.car_rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarsServiceImpl implements CarsService {

    private final CarsRepository vehicleRepository;
    private final CarsMapper vehicleMapper;
    private final UserRepository userRepository;

    @Override
    public List<CarsDto> getAllCars() {
        List<Cars> cars = vehicleRepository.findAll();
        return vehicleMapper.toDtoList(cars);
    }

    @Override
    public List<CarsDto> searchByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return getAllCars();
        }
        List<Cars> cars = vehicleRepository.searchByLocation(location.trim());
        return vehicleMapper.toDtoList(cars);
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
        car.setImageUrl(form.getImageUrl());
        car.setCity(form.getCity());
        car.setCreatedAt(java.time.Instant.now());
        car.setUpdatedAt(java.time.Instant.now());
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
        car.setImageUrl(form.getImageUrl());
        car.setCity(form.getCity());
        car.setUpdatedAt(java.time.Instant.now());
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
}
