package com.nico.store.store.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nico.store.store.domain.Article;
import com.nico.store.store.domain.CartItem;
import com.nico.store.store.domain.Order;
import com.nico.store.store.domain.Payment;
import com.nico.store.store.domain.Shipping;
import com.nico.store.store.domain.ShoppingCart;
import com.nico.store.store.domain.User;
import com.nico.store.store.repository.ArticleRepository;
import com.nico.store.store.repository.CartItemRepository;
import com.nico.store.store.repository.OrderRepository;
import com.nico.store.store.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	OrderRepository orderRepository;
	
	@Autowired
	CartItemRepository cartItemRepository;
	
	@Autowired
	ArticleRepository articleRepository;
			
	@Override
	@Transactional
	@CacheEvict(value = "itemcount", allEntries = true)
	public synchronized Order createOrder(ShoppingCart shoppingCart, Shipping shipping, Payment payment, User user) {
		
		// Gán thông tin người dùng, thanh toán và giao hàng
		Order order = new Order();
		order.setUser(user);
		order.setPayment(payment);
		order.setShipping(shipping);
		
		// Thiết lập tổng số tiền của đơn hàng
		order.setOrderTotal(shoppingCart.getGrandTotal());
		
		// Thiết lập mối quan hệ hai chiều giữa Order và Shipping, Payment
		shipping.setOrder(order);
		payment.setOrder(order);
		
		// Thiết lập ngày đặt hàng và ngày giao hàng ước tính
		LocalDate today = LocalDate.now();
		LocalDate estimatedDeliveryDate = today.plusDays(5);				
		order.setOrderDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		order.setShippingDate(Date.from(estimatedDeliveryDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		
		// Đặt trạng thái ban đầu của đơn đặt hàng
		order.setOrderStatus("In Progress");
		
		// Lưu đơn hàng vào csdl
		order = orderRepository.save(order);
		
		// Duyệt qua các mục trong giỏ hàng để cập nhật tồn kho và liên kết với Order
		
		// Lấy danh sách CartItem từ ShoppingCart: Mỗi CartItem đại diện cho một sản
		// phẩm mà người dùng đã chọn mua và số lượng sản phẩm đó.
		
		List<CartItem> cartItems = shoppingCart.getCartItems();
		
		// Duyệt qua từng CartItem
		for (CartItem item : cartItems) {
			
			// Giảm số lượng tồn kho của sản phẩm (Article)
			Article article = item.getArticle();
			article.decreaseStock(item.getQty());
			
			// Lưu Article sau khi cập nhật tồn kho
			articleRepository.save(article);
			
			//  thiết lập mối quan hệ giữa CartItem và Order để
			//  chỉ ra rằng CartItem thuộc về đơn hàng này.
			item.setOrder(order);
			cartItemRepository.save(item);
		}		
		return order;	
	}
	
	@Override
	public Order findOrderWithDetails(Long id) {
		return orderRepository.findEagerById(id);
	}	

	public List<Order> findByUser(User user) {
		return orderRepository.findByUser(user);
	}

}
