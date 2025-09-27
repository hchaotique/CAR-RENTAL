package com.group1.car_rental.service;

import com.group1.car_rental.dto.VehicleDto;
import java.util.List;

public interface VehicleService {
    List<VehicleDto> getAllVehicles();
    List<VehicleDto> searchByLocation(String location);
}
