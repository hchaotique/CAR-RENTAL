package com.group1.car_rental.service;

import com.group1.car_rental.dto.VehicleDto;
import com.group1.car_rental.entity.Vehicle;
import com.group1.car_rental.mapper.VehicleMapper;
import com.group1.car_rental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    public List<VehicleDto> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return vehicleMapper.toDtoList(vehicles);
    }

    @Override
    public List<VehicleDto> searchByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return getAllVehicles();
        }
        List<Vehicle> vehicles = vehicleRepository.searchByLocation(location.trim());
        return vehicleMapper.toDtoList(vehicles);
    }
}
