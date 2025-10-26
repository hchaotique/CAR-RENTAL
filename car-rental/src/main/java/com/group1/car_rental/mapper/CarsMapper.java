package com.group1.car_rental.mapper;

import com.group1.car_rental.dto.CarsDto;
import com.group1.car_rental.entity.Cars;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CarsMapper {

    CarsDto toDto(Cars cars);

    List<CarsDto> toDtoList(List<Cars> cars);

    @Mapping(target = "id", ignore = true)
    void updateVehicleFromDto(CarsDto dto, @MappingTarget Cars cars);
}
