package com.group1.car_rental.dto;

import lombok.Data;

@Data
public class VehicleDto {
    private Long id;
    private String make;
    private String model;
    private Short year;
    private String transmission;
    private String fuelType;
    private Byte seats;
    private Double dailyPrice;
    private String imageUrl;
    private String city;
    private Double rating;
    private Integer numReviews;
}
