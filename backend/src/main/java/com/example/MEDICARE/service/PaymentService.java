package com.example.MEDICARE.service;

import com.example.MEDICARE.dto.Requests.PayRequest;
import com.example.MEDICARE.dto.Views.Receipt;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DemoPaymentGateway gateway;

    @Value("${app.hospital.name:MediCare Lite Hospital}") private String hospitalName;
    @Value("${app.hospital.address:}") private String hospitalAddress;
    @Value("${app.hospital.phone:}") private String hospitalPhone;
    @Value("${app.timezone:Africa/Lagos}") private String timezone;

    private final SecureRandom random = new SecureRandom();

    public record PayOutcome(Payment payment, boolean success, String message) {}

    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of(timezone)); }

    private String newReference() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String date = LocalDate.now(ZoneId.of(timezone)).format(DateTimeFormatter.BASIC_ISO_DATE);
        String ref;
        do {
            StringBuilder sb = new StringBuilder("MCL-" + date + "-");
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
            ref = sb.toString();
        } while (paymentRepository.existsByReference(ref));
        return ref;
    }

    /** Raises a PENDING charge on a patient's account. */
    @Transactional
    public Payment createCharge(Patient patient, Appointment appointment, Admission admission,
                                BigDecimal amount, String description) {
        if (amount == null || amount.signum() <= 0) throw ApiException.badRequest("Amount must be greater than zero.");
        Payment p = new Payment();
        p.setPatient(patient);
        p.setAppointment(appointment);
        p.setAdmission(admission);
        p.setAmount(amount);
        p.setDescription(description);
        p.setReference(newReference());
        p.setCreatedAt(now());
        return paymentRepository.save(p);
    }

    /** Runs a PENDING payment through the demo gateway. Failed attempts leave it PENDING with a reason. */
    @Transactional
    public PayOutcome pay(Payment payment, PayRequest req, String payerAccountEmail) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("This payment is already " + payment.getStatus().name().toLowerCase() + ".");
        }
        if (req.method() == null || req.method() == PaymentMethod.CASH) {
            throw ApiException.badRequest("Choose card or bank transfer.");
        }

        DemoPaymentGateway.Result r;
        if (req.method() == PaymentMethod.CARD) {
            r = gateway.chargeCard(req.cardNumber(), req.cardHolder(), req.expiry(), req.cvv());
        } else {
            r = gateway.chargeBank(req.bankName(), req.accountNumber(), req.accountName());
        }

        if (!r.success()) {
            payment.setFailureReason(r.message());
            paymentRepository.save(payment);
            return new PayOutcome(payment, false, r.message());
        }

        payment.setMethod(req.method());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now());
        payment.setFailureReason(null);
        payment.setTransactionRef(r.transactionRef());
        payment.setPayerEmail(req.payerEmail() != null && !req.payerEmail().isBlank() ? req.payerEmail() : payerAccountEmail);
        if (req.method() == PaymentMethod.CARD) {
            payment.setPayerName(req.cardHolder().trim());
            payment.setCardBrand(r.cardBrand());
            payment.setCardLast4(r.last4()); // never the full number, never the CVV
            payment.setBankName(r.cardBrand() + " card");
        } else {
            payment.setPayerName(req.accountName().trim());
            payment.setAccountName(req.accountName().trim());
            payment.setBankName(req.bankName());
            payment.setAccountNumber(DemoPaymentGateway.maskAccount(req.accountNumber().trim()));
        }
        payment.setReceiptNumber("RCT-" + payment.getPaidAt().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%05d", payment.getId()));
        paymentRepository.save(payment);
        return new PayOutcome(payment, true, "Payment successful");
    }

    /** Admin records money received over the counter. */
    @Transactional
    public Payment markCash(Payment payment, String receivedBy, String payerName) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("This payment is already " + payment.getStatus().name().toLowerCase() + ".");
        }
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now());
        payment.setPayerName(payerName != null && !payerName.isBlank() ? payerName : payment.getPatient().getFullName());
        payment.setBankName("Cash at hospital cashier");
        payment.setReceivedBy(receivedBy);
        payment.setTransactionRef("CASH" + payment.getId());
        payment.setReceiptNumber("RCT-" + payment.getPaidAt().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%05d", payment.getId()));
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refund(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) throw ApiException.badRequest("Only paid payments can be refunded.");
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment voidPayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) throw ApiException.badRequest("Only unpaid charges can be voided.");
        payment.setStatus(PaymentStatus.VOIDED);
        return paymentRepository.save(payment);
    }

    /** Voids any unpaid charge attached to an appointment (used when it gets cancelled). */
    @Transactional
    public void voidPendingForAppointment(Long appointmentId) {
        for (Payment p : paymentRepository.findByAppointmentIdAndStatus(appointmentId, PaymentStatus.PENDING)) {
            p.setStatus(PaymentStatus.VOIDED);
            paymentRepository.save(p);
        }
    }

    public Receipt receipt(Payment p) {
        if (p.getStatus() != PaymentStatus.PAID && p.getStatus() != PaymentStatus.REFUNDED) {
            throw ApiException.badRequest("A receipt is only available once the payment is complete.");
        }
        Patient pt = p.getPatient();
        String appt = null;
        if (p.getAppointment() != null) {
            Appointment a = p.getAppointment();
            appt = a.getAppointmentType().getLabel() + " with Dr " + a.getDoctor().getUser().getFullName()
                    + " on " + a.getAppointmentDate();
        }
        return new Receipt(hospitalName, hospitalAddress, hospitalPhone,
                p.getReceiptNumber(), p.getReference(), p.getTransactionRef(),
                p.getPaidAt(), p.getAmount(), "NGN", p.getDescription(), p.getStatus(),
                pt.getFullName(), String.format("MCL-P%05d", pt.getId()), pt.getPhoneNumber(),
                p.getPayerName(), p.getPayerEmail(), p.getMethod(),
                p.getBankName(), p.getAccountName(), p.getAccountNumber(), p.getCardBrand(), p.getCardLast4(),
                p.getReceivedBy(), appt);
    }
}
