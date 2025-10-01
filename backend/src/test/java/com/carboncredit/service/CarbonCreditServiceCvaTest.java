package com.carboncredit.service;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.User;
import com.carboncredit.repository.CarbonCreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarbonCreditServiceCvaTest {

    @Mock private CarbonCreditRepository carbonCreditRepository;
    @Mock private AuditService auditService;

    @InjectMocks private CarbonCreditService carbonCreditService;

    private User cvaUser;
    private CarbonCredit pendingCredit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cvaUser = new User();
        cvaUser.setRole(User.UserRole.CVA);
        cvaUser.setUsername("cva1");

        pendingCredit = new CarbonCredit();
        pendingCredit.setId(UUID.randomUUID());
        pendingCredit.setCo2ReducedKg(new BigDecimal("20"));
        pendingCredit.setCreditAmount(new BigDecimal("0.014"));
        pendingCredit.setStatus(CarbonCredit.CreditStatus.PENDING);

        when(carbonCreditRepository.findById(pendingCredit.getId())).thenReturn(Optional.of(pendingCredit));
        when(carbonCreditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void verify_byCva_changesStatusAndWritesAudit() {
        CarbonCredit result = carbonCreditService.verifyCarbonCredit(pendingCredit.getId(), cvaUser, "verified ok");
        assertEquals(CarbonCredit.CreditStatus.VERIFIED, result.getStatus());
        verify(auditService, times(1)).logVerification(eq(result), eq(cvaUser), any(), any(), eq("verified ok"));
    }

    @Test
    void verify_byNonCva_throws() {
        User nonCva = new User();
        nonCva.setRole(User.UserRole.BUYER);
        assertThrows(SecurityException.class, () -> {
            carbonCreditService.verifyCarbonCredit(pendingCredit.getId(), nonCva, null);
        });
        verify(auditService, never()).logVerification(any(), any(), any(), any(), any());
    }

    @Test
    void verify_alreadyVerified_isIdempotent() {
        pendingCredit.setStatus(CarbonCredit.CreditStatus.VERIFIED);
        CarbonCredit result = carbonCreditService.verifyCarbonCredit(pendingCredit.getId(), cvaUser, "comments");
        // Should return the same object (or the repository value)
        assertEquals(CarbonCredit.CreditStatus.VERIFIED, result.getStatus());
        // AuditService should not be called again
        verify(auditService, never()).logVerification(any(), any(), any(), any(), any());
    }
}
