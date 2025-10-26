package com.group1.car_rental.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CarsForm {
    private Long id;

    @NotBlank(message = "Hãng xe không được trống")
    @Size(max = 50, message = "Hãng xe quá dài")
    private String make;

    @NotBlank(message = "Model không được trống")
    @Size(max = 80, message = "Model quá dài")
    private String model;

    @Min(value = 1980, message = "Năm sản xuất không hợp lệ")
    @Max(value = 2026, message = "Năm sản xuất không hợp lệ")
    private Short year;

    @NotBlank(message = "Loại hộp số không được trống")
    @Pattern(regexp = "^(MANUAL|AUTO)$", message = "Loại hộp số không hợp lệ")
    private String transmission;

    @NotBlank(message = "Loại nhiên liệu không được trống")
    @Pattern(regexp = "^(PETROL|DIESEL|ELECTRIC|HYBRID)$", message = "Loại nhiên liệu không hợp lệ")
    private String fuelType;

    @Min(value = 1, message = "Số ghế phải lớn hơn 0")
    @Max(value = 12, message = "Số ghế tối đa là 12")
    private Byte seats;

    @NotNull(message = "Giá thuê không được trống")
    @Positive(message = "Giá thuê phải lớn hơn 0")
    private Double dailyPrice;

    @Size(max = 255, message = "URL ảnh quá dài")
    private String imageUrl;

    @NotBlank(message = "Thành phố không được trống")
    @Size(max = 100, message = "Tên thành phố quá dài")
    private String city;
}