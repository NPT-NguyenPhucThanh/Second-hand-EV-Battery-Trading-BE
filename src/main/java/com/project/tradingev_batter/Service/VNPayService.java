package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.config.VNPayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    public VNPayService(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }

    //TẠO PAYMENT URL - Tạo URL để redirect user sang VNPay
    public String createPaymentUrl(double amount, String orderInfo, String transactionCode, String ipAddress) {
        try {
            log.info("Creating VNPay payment URL for transaction: {}, amount: {}", transactionCode, amount);
            
            // Convert amount to VNPay format (x100, no decimal)
            long vnpAmount = (long) (amount * 100);
            
            // Create timestamp
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(new Date());
            
            // Calculate expiry time (1 hour from now)
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            calendar.add(Calendar.MINUTE, vnPayConfig.getTimeoutMinutes());
            String vnp_ExpireDate = formatter.format(calendar.getTime());
            
            // Build parameters
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
            vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
            vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
            vnp_Params.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
            vnp_Params.put("vnp_TxnRef", transactionCode); // Mã giao dịch nội bộ
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
            vnp_Params.put("vnp_Locale", vnPayConfig.getLocale());
            vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnp_Params.put("vnp_IpnUrl", vnPayConfig.getIpnUrl());
            vnp_Params.put("vnp_IpAddr", ipAddress);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            // Build query string và tạo secure hash
            String queryUrl = buildQueryUrl(vnp_Params);
            String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), queryUrl);
            
            // Build final payment URL
            String paymentUrl = vnPayConfig.getApiUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
            
            log.info("Payment URL created successfully for transaction: {}", transactionCode);
            return paymentUrl;
            
        } catch (Exception e) {
            log.error("Error creating payment URL for transaction: {}", transactionCode, e);
            throw new RuntimeException("Không thể tạo link thanh toán: " + e.getMessage());
        }
    }

    //VERIFY CALLBACK - Xác thực callback từ VNPay
    public boolean verifyCallback(Map<String, String> params) {
        try {
            // Lấy secure hash từ VNPay
            String vnp_SecureHash = params.get("vnp_SecureHash");
            
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                log.warn("Missing vnp_SecureHash in callback");
                return false;
            }
            
            // Remove secure hash và sign type từ params để tính lại hash
            Map<String, String> verifyParams = new HashMap<>(params);
            verifyParams.remove("vnp_SecureHash");
            verifyParams.remove("vnp_SecureHashType");
            
            // Build query string từ params (đã sort)
            String queryUrl = buildQueryUrl(verifyParams);
            
            // Tính hash với secret key
            String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), queryUrl);
            
            // So sánh hash
            boolean isValid = calculatedHash.equalsIgnoreCase(vnp_SecureHash);
            
            if (!isValid) {
                log.warn("Invalid secure hash. Expected: {}, Got: {}", calculatedHash, vnp_SecureHash);
            } else {
                log.info("Callback verified successfully for transaction: {}", params.get("vnp_TxnRef"));
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying callback", e);
            return false;
        }
    }

    //PARSE VNPAY RESPONSE - Parse response từ VNPay thành Map
    public Map<String, String> parseVNPayResponse(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        
        result.put("transactionCode", params.get("vnp_TxnRef"));
        result.put("vnpayTransactionNo", params.get("vnp_TransactionNo"));
        result.put("amount", params.get("vnp_Amount")); // Nhớ chia 100
        result.put("bankCode", params.get("vnp_BankCode"));
        result.put("cardType", params.get("vnp_CardType"));
        result.put("responseCode", params.get("vnp_ResponseCode"));
        result.put("transactionStatus", params.get("vnp_TransactionStatus"));
        result.put("paymentDate", params.get("vnp_PayDate"));
        
        return result;
    }

    //CHECK PAYMENT SUCCESS - Kiểm tra xem payment có thành công không
    public boolean isPaymentSuccess(String responseCode) {
        return "00".equals(responseCode);
    }

    //GET RESPONSE CODE MESSAGE - Lấy message từ response code
    public String getResponseMessage(String responseCode) {
        Map<String, String> messages = new HashMap<>();
        messages.put("00", "Giao dịch thành công");
        messages.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).");
        messages.put("09", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.");
        messages.put("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần");
        messages.put("11", "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.");
        messages.put("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.");
        messages.put("13", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP). Xin quý khách vui lòng thực hiện lại giao dịch.");
        messages.put("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch");
        messages.put("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.");
        messages.put("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.");
        messages.put("75", "Ngân hàng thanh toán đang bảo trì.");
        messages.put("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định. Xin quý khách vui lòng thực hiện lại giao dịch");
        messages.put("99", "Các lỗi khác (lỗi còn lại, không có trong danh sách mã lỗi đã liệt kê)");
        
        return messages.getOrDefault(responseCode, "Lỗi không xác định");
    }

    //BUILD QUERY URL - Xây dựng query string từ params (sorted by key)
    private String buildQueryUrl(Map<String, String> params) {
        // Sort params by key (VNPay yêu cầu)
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    
                    if (itr.hasNext()) {
                        query.append('&');
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding URL", e);
                }
            }
        }
        
        return query.toString();
    }

    //HMAC SHA512 - Tạo secure hash
    //ĐÂY LÀ TRÁI TIM CỦA SECURITY!
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            
            byte[] hashBytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder result = new StringBuilder();
            for (byte b : hashBytes) {
                result.append(String.format("%02x", b));
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512", e);
            throw new RuntimeException("Lỗi tạo chữ ký bảo mật");
        }
    }

    //GENERATE TRANSACTION CODE - Tạo mã giao dịch unique
    public String generateTransactionCode(Long orderId, String type) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = formatter.format(new Date());
        return type + "_" + orderId + "_" + timestamp;
    }

    //MOCK PAYMENT SUCCESS - Mock cho testing (khi mock-mode = true)
    public Map<String, String> mockPaymentSuccess(String transactionCode, double amount) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String payDate = formatter.format(new Date());
        
        Map<String, String> mockResponse = new HashMap<>();
        mockResponse.put("vnp_TxnRef", transactionCode);
        mockResponse.put("vnp_TransactionNo", "MOCK" + System.currentTimeMillis());
        mockResponse.put("vnp_Amount", String.valueOf((long)(amount * 100)));
        mockResponse.put("vnp_BankCode", "NCB");
        mockResponse.put("vnp_CardType", "ATM");
        mockResponse.put("vnp_ResponseCode", "00");
        mockResponse.put("vnp_TransactionStatus", "00");
        mockResponse.put("vnp_PayDate", payDate);
        
        log.info("Mock payment success for transaction: {}", transactionCode);
        return mockResponse;
    }

    //IS MOCK MODE - Kiểm tra có đang ở mock mode không
    public boolean isMockMode() {
        return vnPayConfig.isMockMode();
    }
}
