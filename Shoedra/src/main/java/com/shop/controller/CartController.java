package com.shop.controller;

import com.shop.dto.CartDetailDto;
import com.shop.dto.CartItemDto;
import com.shop.dto.CartOrderDto;
import com.shop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 장바구니에 담기 */
    @PostMapping("/cart")
    @ResponseBody
    public ResponseEntity<?> addCartItem(@RequestBody @Valid CartItemDto cartItemDto,
                                         BindingResult bindingResult,
                                         Principal principal) {
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                sb.append(error.getDefaultMessage());
            }
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        try {
            String email = principal.getName();
            Long cartItemId = cartService.addCart(cartItemDto, email);
            return new ResponseEntity<>(cartItemId, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("장바구니 추가 실패: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /** 장바구니 페이지 */
    @GetMapping("/cart")
    public String cartPage(Principal principal, Model model) {
        List<CartDetailDto> cartItems = cartService.getCartList(principal.getName());
        model.addAttribute("cartItems", cartItems);
        return "cart/cartList";
    }

    /** 장바구니 수량 변경 */
    @PatchMapping("/cartItem/{cartItemId}")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(@PathVariable Long cartItemId,
                                            @RequestParam int count,
                                            Principal principal) {
        try {
            cartService.updateCartItemCount(cartItemId, count, principal.getName());
            return new ResponseEntity<>(cartItemId, HttpStatus.OK);
        } catch (SecurityException e) {
            return new ResponseEntity<>("수정 권한이 없습니다.", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>("수정 실패: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /** 장바구니 아이템 삭제 */
    @DeleteMapping("/cartItem/{cartItemId}")
    @ResponseBody
    public ResponseEntity<?> deleteCartItem(@PathVariable Long cartItemId,
                                            Principal principal) {
        try {
            cartService.deleteCartItem(cartItemId, principal.getName());
            return new ResponseEntity<>(cartItemId, HttpStatus.OK);
        } catch (SecurityException e) {
            return new ResponseEntity<>("삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>("삭제 실패: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /** ✅ 결제 후 주문 처리 */
    @PostMapping("/cart/orders")
    @ResponseBody
    public ResponseEntity<?> orderCartItems(@RequestBody List<CartOrderDto> cartOrderDtoList,
                                            Principal principal) {
        if (cartOrderDtoList == null || cartOrderDtoList.isEmpty()) {
            return new ResponseEntity<>("주문할 상품이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            String email = principal.getName();
            Long orderId = cartService.orderCartItem(cartOrderDtoList, email); // ✅ 전체 처리

            return new ResponseEntity<>("주문 완료! 주문번호: " + orderId, HttpStatus.OK);
        } catch (IllegalStateException ise) {
            return new ResponseEntity<>("결제 검증 실패: " + ise.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("주문 처리 중 오류: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
