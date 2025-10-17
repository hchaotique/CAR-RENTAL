package com.group1.car_rental.service;

import com.group1.car_rental.dto.CarsDto;
import java.util.List;

public interface CarsService {
    List<CarsDto> getAllVehicles();
    List<CarsDto> searchByLocation(String location);
}
