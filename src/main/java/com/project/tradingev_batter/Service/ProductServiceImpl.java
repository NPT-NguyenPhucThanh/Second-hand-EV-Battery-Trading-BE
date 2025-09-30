package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ProductServiceImpl implements  ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        product.setCreatedat(new Date());
        product.setStatus("CHO_DUYET");
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        Product product = getProductById(id);
        product.setProductname(updatedProduct.getProductname());
        product.setDescription(updatedProduct.getDescription());
        product.setCost(updatedProduct.getCost());
        // Update các field khác nếu cần
        product.setUpdatedat(new Date());
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
