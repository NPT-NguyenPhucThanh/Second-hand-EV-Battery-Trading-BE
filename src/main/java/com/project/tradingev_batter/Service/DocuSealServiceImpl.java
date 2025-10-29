package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.ContractsRepository;
import com.project.tradingev_batter.Repository.NotificationRepository;
import com.project.tradingev_batter.Repository.ProductRepository;
import com.project.tradingev_batter.config.DocuSealConfig;
import com.project.tradingev_batter.dto.docuseal.DocuSealSubmissionRequest;
import com.project.tradingev_batter.dto.docuseal.DocuSealSubmissionResponse;
import com.project.tradingev_batter.dto.docuseal.DocuSealWebhookPayload;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.ProductStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;


//Xử lý tất cả logic liên quan đến DocuSeal API
@Service
@Slf4j
public class DocuSealServiceImpl implements DocuSealService {

    private final DocuSealConfig docuSealConfig;
    private final RestTemplate docuSealRestTemplate;
    private final ContractsRepository contractsRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;

    public DocuSealServiceImpl(
            DocuSealConfig docuSealConfig,
            @Qualifier("docuSealRestTemplate") RestTemplate docuSealRestTemplate,
            ContractsRepository contractsRepository,
            NotificationRepository notificationRepository,
            ProductRepository productRepository) {
        this.docuSealConfig = docuSealConfig;
        this.docuSealRestTemplate = docuSealRestTemplate;
        this.contractsRepository = contractsRepository;
        this.notificationRepository = notificationRepository;
        this.productRepository = productRepository;
    }

    /**
     * Tạo hợp đồng đăng bán sản phẩm
     * Flow: Seller ký với Hệ thống sau khi xe đạt kiểm định
     */
    @Override
    @Transactional
    public Contracts createProductListingContract(Product product, User seller, User manager) {
        log.info("Creating product listing contract for product: {}, seller: {}",
                product.getProductid(), seller.getUserid());

        try {
            log.info("Step 1: Building submission request");
            // 1. Tạo submission request
            DocuSealSubmissionRequest request = buildProductListingRequest(product, seller);
            log.info("Request built. Template ID: {}", request.getTemplate_id());

            log.info("Step 2: Calling DocuSeal API");
            // 2. Gọi DocuSeal API
            DocuSealSubmissionResponse response = createSubmission(request);
            log.info("DocuSeal API response received. Slug: {}", response.getSlug());

            log.info("Step 3: Creating Contract entity");
            // 3. Tạo Contract entity và lưu DB
            Contracts contract = new Contracts();
            contract.setSignedat(new Date());
            contract.setOrders(null); // Hợp đồng đăng bán không liên kết order
            contract.setProducts(product); // Link product vào contract
            contract.setBuyers(null);
            contract.setSellers(seller);
            contract.setAdmins(manager);
            contract.setContractType("PRODUCT_LISTING");
            contract.setSignedMethod("DOCUSEAL");
            contract.setStatus(false); // Chờ ký

            // Lưu thông tin DocuSeal
            contract.setDocusealSubmissionId(response.getSlug());
            contract.setStartDate(new Date());

            log.info("Step 4: Saving contract to DB");
            contractsRepository.save(contract);
            log.info("Contract saved. ID: {}", contract.getContractid());

            log.info("Step 5: Creating notification for seller");
            // 4. Tạo notification cho seller
            String signingUrl = getSigningUrlForSeller(response);

            String notificationTitle = "Hợp đồng đăng bán đã sẵn sàng";
            String notificationMessage = String.format(
                "Vui lòng ký hợp đồng điện tử để hoàn tất việc đăng bán xe %s. " +
                "Link ký: %s",
                product.getProductname(),
                signingUrl
            );

            createNotification(seller, notificationTitle, notificationMessage);

            log.info("Product listing contract created successfully. Submission ID: {}, Signing URL: {}",
                    response.getSlug(), signingUrl);
            return contract;

        } catch (Exception e) {
            log.error("Error creating product listing contract. Error type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Không thể tạo hợp đồng đăng bán: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo hợp đồng mua bán
     * Flow: Buyer và Seller cùng ký khi buyer đặt cọc 10%
     */
    @Override
    @Transactional
    public Contracts createSaleTransactionContract(Orders order, User buyer, User seller, String transactionLocation) {
        log.info("Creating sale transaction contract for order: {}, buyer: {}, seller: {}",
                order.getOrderid(), buyer.getUserid(), seller.getUserid());

        try {
            // 1. Tạo submission request
            DocuSealSubmissionRequest request = buildSaleTransactionRequest(order, buyer, seller, transactionLocation);

            // 2. Gọi DocuSeal API
            DocuSealSubmissionResponse response = createSubmission(request);

            // 3. Tạo Contract entity
            Contracts contract = new Contracts();
            contract.setSignedat(new Date());
            contract.setOrders(order);
            contract.setBuyers(buyer);
            contract.setSellers(seller);
            contract.setAdmins(null); // Hợp đồng buyer-seller không cần manager
            contract.setContractType("SALE_TRANSACTION");
            contract.setSignedMethod("DOCUSEAL");
            contract.setStatus(false); // Chờ ký
            contract.setDocusealSubmissionId(response.getSlug());
            contract.setStartDate(new Date());

            contractsRepository.save(contract);

            // 4. Tạo notification cho cả buyer và seller
            String orderInfo = "Đơn hàng #" + order.getOrderid() + " - Tổng tiền: " + order.getTotalfinal() + " VNĐ";

            String buyerSigningUrl = getSigningUrlForBuyer(response);
            String buyerTitle = "Hợp đồng mua xe đã sẵn sàng";
            String buyerMessage = String.format(
                "Vui lòng ký hợp đồng mua xe. %s. Link ký: %s",
                orderInfo,
                buyerSigningUrl
            );

            String sellerSigningUrl = getSigningUrlForSeller(response);
            String sellerTitle = "Hợp đồng bán xe đã sẵn sàng";
            String sellerMessage = String.format(
                "Vui lòng ký hợp đồng bán xe. %s. Link ký: %s",
                orderInfo,
                sellerSigningUrl
            );

            createNotification(buyer, buyerTitle, buyerMessage);
            createNotification(seller, sellerTitle, sellerMessage);

            log.info("Sale transaction contract created successfully. Submission ID: {}", response.getSlug());
            return contract;

        } catch (Exception e) {
            log.error("Error creating sale transaction contract", e);
            throw new RuntimeException("Không thể tạo hợp đồng mua bán: " + e.getMessage(), e);
        }
    }

    //Lấy thông tin submission từ DocuSeal
    @Override
    public DocuSealSubmissionResponse getSubmission(String submissionId) {
        log.info("Getting submission: {}", submissionId);

        String url = docuSealConfig.getApi().getBaseUrl() + "/api/submissions/" + submissionId;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<DocuSealSubmissionResponse> response = docuSealRestTemplate.exchange(
                    url, HttpMethod.GET, entity, DocuSealSubmissionResponse.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error getting submission: {}", submissionId, e);
            throw new RuntimeException("Không thể lấy thông tin submission: " + e.getMessage(), e);
        }
    }

    //Xử lý webhook callback từ DocuSeal
    @Override
    @Transactional
    public void handleWebhook(DocuSealWebhookPayload payload) {
        log.info("Handling DocuSeal webhook. Event: {}, Submission ID: {}",
                payload.getEventType(), payload.getData().getSlug());

        String eventType = payload.getEventType();
        DocuSealWebhookPayload.SubmissionData data = payload.getData();

        // Tìm contract trong DB
        Contracts contract = contractsRepository.findByDocusealSubmissionId(data.getSlug());
        if (contract == null) {
            log.warn("Contract not found for submission ID: {}", data.getSlug());
            return;
        }

        switch (eventType) {
            case "submission.completed":
                handleSubmissionCompleted(contract, data);
                break;

            case "submitter.completed":
                handleSubmitterCompleted(contract, data);
                break;

            case "submitter.declined":
                handleSubmitterDeclined(contract, data);
                break;

            default:
                log.info("Unhandled event type: {}", eventType);
        }
    }

    //Xử lý khi submission hoàn tất (tất cả người ký đã ký xong)
    private void handleSubmissionCompleted(Contracts contract, DocuSealWebhookPayload.SubmissionData data) {
        log.info("Submission completed for contract: {}", contract.getContractid());

        // Cập nhật contract
        contract.setStatus(true); // Đã ký xong
        contract.setEndDate(data.getCompletedAt());

        // Lưu URL document đã ký
        if (data.getDocuments() != null && !data.getDocuments().isEmpty()) {
            contract.setDocusealDocumentUrl(data.getDocuments().get(0).getUrl());
        }

        contractsRepository.save(contract);

        // Xử lý theo loại hợp đồng
        if ("PRODUCT_LISTING".equals(contract.getContractType())) {
            handleProductListingCompleted(contract);
        } else if ("SALE_TRANSACTION".equals(contract.getContractType())) {
            handleSaleTransactionCompleted(contract);
        }
    }

    //Xử lý khi hoàn tất hợp đồng đăng bán
    private void handleProductListingCompleted(Contracts contract) {
        User seller = contract.getSellers();
        Product product = contract.getProducts();

        // Đưa xe vào kho và chuyển status sang DANG_BAN
        if (product != null) {
            product.setStatus(ProductStatus.DANG_BAN);
            product.setInWarehouse(true);
            product.setUpdatedat(new Date());
            productRepository.save(product);

            log.info("Product {} has been added to warehouse and status changed to DANG_BAN", product.getProductid());
        }

        // Tạo notification cho seller
        createNotification(seller,
                "Hợp đồng đăng bán đã hoàn tất",
                "Hợp đồng đăng bán đã được ký thành công. Sản phẩm " + (product != null ? product.getProductname() : "") + " đã được đưa vào kho và hiển thị trên nền tảng.");

        log.info("Product listing contract completed for seller: {}", seller.getUserid());
    }

    //Xử lý khi hoàn tất hợp đồng mua bán
    private void handleSaleTransactionCompleted(Contracts contract) {
        Orders order = contract.getOrders();
        User buyer = contract.getBuyers();
        User seller = contract.getSellers();

        // Cập nhật trạng thái đơn hàng sang "Đã ký hợp đồng"
        order.setStatus(OrderStatus.CHO_DUYET);

        // Tạo notification cho buyer
        createNotification(buyer,
                "Hợp đồng mua xe đã hoàn tất",
                "Hợp đồng mua xe đã được ký bởi cả hai bên. Đơn hàng đang chờ Manager duyệt giao dịch.");

        // Tạo notification cho seller
        createNotification(seller,
                "Hợp đồng bán xe đã hoàn tất",
                "Hợp đồng bán xe đã được ký bởi cả hai bên. Vui lòng chờ Manager duyệt giao dịch.");

        log.info("Sale transaction contract completed for order: {}", order.getOrderid());
    }

    //Xử lý khi một người ký hoàn tất phần của mình
    private void handleSubmitterCompleted(Contracts contract, DocuSealWebhookPayload.SubmissionData data) {
        log.info("Submitter completed for contract: {}", contract.getContractid());

        // Tìm submitter vừa ký
        Optional<DocuSealWebhookPayload.SubmitterInfo> completedSubmitter = data.getSubmitters().stream()
                .filter(s -> "completed".equals(s.getStatus()))
                .findFirst();

        if (completedSubmitter.isPresent()) {
            DocuSealWebhookPayload.SubmitterInfo submitter = completedSubmitter.get();

            // Cập nhật thời gian ký của seller (nếu là seller)
            if ("Seller".equalsIgnoreCase(submitter.getRole())) {
                contract.setSellerSignedAt(submitter.getCompletedAt());
                contractsRepository.save(contract);

                createNotification(contract.getSellers(),
                        "Bạn đã ký hợp đồng thành công",
                        "Hợp đồng đang chờ bên còn lại ký.");
            }

            // Cập nhật thời gian ký của buyer (nếu là buyer)
            if ("Buyer".equalsIgnoreCase(submitter.getRole())) {
                contract.setSignedbyBuyer(submitter.getCompletedAt());
                contractsRepository.save(contract);

                createNotification(contract.getBuyers(),
                        "Bạn đã ký hợp đồng thành công",
                        "Hợp đồng đang chờ bên còn lại ký.");
            }
        }
    }

    //Xử lý khi người ký từ chối
    @SuppressWarnings("unused")
    private void handleSubmitterDeclined(Contracts contract, DocuSealWebhookPayload.SubmissionData data) {
        log.warn("Submitter declined for contract: {}", contract.getContractid());

        contract.setStatus(false);
        contractsRepository.save(contract);

        // Thông báo cho các bên liên quan
        if (contract.getSellers() != null) {
            createNotification(contract.getSellers(),
                    "Hợp đồng bị từ chối",
                    "Hợp đồng đã bị một bên từ chối. Vui lòng liên hệ hỗ trợ.");
        }

        if (contract.getBuyers() != null) {
            createNotification(contract.getBuyers(),
                    "Hợp đồng bị từ chối",
                    "Hợp đồng đã bị một bên từ chối. Vui lòng liên hệ hỗ trợ.");
        }
    }

    //Tải PDF đã ký
    @Override
    public byte[] downloadSignedDocument(String documentUrl) {
        log.info("Downloading signed document from: {}", documentUrl);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = docuSealRestTemplate.exchange(
                    documentUrl, HttpMethod.GET, entity, byte[].class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error downloading signed document", e);
            throw new RuntimeException("Không thể tải document: " + e.getMessage(), e);
        }
    }

    //Kiểm tra submission đã hoàn tất chưa
    @Override
    public boolean isSubmissionCompleted(String submissionId) {
        try {
            DocuSealSubmissionResponse response = getSubmission(submissionId);
            return "completed".equalsIgnoreCase(response.getStatus());
        } catch (Exception e) {
            log.error("Error checking submission status", e);
            return false;
        }
    }

    //Gửi lại email nhắc nhở
    @Override
    public void resendSigningEmail(String submissionId, String submitterEmail) {
        log.info("Resending signing email for submission: {}, email: {}", submissionId, submitterEmail);

        String url = docuSealConfig.getApi().getBaseUrl() + "/api/submissions/" + submissionId + "/send_email";

        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("submitter_email", submitterEmail);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            docuSealRestTemplate.postForEntity(url, entity, String.class);

            log.info("Signing email resent successfully");

        } catch (Exception e) {
            log.error("Error resending signing email", e);
            throw new RuntimeException("Không thể gửi lại email: " + e.getMessage(), e);
        }
    }

    // ========================= PRIVATE HELPER METHODS ================================================================

    //Tạo submission request cho hợp đồng đăng bán
    private DocuSealSubmissionRequest buildProductListingRequest(Product product, User seller) {
        // Format phone number với country code
        String sellerPhone = formatPhoneNumber(seller.getPhone());

        // Lấy thông tin xe
        String productName = product.getProductname() != null ? product.getProductname() : "N/A";
        String productPrice = String.format("%,.0f VNĐ", product.getCost());
        // Dùng description thay vì specs (Free plan không có Textarea)
        String productDescription = product.getDescription() != null ? product.getDescription() : "N/A";
        String productType = product.getType() != null ? product.getType() : "N/A";

        // Thông tin seller
        String sellerName = seller.getDisplayname() != null ? seller.getDisplayname() : seller.getUsername();
        String sellerEmail = seller.getEmail() != null ? seller.getEmail() : "N/A";
        String sellerPhone_display = seller.getPhone() != null ? seller.getPhone() : "N/A";

        // Tạo fields data để điền sẵn vào document
        List<DocuSealSubmissionRequest.Field> fields = List.of(
            DocuSealSubmissionRequest.Field.builder()
                .name("product_name")
                .default_value(productName)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("product_price")
                .default_value(productPrice)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("product_description")  // Đổi từ product_specs
                .default_value(productDescription)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("product_type")
                .default_value(productType)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("seller_name")
                .default_value(sellerName)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("seller_email")
                .default_value(sellerEmail)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("seller_phone")
                .default_value(sellerPhone_display)
                .build(),
            // NOTE: Không gửi manager_name vì template không có field này
            // Manager chỉ duyệt, không ký hợp đồng
            DocuSealSubmissionRequest.Field.builder()
                .name("contract_date")
                .default_value(new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()))
                .build()
        );

        // Tạo submitter cho Seller với fields data
        DocuSealSubmissionRequest.Submitter sellerSubmitter = DocuSealSubmissionRequest.Submitter.builder()
                .role("Seller")
                .email(seller.getEmail())
                .name(sellerName)
                .phone(sellerPhone)
                .fields(fields)
                .build();

        // Tạo submission request
        return DocuSealSubmissionRequest.builder()
                .template_id(docuSealConfig.getTemplate().getProductListing())
                .submitters(List.of(sellerSubmitter))
                .webhook_url(docuSealConfig.getWebhook().getUrl())
                .send_email(true)
                .send_sms(false)
                .build();
    }

    //Tạo submission request cho hợp đồng mua bán
    private DocuSealSubmissionRequest buildSaleTransactionRequest(Orders order, User buyer, User seller, String transactionLocation) {
        // Format phone number với country code
        String buyerPhone = formatPhoneNumber(buyer.getPhone());
        String sellerPhone = formatPhoneNumber(seller.getPhone());

        // Lấy thông tin order
        String orderInfo = "Đơn hàng #" + order.getOrderid();
        String totalAmount = String.format("%,.0f VNĐ", order.getTotalfinal());
        String depositAmount = String.format("%,.0f VNĐ", order.getTotalfinal() * 0.1);
        String finalPayment = String.format("%,.0f VNĐ", order.getTotalfinal() * 0.9);

        // Thông tin buyer
        String buyerName = buyer.getDisplayname() != null ? buyer.getDisplayname() : buyer.getUsername();
        String buyerEmail = buyer.getEmail() != null ? buyer.getEmail() : "N/A";
        String buyerPhone_display = buyer.getPhone() != null ? buyer.getPhone() : "N/A";

        // Thông tin seller
        String sellerName = seller.getDisplayname() != null ? seller.getDisplayname() : seller.getUsername();
        String sellerEmail = seller.getEmail() != null ? seller.getEmail() : "N/A";
        String sellerPhone_display = seller.getPhone() != null ? seller.getPhone() : "N/A";

        // Địa điểm giao dịch
        String location = transactionLocation != null ? transactionLocation : "Theo thỏa thuận";

        // Tạo fields data chung cho cả buyer và seller
        List<DocuSealSubmissionRequest.Field> commonFields = List.of(
            DocuSealSubmissionRequest.Field.builder()
                .name("order_id")
                .default_value(orderInfo)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("total_amount")
                .default_value(totalAmount)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("deposit_amount")
                .default_value(depositAmount)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("final_payment")
                .default_value(finalPayment)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("buyer_name")
                .default_value(buyerName)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("buyer_email")
                .default_value(buyerEmail)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("buyer_phone")
                .default_value(buyerPhone_display)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("seller_name")
                .default_value(sellerName)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("seller_email")
                .default_value(sellerEmail)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("seller_phone")
                .default_value(sellerPhone_display)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("transaction_location")
                .default_value(location)
                .build(),
            DocuSealSubmissionRequest.Field.builder()
                .name("contract_date")
                .default_value(new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()))
                .build()
        );

        // Tạo submitter cho Buyer
        DocuSealSubmissionRequest.Submitter buyerSubmitter = DocuSealSubmissionRequest.Submitter.builder()
                .role("Buyer")
                .email(buyer.getEmail())
                .name(buyerName)
                .phone(buyerPhone)
                .fields(commonFields)
                .build();

        // Tạo submitter cho Seller
        DocuSealSubmissionRequest.Submitter sellerSubmitter = DocuSealSubmissionRequest.Submitter.builder()
                .role("Seller")
                .email(seller.getEmail())
                .name(sellerName)
                .phone(sellerPhone)
                .fields(commonFields)
                .build();

        // Tạo submission request
        return DocuSealSubmissionRequest.builder()
                .template_id(docuSealConfig.getTemplate().getSaleTransaction())
                .submitters(List.of(buyerSubmitter, sellerSubmitter))
                .webhook_url(docuSealConfig.getWebhook().getUrl())
                .send_email(true)
                .send_sms(false)
                .build();
    }


    //Gọi DocuSeal API để tạo submission
    private DocuSealSubmissionResponse createSubmission(DocuSealSubmissionRequest request) {
        String url = docuSealConfig.getApi().getBaseUrl() + "/api/submissions";
        log.info("Calling DocuSeal API: {}", url);

        // Debug logging
        log.info("=== DOCUSEAL REQUEST DEBUG ===");
        log.info("Template ID: {}", request.getTemplate_id());
        log.info("Submitters count: {}", request.getSubmitters() != null ? request.getSubmitters().size() : 0);

        // Log từng submitter và fields
        if (request.getSubmitters() != null) {
            for (DocuSealSubmissionRequest.Submitter sub : request.getSubmitters()) {
                log.info("Submitter: role={}, email={}, name={}",
                        sub.getRole(), sub.getEmail(), sub.getName());

                if (sub.getFields() != null && !sub.getFields().isEmpty()) {
                    log.info("  Fields count: {}", sub.getFields().size());
                    for (DocuSealSubmissionRequest.Field f : sub.getFields()) {
                        log.info("    - Field: name='{}', value='{}'",
                                f.getName(),
                                f.getDefault_value() != null && f.getDefault_value().length() > 50
                                    ? f.getDefault_value().substring(0, 50) + "..."
                                    : f.getDefault_value());
                    }
                } else {
                    log.warn("  ⚠️ No fields data for this submitter!");
                }
            }
        }
        log.info("=============================");

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<DocuSealSubmissionRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("Sending POST request to DocuSeal...");
            // DocuSeal API trả về Array of submissions
            ResponseEntity<DocuSealSubmissionResponse[]> response = docuSealRestTemplate.postForEntity(
                    url, entity, DocuSealSubmissionResponse[].class);

            log.info("DocuSeal API responded with status: {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.CREATED) {
                throw new RuntimeException("DocuSeal API returned status: " + response.getStatusCode());
            }

            DocuSealSubmissionResponse[] submissions = response.getBody();
            if (submissions == null || submissions.length == 0) {
                log.error("DocuSeal API returned empty response");
                throw new RuntimeException("DocuSeal API returned empty response");
            }

            // Lấy submission đầu tiên (vì chỉ tạo 1 submission)
            DocuSealSubmissionResponse result = submissions[0];

            // Debug logging response
            log.info("=== DOCUSEAL RESPONSE DEBUG ===");
            log.info("Submission Slug: {}", result.getSlug());
            log.info("Status: {}", result.getStatus());
            log.info("Submission URL: {}", result.getSubmissionUrl());
            log.info("Submitters count: {}",
                    result.getSubmitters() != null ? result.getSubmitters().size() : 0);

            if (result.getSubmitters() != null && !result.getSubmitters().isEmpty()) {
                for (DocuSealSubmissionResponse.SubmitterDetail sub : result.getSubmitters()) {
                    log.info("  Submitter: role={}, email={}, status={}, url={}",
                            sub.getRole(), sub.getEmail(), sub.getStatus(), sub.getUrl());
                }
            } else {
                log.warn("⚠️ Response submitters array is empty - URLs will use fallback");
            }
            log.info("===============================");

            return result;

        } catch (Exception e) {
            log.error("Error calling DocuSeal API. Error type: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Lỗi khi gọi DocuSeal API: " + e.getMessage(), e);
        }
    }

    //Tạo HTTP headers với API key
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", docuSealConfig.getApi().getKey());
        headers.set("Accept", "application/json");
        return headers;
    }

    //Lấy signing URL cho seller
    private String getSigningUrlForSeller(DocuSealSubmissionResponse response) {
        if (response == null) {
            log.warn("Response is null, cannot get signing URL for seller");
            return "N/A";
        }

        log.info("Getting signing URL for seller. Submission ID: {}, Submitters count: {}",
                response.getSlug(),
                response.getSubmitters() != null ? response.getSubmitters().size() : 0);

        // Nếu có submitters, tìm seller
        if (response.getSubmitters() != null && !response.getSubmitters().isEmpty()) {
            String sellerUrl = response.getSubmitters().stream()
                    .filter(s -> s != null && "Seller".equalsIgnoreCase(s.getRole()))
                    .findFirst()
                    .map(DocuSealSubmissionResponse.SubmitterDetail::getUrl)
                    .orElse(null);

            if (sellerUrl != null && !sellerUrl.isEmpty()) {
                log.info("Found seller signing URL: {}", sellerUrl);
                return sellerUrl;
            }
        }

        // Fallback: Sử dụng submission URL chung
        if (response.getSubmissionUrl() != null && !response.getSubmissionUrl().isEmpty()) {
            log.info("Using submission URL as fallback: {}", response.getSubmissionUrl());
            return response.getSubmissionUrl();
        }

        log.warn("No signing URL found for submission: {}", response.getSlug());
        return "https://docuseal.com/s/" + response.getSlug();
    }

    //Lấy signing URL cho buyer
    private String getSigningUrlForBuyer(DocuSealSubmissionResponse response) {
        if (response == null) {
            log.warn("Response is null, cannot get signing URL for buyer");
            return "N/A";
        }

        log.info("Getting signing URL for buyer. Submission ID: {}, Submitters count: {}",
                response.getSlug(),
                response.getSubmitters() != null ? response.getSubmitters().size() : 0);

        // Nếu có submitters, tìm buyer
        if (response.getSubmitters() != null && !response.getSubmitters().isEmpty()) {
            String buyerUrl = response.getSubmitters().stream()
                    .filter(s -> s != null && "Buyer".equalsIgnoreCase(s.getRole()))
                    .findFirst()
                    .map(DocuSealSubmissionResponse.SubmitterDetail::getUrl)
                    .orElse(null);

            if (buyerUrl != null && !buyerUrl.isEmpty()) {
                log.info("Found buyer signing URL: {}", buyerUrl);
                return buyerUrl;
            }
        }

        // Fallback: Sử dụng submission URL chung
        if (response.getSubmissionUrl() != null && !response.getSubmissionUrl().isEmpty()) {
            log.info("Using submission URL as fallback: {}", response.getSubmissionUrl());
            return response.getSubmissionUrl();
        }

        log.warn("No signing URL found for submission: {}", response.getSlug());
        return "https://docuseal.com/s/" + response.getSlug();
    }

    //Tạo notification
    private void createNotification(User user, String title, String description) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());
        notification.setUsers(user);
        notificationRepository.save(notification);
    }

    //Format phone number theo chuẩn quốc tế (VD: +84xxxxxxxxx)
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "+84000000000"; // Default nếu không có phone
        }

        // Loại bỏ tất cả ký tự không phải số
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        // Nếu bắt đầu bằng 0, thay bằng +84
        if (cleanPhone.startsWith("0")) {
            return "+84" + cleanPhone.substring(1);
        }

        // Nếu bắt đầu bằng 84, thêm +
        if (cleanPhone.startsWith("84")) {
            return "+" + cleanPhone;
        }

        // Nếu đã có +, giữ nguyên
        if (phone.startsWith("+")) {
            return phone.replaceAll("[^0-9+]", "");
        }

        // Mặc định thêm +84 cho số Việt Nam
        return "+84" + cleanPhone;
    }
}