package com.group1.car_rental.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "driver_license_no")
    private byte[] driverLicenseNo;

    @Column(name = "driver_license_expiry")
    private LocalDate driverLicenseExpiry;

    @Column(name = "address_encrypted")
    private byte[] addressEncrypted;

    @Column(name = "kyc_status", nullable = false, length = 16)
    private String kycStatus = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructor for registration
    public UserProfile(String fullName, User user) {
        this.fullName = fullName;
        this.user = user;
        this.kycStatus = "PENDING";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getFullName() { return fullName; }
    public LocalDate getDob() { return dob; }
    public byte[] getDriverLicenseNo() { return driverLicenseNo; }
    public LocalDate getDriverLicenseExpiry() { return driverLicenseExpiry; }
    public byte[] getAddressEncrypted() { return addressEncrypted; }
    public String getKycStatus() { return kycStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public void setDriverLicenseNo(byte[] driverLicenseNo) { this.driverLicenseNo = driverLicenseNo; }
    public void setDriverLicenseExpiry(LocalDate driverLicenseExpiry) { this.driverLicenseExpiry = driverLicenseExpiry; }
    public void setAddressEncrypted(byte[] addressEncrypted) { this.addressEncrypted = addressEncrypted; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
