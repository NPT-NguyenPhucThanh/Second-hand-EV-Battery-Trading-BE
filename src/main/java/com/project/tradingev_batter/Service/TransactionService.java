package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public TransactionService(TransactionRepository transactionRepository,
                             OrderRepository orderRepository) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
    }

    //Lấy tất cả transactions của một order
    //Bao gồm: DEPOSIT, FINAL_PAYMENT, REFUND, COMMISSION
    public List<Transaction> getTransactionsByOrderId(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));

        return transactionRepository.findByOrders_Orderid(orderId);
    }

    //Lấy transaction history với thông tin chi tiết
    //Trả về Map với các thông tin hữu ích cho frontend
    public Map<String, Object> getTransactionHistoryDetail(Long orderId) {
        List<Transaction> transactions = getTransactionsByOrderId(orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("totalTransactions", transactions.size());

        // Phân loại transactions theo type
        Map<TransactionType, List<Transaction>> groupedByType = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionType));

        // Tính tổng tiền theo từng loại
        double totalDeposit = groupedByType.getOrDefault(TransactionType.DEPOSIT, new ArrayList<>())
                .stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalFinalPayment = groupedByType.getOrDefault(TransactionType.FINAL_PAYMENT, new ArrayList<>())
                .stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalRefund = groupedByType.getOrDefault(TransactionType.REFUND, new ArrayList<>())
                .stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalCommission = groupedByType.getOrDefault(TransactionType.COMMISSION, new ArrayList<>())
                .stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalBatteryPayment = groupedByType.getOrDefault(TransactionType.BATTERY_PAYMENT, new ArrayList<>())
                .stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Thông tin tổng hợp
        result.put("summary", Map.of(
            "totalDeposit", totalDeposit,
            "totalFinalPayment", totalFinalPayment,
            "totalBatteryPayment", totalBatteryPayment,
            "totalRefund", totalRefund,
            "totalCommission", totalCommission,
            "netAmount", totalDeposit + totalFinalPayment + totalBatteryPayment - totalRefund - totalCommission
        ));

        // Chi tiết transactions theo loại
        Map<String, List<Map<String, Object>>> transactionsByType = new HashMap<>();

        for (TransactionType type : TransactionType.values()) {
            List<Transaction> transactionsOfType = groupedByType.getOrDefault(type, new ArrayList<>());
            List<Map<String, Object>> formattedTransactions = transactionsOfType.stream()
                    .map(this::formatTransaction)
                    .collect(Collectors.toList());
            transactionsByType.put(type.name(), formattedTransactions);
        }

        result.put("transactionsByType", transactionsByType);

        // Tất cả transactions (sorted by date)
        List<Map<String, Object>> allTransactions = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getCreatedat))
                .map(this::formatTransaction)
                .collect(Collectors.toList());

        result.put("allTransactions", allTransactions);

        return result;
    }

    //Lấy tất cả transactions của một user
    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByCreatedBy_Userid(userId);
    }

    //Lấy transaction detail
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction không tồn tại"));
    }

    //Format transaction thành Map để trả về frontend
    private Map<String, Object> formatTransaction(Transaction transaction) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("transactionId", transaction.getTransid());
        formatted.put("transactionCode", transaction.getTransactionCode());
        formatted.put("transactionType", transaction.getTransactionType());
        formatted.put("amount", transaction.getAmount());
        formatted.put("method", transaction.getMethod());
        formatted.put("status", transaction.getStatus());
        formatted.put("description", transaction.getDescription());
        formatted.put("createdAt", transaction.getCreatedat());
        formatted.put("paymentDate", transaction.getPaymentDate());
        formatted.put("vnpayTransactionNo", transaction.getVnpayTransactionNo());
        formatted.put("bankCode", transaction.getBankCode());
        formatted.put("cardType", transaction.getCardType());
        formatted.put("isEscrowed", transaction.getIsEscrowed());
        formatted.put("escrowReleaseDate", transaction.getEscrowReleaseDate());

        return formatted;
    }

    //Kiểm tra order có transactions pending không
    public boolean hasPendingTransactions(Long orderId) {
        List<Transaction> transactions = getTransactionsByOrderId(orderId);
        return transactions.stream()
                .anyMatch(t -> t.getStatus() == com.project.tradingev_batter.enums.TransactionStatus.PENDING);
    }

    //Tính tổng tiền đã thanh toán cho order
    public double getTotalPaidAmount(Long orderId) {
        List<Transaction> transactions = getTransactionsByOrderId(orderId);
        return transactions.stream()
                .filter(t -> t.getStatus() == com.project.tradingev_batter.enums.TransactionStatus.SUCCESS)
                .filter(t -> t.getAmount() != null)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
}

