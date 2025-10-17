package com.group1.car_rental.service;

import com.group1.car_rental.dto.CarsDto;
import com.group1.car_rental.entity.Cars;
import com.group1.car_rental.mapper.CarsMapper;
import com.group1.car_rental.repository.CarsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarsServiceImpl implements CarsService {

    private final CarsRepository vehicleRepository;
    private final CarsMapper vehicleMapper;

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
}
