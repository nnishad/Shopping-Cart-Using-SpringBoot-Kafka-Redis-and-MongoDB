package com.reljicd.service.impl;

import com.reljicd.exception.NotEnoughProductsInStockException;
import com.reljicd.kafka.Producer;
import com.reljicd.model.Product;
import com.reljicd.repository.ProductRepository;
import com.reljicd.service.ShoppingCartService;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shopping Cart is implemented with a Map, and as a session bean
 *
 * @author Dusan
 */
@Service
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Transactional
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ProductRepository productRepository;
	private static final Logger logger=LoggerFactory.getLogger(Producer.class);


    private Map<Product, Integer> products = new HashMap<>();
    @Autowired
    private Producer producer1;

    @Autowired
    public ShoppingCartServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * If product is in the map just increment quantity by 1.
     * If product is not in the map with, add it with quantity 1
     *
     * @param product
     */
    @Override
    public void addProduct(Product product) {
        if (products.containsKey(product)) {
            products.replace(product, products.get(product) + 1);
        } else {
            products.put(product, 1);
        }
        
        String jsonProduct=newProductToJSON(product);
       
        System.out.println(jsonProduct);
       producer1.sendKafkaMessage(jsonProduct, "cart1");
    }

    private String newProductToJSON(Product product) {
    	 JSONObject jsonObject = new JSONObject();
         JSONObject nestedJsonObject = new JSONObject();

         try {
             /*
             Adding some random data into the JSON object.
              */
             jsonObject.put("index", "userID1");
             //jsonObject.put("", "The index is now: ");

             /*
             We're adding a field in the nested JSON object.
              */
             nestedJsonObject.put("Product Key", product.getId());
             nestedJsonObject.put("Product Name", product.getName());
             nestedJsonObject.put("Product Qty", product.getQuantity());
             nestedJsonObject.put("Product Price", product.getPrice());

             /*
             Adding the nexted JSON object to the main JSON object.
              */
             jsonObject.put("Cart Details", nestedJsonObject);

         } catch (JSONException e) {
             logger.error(e.getMessage());
         }
		return jsonObject.toString();
	}

	/**
     * If product is in the map with quantity > 1, just decrement quantity by 1.
     * If product is in the map with quantity 1, remove it from map
     *
     * @param product
     */
    @Override
    public void removeProduct(Product product) {
        if (products.containsKey(product)) {
            if (products.get(product) > 1)
                products.replace(product, products.get(product) - 1);
            else if (products.get(product) == 1) {
                products.remove(product);
            }
        }
        Map<Product, Integer> unmodifiableMap = Collections.unmodifiableMap(products);
        //producer1.sendKafkaMessage(unmodifiableMap.toString(), "cart1");
        unmodifiableMap.entrySet().forEach(entry->{
            System.out.print(entry.getKey().getName() + "removeX" + entry.getValue());  
         });
    }

    /**
     * @return unmodifiable copy of the map
     */
    @Override
    public Map<Product, Integer> getProductsInCart() {
        Map<Product, Integer> unmodifiableMap = Collections.unmodifiableMap(products);
        //producer1.sendKafkaMessage(unmodifiableMap.toString(), "cart1");
       
		return unmodifiableMap;
    }

    /**
     * Checkout will rollback if there is not enough of some product in stock
     *
     * @throws NotEnoughProductsInStockException
     */
    @Override
    public void checkout() throws NotEnoughProductsInStockException {
        Product product;
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            // Refresh quantity for every product before checking
            product = productRepository.findOne(entry.getKey().getId());
            if (product.getQuantity() < entry.getValue())
                throw new NotEnoughProductsInStockException(product);
            entry.getKey().setQuantity(product.getQuantity() - entry.getValue());
        }
        productRepository.save(products.keySet());
        productRepository.flush();
        products.clear();
    }

    @Override
    public BigDecimal getTotal() {
        return products.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }
}
