package com.carboncredit.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.carboncredit.dto.*;
import com.carboncredit.entity.*;

/**
 * Utility class for converting entities to DTOs and preventing circular
 * references
 */
public class DTOMapper {

    // User mappings
    public static UserDTO toUserDTO(User user) {
        return user != null ? new UserDTO(user) : null;
    }

    public static List<UserDTO> toUserDTOList(List<User> users) {
        return users.stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    // Transaction mappings
    public static TransactionDTO toTransactionDTO(Transaction transaction) {
        return transaction != null ? new TransactionDTO(transaction) : null;
    }

    public static List<TransactionDTO> toTransactionDTOList(List<Transaction> transactions) {
        return transactions.stream()
                .map(TransactionDTO::new)
                .collect(Collectors.toList());
    }

    public static Page<TransactionDTO> toTransactionDTOPage(Page<Transaction> transactionPage) {
        List<TransactionDTO> dtoList = toTransactionDTOList(transactionPage.getContent());
        return new PageImpl<>(dtoList, transactionPage.getPageable(), transactionPage.getTotalElements());
    }

    // CarbonCredit mappings
    public static CarbonCreditDTO toCarbonCreditDTO(CarbonCredit credit) {
        return credit != null ? new CarbonCreditDTO(credit) : null;
    }

    public static CarbonCreditDTO toCarbonCreditDTO(CarbonCredit credit, boolean lightweight) {
        return credit != null ? new CarbonCreditDTO(credit) : null;
    }

    public static List<CarbonCreditDTO> toCarbonCreditDTOList(List<CarbonCredit> credits) {
        return credits.stream()
                .map(CarbonCreditDTO::new)
                .collect(Collectors.toList());
    }

    // CreditListing mappings
    public static CreditListingDTO toCreditListingDTO(CreditListing listing) {
        return listing != null ? new CreditListingDTO(listing) : null;
    }

    public static CreditListingDTO toCreditListingDTO(CreditListing listing, boolean lightweight) {
        return listing != null ? new CreditListingDTO(listing) : null;
    }

    public static List<CreditListingDTO> toCreditListingDTOList(List<CreditListing> listings) {
        return listings.stream()
                .map(CreditListingDTO::new)
                .collect(Collectors.toList());
    }

    public static Page<CreditListingDTO> toCreditListingDTOPage(Page<CreditListing> listingPage) {
        List<CreditListingDTO> dtoList = toCreditListingDTOList(listingPage.getContent());
        return new PageImpl<>(dtoList, listingPage.getPageable(), listingPage.getTotalElements());
    }

    // Wallet mappings
    public static WalletDTO toWalletDTO(Wallet wallet) {
        return wallet != null ? new WalletDTO(wallet) : null;
    }

    // Vehicle mappings
    public static VehicleDTO toVehicleDTO(Vehicle vehicle) {
        return vehicle != null ? new VehicleDTO(vehicle) : null;
    }

    public static List<VehicleDTO> toVehicleDTOList(List<Vehicle> vehicles) {
        return vehicles.stream()
                .map(VehicleDTO::new)
                .collect(Collectors.toList());
    }

    // JourneyData mappings
    public static JourneyDataDTO toJourneyDataDTO(JourneyData journey) {
        return journey != null ? new JourneyDataDTO(journey) : null;
    }

    public static List<JourneyDataDTO> toJourneyDataDTOList(List<JourneyData> journeys) {
        return journeys.stream()
                .map(JourneyDataDTO::new)
                .collect(Collectors.toList());
    }

    // Dispute mappings
    public static DisputeDTO toDisputeDTO(Dispute dispute) {
        return dispute != null ? new DisputeDTO(dispute) : null;
    }

    public static List<DisputeDTO> toDisputeDTOList(List<Dispute> disputes) {
        return disputes.stream()
                .map(DisputeDTO::new)
                .collect(Collectors.toList());
    }

    public static Page<DisputeDTO> toDisputeDTOPage(Page<Dispute> disputePage) {
        List<DisputeDTO> dtoList = toDisputeDTOList(disputePage.getContent());
        return new PageImpl<>(dtoList, disputePage.getPageable(), disputePage.getTotalElements());
    }

    // Certificate mappings
    public static CertificateDTO toCertificateDTO(Certificate certificate) {
        return certificate != null ? new CertificateDTO(certificate) : null;
    }

    public static List<CertificateDTO> toCertificateDTOList(List<Certificate> certificates) {
        return certificates.stream()
                .map(CertificateDTO::new)
                .collect(Collectors.toList());
    }

    // AuditLog mappings
    public static AuditLogDTO toAuditLogDTO(AuditLog auditLog) {
        return auditLog != null ? new AuditLogDTO(auditLog) : null;
    }

    public static List<AuditLogDTO> toAuditLogDTOList(List<AuditLog> auditLogs) {
        return auditLogs.stream()
                .map(AuditLogDTO::new)
                .collect(Collectors.toList());
    }

    // Notification mappings
    public static NotificationDTO toNotificationDTO(Notification notification) {
        return notification != null ? new NotificationDTO(notification) : null;
    }

    public static List<NotificationDTO> toNotificationDTOList(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationDTO::new)
                .collect(Collectors.toList());
    }

    public static Page<NotificationDTO> toNotificationDTOPage(Page<Notification> notificationPage) {
        List<NotificationDTO> dtoList = toNotificationDTOList(notificationPage.getContent());
        return new PageImpl<>(dtoList, notificationPage.getPageable(), notificationPage.getTotalElements());
    }

    // SystemSetting mappings
    public static SystemSettingDTO toSystemSettingDTO(SystemSetting setting) {
        return setting != null ? new SystemSettingDTO(setting) : null;
    }

    public static List<SystemSettingDTO> toSystemSettingDTOList(List<SystemSetting> settings) {
        return settings.stream()
                .map(SystemSettingDTO::new)
                .collect(Collectors.toList());
    }
}