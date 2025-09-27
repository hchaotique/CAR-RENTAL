package com.group1.car_rental.mapper;

import com.group1.car_rental.dto.VehicleDto;
import com.group1.car_rental.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleDto toDto(Vehicle vehicle);

    List<VehicleDto> toDtoList(List<Vehicle> vehicles);

    @Mapping(target = "id", ignore = true)
    void updateVehicleFromDto(VehicleDto dto, @MappingTarget Vehicle vehicle);
}
