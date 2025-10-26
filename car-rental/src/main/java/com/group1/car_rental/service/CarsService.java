package com.group1.car_rental.service;

import com.group1.car_rental.dto.CarsDto;
import com.group1.car_rental.dto.CarsForm;
import java.util.List;

public interface CarsService {
    List<CarsDto> getAllCars();
    List<CarsDto> searchByLocation(String location);
    List<CarsDto> getVehiclesByOwner(Long ownerId);
    CarsDto getVehicleByIdAndOwner(Long id, Long ownerId);
    CarsDto createVehicle(CarsForm form, Long ownerId);
    CarsDto updateVehicle(Long id, CarsForm form, Long ownerId);
    void deleteVehicle(Long id, Long ownerId);
}
