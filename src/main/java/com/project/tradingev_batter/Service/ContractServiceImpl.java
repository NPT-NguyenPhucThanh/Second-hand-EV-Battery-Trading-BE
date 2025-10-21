package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import com.project.tradingev_batter.enums.ProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DocuSealService docuSealService;
    private final NotificationService notificationService;

    public ContractServiceImpl(ContractRepository contractRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              DocuSealService docuSealService,
                              NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.docuSealService = docuSealService;
        this.notificationService = notificationService;
    }

    // TẠO CONTRACT SAU KHI KIỂM ĐỊNH PASS
    // Manager gọi method này sau khi nhập kết quả kiểm định PASS
    @Override
    @Transactional
    public Contracts createContractAfterInspection(Long productId, Long sellerId) {
        // Lấy thông tin product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validate product đã pass kiểm định
        if (!ProductStatus.DA_DUYET.equals(product.getStatus())) {
            throw new RuntimeException("Product chưa được duyệt kiểm định");
        }

        // Lấy thông tin seller
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // Tạo contract mới sử dụng DocuSealService
        // NOTE: DocuSealService.createProductListingContract sẽ tạo và lưu contract vào DB
        try {
            Contracts contract = docuSealService.createProductListingContract(product, seller, null);

            // Tạo notification cho seller
            notificationService.createNotification(
                seller.getUserid(),
                "Hợp đồng đăng bán đã sẵn sàng",
                "Xe " + product.getProductname() + " đã đạt kiểm định. Vui lòng ký hợp đồng để đưa xe vào kho."
            );

            return contract;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo hợp đồng DocuSeal: " + e.getMessage());
        }
    }

    // LẤY DANH SÁCH CONTRACT PENDING CỦA SELLER
    @Override
    public List<Contracts> getPendingContracts(Long sellerId) {
        return contractRepository.findBySellers_UseridAndStatusFalseOrderBySignedatDesc(sellerId);
    }

    // SELLER KÝ CONTRACT
    @Override
    @Transactional
    public Contracts signContract(Long contractId, Long sellerId) {
        Contracts contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Validate seller ownership
        if (!contract.getSellers().getUserid().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền ký hợp đồng này");
        }

        // Validate contract chưa ký
        if (contract.isStatus()) {
            throw new RuntimeException("Hợp đồng đã được ký rồi");
        }

        // Trả về contract với docusealSubmissionId để frontend có thể lấy signing URL
        // FE gọi DocuSeal API trực tiếp để lấy URL ký
        return contract;
    }

    // XỬ LÝ DOCUSEAL CALLBACK
    // Được gọi khi DocuSeal webhook callback về
    @Override
    @Transactional
    public Contracts handleDocuSealCallback(String submissionId, String status) {
        Contracts contract = contractRepository.findByDocusealSubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Contract not found with submissionId: " + submissionId));

        if ("completed".equals(status)) {
            // Seller đã ký xong
            contract.setStatus(true); // Đã ký
            contract.setSellerSignedAt(new Date());
            contract.setSignedbySeller(new Date());

            // Lấy thông tin submission từ DocuSeal để có document URL
            try {
                var submission = docuSealService.getSubmission(submissionId);
                if (submission != null && submission.getDocuments() != null && !submission.getDocuments().isEmpty()) {
                    String documentUrl = submission.getDocuments().get(0).getUrl();
                    contract.setDocusealDocumentUrl(documentUrl);
                    contract.setContractFile(documentUrl);
                }
            } catch (Exception e) {
                System.err.println("Không thể lấy document URL: " + e.getMessage());
            }

            contractRepository.save(contract);

            // TODO: Đưa xe vào kho và chuyển status product sang DANG_BAN
            // Sẽ implement ở phần sau

            // Tạo notification cho seller
            notificationService.createNotification(
                contract.getSellers().getUserid(),
                "Ký hợp đồng thành công",
                "Hợp đồng đã được ký. Xe sẽ được đưa vào kho và hiển thị trên nền tảng."
            );
        }

        return contract;
    }

    @Override
    public Contracts getContractByOrderId(Long orderId) {
        return contractRepository.findByOrders_Orderid(orderId)
                .orElse(null);
    }

    @Override
    public List<Contracts> getSellerContracts(Long sellerId) {
        return contractRepository.findBySellers_UseridOrderBySignedatDesc(sellerId);
    }
}

