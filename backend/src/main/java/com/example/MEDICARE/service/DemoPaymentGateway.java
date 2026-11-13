package com.example.MEDICARE.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.List;

/**
 * A pretend payment processor ("DemoPay"). It validates the details the way a real gateway would
 * and decides success or failure from well-known TEST values - no money ever moves.
 *
 *   Cards   4242 4242 4242 4242 -> approved        4000 0000 0000 0002 -> declined
 *           5555 5555 5555 4444 -> approved        4000 0000 0000 9995 -> insufficient funds
 *   Banks   any valid 10-digit account number -> approved; numbers ending in 0000 -> insufficient funds
 */
@Component
public class DemoPaymentGateway {

    public static final List<String> BANKS = List.of(
            "Access Bank", "Citibank Nigeria", "Ecobank Nigeria", "Fidelity Bank", "First Bank of Nigeria",
            "First City Monument Bank (FCMB)", "Guaranty Trust Bank (GTBank)", "Kuda Bank", "Moniepoint MFB",
            "OPay", "PalmPay", "Polaris Bank", "Stanbic IBTC Bank", "Sterling Bank", "United Bank for Africa (UBA)",
            "Union Bank", "Wema Bank", "Zenith Bank");

    public record Result(boolean success, String message, String transactionRef, String cardBrand, String last4) {}

    private final SecureRandom random = new SecureRandom();

    private String txnRef() {
        StringBuilder sb = new StringBuilder("TXN");
        for (int i = 0; i < 12; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    public Result chargeCard(String number, String holder, String expiry, String cvv) {
        String digits = number == null ? "" : number.replaceAll("[\\s-]", "");
        if (holder == null || holder.isBlank()) return fail("Enter the name on the card.");
        if (!digits.matches("\\d{13,19}") || !luhn(digits)) return fail("That card number isn't valid.");
        if (cvv == null || !cvv.matches("\\d{3,4}")) return fail("Enter the 3 or 4 digit security code (CVV).");

        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])\\s*/\\s*\\d{2}")) return fail("Enter the expiry date as MM/YY.");
        String[] p = expiry.replace(" ", "").split("/");
        YearMonth exp = YearMonth.of(2000 + Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        if (exp.isBefore(YearMonth.now())) return fail("This card has expired.");

        String last4 = digits.substring(digits.length() - 4);
        String brand = brand(digits);

        if (digits.equals("4000000000000002")) return new Result(false, "Card declined by your bank.", null, brand, last4);
        if (digits.equals("4000000000009995")) return new Result(false, "Insufficient funds on this card.", null, brand, last4);
        return new Result(true, "Approved", txnRef(), brand, last4);
    }

    public Result chargeBank(String bankName, String accountNumber, String accountName) {
        if (bankName == null || !BANKS.contains(bankName)) return fail("Choose a bank from the list.");
        if (accountName == null || accountName.isBlank()) return fail("Enter the account name.");
        String acct = accountNumber == null ? "" : accountNumber.trim();
        if (!acct.matches("\\d{10}")) return fail("Account numbers have exactly 10 digits.");
        if (acct.endsWith("0000")) return new Result(false, "Insufficient funds in this account.", null, null, null);
        return new Result(true, "Approved", txnRef(), null, null);
    }

    public static String maskAccount(String acct) {
        if (acct == null || acct.length() < 7) return acct;
        return acct.substring(0, 3) + "****" + acct.substring(acct.length() - 3);
    }

    private Result fail(String m) { return new Result(false, m, null, null, null); }

    private static boolean luhn(String d) {
        int sum = 0;
        boolean alt = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (alt) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private static String brand(String d) {
        if (d.startsWith("4")) return "Visa";
        if (d.matches("^(5[1-5]|2[2-7]).*")) return "Mastercard";
        if (d.startsWith("506") || d.startsWith("650")) return "Verve";
        if (d.matches("^3[47].*")) return "Amex";
        return "Card";
    }
}
