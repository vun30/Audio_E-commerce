package org.example.audio_ecommerce;

import jakarta.mail.MessagingException;
import org.example.audio_ecommerce.config.CodConfig;
import org.example.audio_ecommerce.dto.request.CheckoutItemRequest;
import org.example.audio_ecommerce.dto.request.ForgotPasswordRequest;
import org.example.audio_ecommerce.dto.request.ResetPasswordRequest;
import org.example.audio_ecommerce.dto.request.UpdateCartItemQtyRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CodEligibilityResponse;
import org.example.audio_ecommerce.dto.response.CartResponse;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.security.JwtTokenProvider;
import org.example.audio_ecommerce.service.*;
import org.example.audio_ecommerce.service.Impl.AccountServiceImpl;
import org.example.audio_ecommerce.service.Impl.CartServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Service Suite - Smoke Test (Account + Cart)")
class AccountServiceImpl_SmokeTest {

    // =========================
    // AccountServiceImpl mocks
    // =========================
    private final AccountRepository repository = mock(AccountRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final StoreWalletRepository storeWalletRepository = mock(StoreWalletRepository.class);
    private final StoreWalletTransactionRepository storeWalletTransactionRepository = mock(StoreWalletTransactionRepository.class);
    private final StaffRepository staffRepository = mock(StaffRepository.class);
    private final EmailService emailService = mock(EmailService.class);

    private final AccountServiceImpl sut = new AccountServiceImpl(
            repository, storeRepository, customerRepository, passwordEncoder,
            authenticationManager, jwtTokenProvider, walletRepository,
            storeWalletRepository, storeWalletTransactionRepository,
            staffRepository, emailService
    );

    // =========================
    // CartServiceImpl mocks
    // =========================
    private final CartRepository cartRepo = mock(CartRepository.class);
    private final CartItemRepository cartItemRepo = mock(CartItemRepository.class);
    private final ProductRepository productRepo = mock(ProductRepository.class);
    private final ProductComboRepository comboRepo = mock(ProductComboRepository.class);
    private final CustomerRepository customerRepo = mock(CustomerRepository.class);
    private final CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
    private final StoreOrderRepository storeOrderRepo = mock(StoreOrderRepository.class);
    private final StoreRepository storeRepo = mock(StoreRepository.class);

    private final VoucherService voucherService = mock(VoucherService.class);
    private final GhnFeeService ghnFeeService = mock(GhnFeeService.class);
    private final ProductVariantRepository productVariantRepo = mock(ProductVariantRepository.class);
    private final OrderCodeGeneratorService orderCodeGeneratorService = mock(OrderCodeGeneratorService.class);
    private final PlatformCampaignProductRepository platformCampaignProductRepository = mock(PlatformCampaignProductRepository.class);
    private final NotificationCreatorService notificationCreatorService = mock(NotificationCreatorService.class);
    private final PlatformFeeRepository platformFeeRepository = mock(PlatformFeeRepository.class);
    private final PlatformCampaignProductUsageRepository platformCampaignProductUsageRepository = mock(PlatformCampaignProductUsageRepository.class);

    private final StoreWalletRepository storeWalletRepo2 = mock(StoreWalletRepository.class);
    private final CodConfig codConfig = mock(CodConfig.class);

    private final CartServiceImpl cartService = new CartServiceImpl(
            cartRepo,
            cartItemRepo,
            productRepo,
            comboRepo,
            customerRepo,
            customerOrderRepository,
            storeOrderRepo,
            storeRepo,
            voucherService,
            ghnFeeService,
            productVariantRepo,
            orderCodeGeneratorService,
            platformCampaignProductRepository,
            notificationCreatorService,
            platformFeeRepository,
            platformCampaignProductUsageRepository,
            storeWalletRepo2,
            codConfig
    );

    AccountServiceImpl_SmokeTest() {
        ReflectionTestUtils.setField(sut, "frontendUrl", "http://localhost:3000");
    }

    // ==========================================================
    // ===================== Account tests ======================
    // ==========================================================

    @DisplayName("forgotPassword() :: UTCID01/02/03")
    @ParameterizedTest(name = "forgotPassword() :: {0}")
    @MethodSource("forgotPasswordCases")
    void forgotPassword_test(String tcId,
                             ForgotPasswordRequest req,
                             Optional<Account> accOpt,
                             RuntimeException repoThrow,
                             HttpStatus expectedStatus,
                             Integer expectedCode) throws MessagingException {

        if (repoThrow != null) {
            when(repository.findByEmailIgnoreCase(req.getEmail())).thenThrow(repoThrow);
        } else {
            when(repository.findByEmailIgnoreCase(req.getEmail())).thenReturn(accOpt);
        }

        ResponseEntity<BaseResponse> res = sut.forgotPassword(req);

        assertEquals(expectedStatus, res.getStatusCode(), tcId);
        assertNotNull(res.getBody(), tcId);
        if (expectedCode != null) {
            assertEquals(expectedCode, extractCode(res.getBody()), tcId + ": wrong code");
        }

        if (tcId.startsWith("UTCID01")) {
            verify(repository, times(1)).save(any(Account.class));
            verify(emailService, times(1)).sendEmail(eq(EmailTemplateType.RESET_PASSWORD), any());
        }
    }

    static Stream<Arguments> forgotPasswordCases() {
        ForgotPasswordRequest ok = new ForgotPasswordRequest();
        ok.setEmail("a@test.com");

        Account acc = new Account();
        acc.setEmail("a@test.com");
        acc.setName("A");
        acc.setRole(RoleEnum.CUSTOMER);

        ForgotPasswordRequest notFound = new ForgotPasswordRequest();
        notFound.setEmail("missing@test.com");

        ForgotPasswordRequest boundary = new ForgotPasswordRequest();
        boundary.setEmail("a@test.com");

        return Stream.of(
                Arguments.of("UTCID01 - Normal", ok, Optional.of(acc), null, HttpStatus.OK, 200),
                Arguments.of("UTCID02 - Abnormal", notFound, Optional.empty(),
                        new UsernameNotFoundException("Email không tồn tại trong hệ thống"),
                        HttpStatus.NOT_FOUND, 404),
                Arguments.of("UTCID03 - Boundary", boundary, Optional.empty(),
                        new RuntimeException("DB error"),
                        HttpStatus.INTERNAL_SERVER_ERROR, 500)
        );
    }

    @DisplayName("resetPassword() :: UTCID01/02/03")
    @ParameterizedTest(name = "resetPassword() :: {0}")
    @MethodSource("resetPasswordCases")
    void resetPassword_test(String tcId,
                            ResetPasswordRequest req,
                            Optional<Account> accOpt,
                            HttpStatus expectedStatus,
                            Integer expectedCode,
                            boolean expectSave) {

        when(repository.findByResetPasswordToken(req.getToken())).thenReturn(accOpt);
        when(passwordEncoder.encode(req.getNewPassword())).thenReturn("ENC(" + req.getNewPassword() + ")");

        ResponseEntity<BaseResponse> res = sut.resetPassword(req);

        assertEquals(expectedStatus, res.getStatusCode(), tcId);
        assertNotNull(res.getBody(), tcId);
        if (expectedCode != null) {
            assertEquals(expectedCode, extractCode(res.getBody()), tcId + ": wrong code");
        }

        if (expectSave) verify(repository, times(1)).save(any(Account.class));
        else verify(repository, never()).save(any());
    }

    static Stream<Arguments> resetPasswordCases() {
        ResetPasswordRequest ok = new ResetPasswordRequest();
        ok.setToken("t1");
        ok.setNewPassword("NewPass@123");

        Account accOk = new Account();
        accOk.setResetPasswordToken("t1");
        accOk.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(5));

        ResetPasswordRequest notFound = new ResetPasswordRequest();
        notFound.setToken("missing");
        notFound.setNewPassword("NewPass@123");

        ResetPasswordRequest expired = new ResetPasswordRequest();
        expired.setToken("t2");
        expired.setNewPassword("NewPass@123");

        Account accExpired = new Account();
        accExpired.setResetPasswordToken("t2");
        accExpired.setResetPasswordTokenExpiry(LocalDateTime.now().minusNanos(1));

        return Stream.of(
                Arguments.of("UTCID01 - Normal", ok, Optional.of(accOk), HttpStatus.OK, 200, true),
                Arguments.of("UTCID02 - Abnormal", notFound, Optional.empty(), HttpStatus.BAD_REQUEST, 400, false),
                Arguments.of("UTCID03 - Boundary", expired, Optional.of(accExpired), HttpStatus.BAD_REQUEST, 400, false)
        );
    }

    // ==========================================================
    // ======================= Cart tests =======================
    // ==========================================================

    @DisplayName("Cart.getActiveCart() :: UTCID01/02/03")
    @ParameterizedTest(name = "getActiveCart() :: {0}")
    @MethodSource("getActiveCartCases")
    void getActiveCart_test(String tcId,
                            UUID customerId,
                            boolean customerExists,
                            boolean hasActiveCart) {

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);

        if (!customerExists) {
            when(customerRepo.findById(customerId)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> cartService.getActiveCart(customerId), tcId);
            return;
        }

        when(customerRepo.findById(customerId)).thenReturn(Optional.of(customer));

        if (hasActiveCart) {
            Cart cart = mock(Cart.class);
            when(cart.getCustomer()).thenReturn(customer);
            when(cart.getStatus()).thenReturn(CartStatus.ACTIVE);
            when(cart.getItems()).thenReturn(new ArrayList<>());
            when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

            CartResponse resp = cartService.getActiveCart(customerId);
            assertNotNull(resp, tcId);
            verify(cartRepo, times(1)).findByCustomerAndStatus(customer, CartStatus.ACTIVE);
        } else {
            when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.empty());

            CartResponse resp = cartService.getActiveCart(customerId);
            assertNotNull(resp, tcId);
            verify(cartRepo, times(1)).findByCustomerAndStatus(customer, CartStatus.ACTIVE);
        }
    }

    static Stream<Arguments> getActiveCartCases() {
        return Stream.of(
                Arguments.of("UTCID01 - Normal (has active cart)", UUID.randomUUID(), true, true),
                Arguments.of("UTCID02 - Abnormal (customer not found)", UUID.randomUUID(), false, false),
                Arguments.of("UTCID03 - Boundary (no cart => return empty cart)", UUID.randomUUID(), true, false)
        );
    }

    @DisplayName("Cart.checkCodEligibility() :: UTCID01/02/03")
    @ParameterizedTest(name = "checkCodEligibility() :: {0}")
    @MethodSource("checkCodEligibilityCases")
    void checkCodEligibility_test(String tcId,
                                  boolean overallExpected,
                                  BigDecimal depositBalance,
                                  BigDecimal ratio,
                                  BigDecimal lineTotal,
                                  boolean requestMatchesItem) {

        UUID customerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        when(customerRepo.findById(customerId)).thenReturn(Optional.of(customer));

        Store store = mock(Store.class);
        when(store.getStoreId()).thenReturn(storeId);
        when(store.getStoreName()).thenReturn("S1");

        Product product = mock(Product.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getStore()).thenReturn(store);

        CartItem item = mock(CartItem.class);
        when(item.getType()).thenReturn(CartItemType.PRODUCT);
        when(item.getProduct()).thenReturn(product);
        when(item.getCombo()).thenReturn(null);
        when(item.getLineTotal()).thenReturn(lineTotal);

        // required by matchesCartItem(...)
        when(item.getProductIdOrNull()).thenReturn(productId);
        when(item.getVariantIdOrNull()).thenReturn(null);

        Cart cart = mock(Cart.class);
        when(cart.getCustomer()).thenReturn(customer);
        when(cart.getStatus()).thenReturn(CartStatus.ACTIVE);
        when(cart.getItems()).thenReturn(List.of(item));

        when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CheckoutItemRequest req = new CheckoutItemRequest();
        req.setType("PRODUCT");
        if (requestMatchesItem) {
            req.setProductId(productId);
            req.setVariantId(null);
        } else {
            req.setProductId(UUID.randomUUID());
            req.setVariantId(null);
        }

        when(codConfig.getCodDepositRatio()).thenReturn(ratio);

        StoreWallet wallet = mock(StoreWallet.class);
        when(wallet.getDepositBalance()).thenReturn(depositBalance);
        when(storeWalletRepo2.findByStore_StoreId(storeId)).thenReturn(Optional.of(wallet));

        if (!requestMatchesItem) {
            assertThrows(IllegalStateException.class,
                    () -> cartService.checkCodEligibility(customerId, List.of(req)),
                    tcId);
            return;
        }

        CodEligibilityResponse resp = cartService.checkCodEligibility(customerId, List.of(req));
        assertNotNull(resp, tcId);
        assertEquals(overallExpected, resp.isOverallEligible(), tcId);

        verify(storeWalletRepo2, times(1)).findByStore_StoreId(storeId);
    }

    static Stream<Arguments> checkCodEligibilityCases() {
        return Stream.of(
                Arguments.of("UTCID01 - Normal (eligible)", true,
                        new BigDecimal("5000"), new BigDecimal("0.10"), new BigDecimal("30000"), true),
                Arguments.of("UTCID02 - Abnormal (no matching items)", false,
                        new BigDecimal("5000"), new BigDecimal("0.10"), new BigDecimal("30000"), false),
                Arguments.of("UTCID03 - Boundary (deposit equals required)", true,
                        new BigDecimal("3000"), new BigDecimal("0.10"), new BigDecimal("30000"), true)
        );
    }

    @DisplayName("Cart.updateItemQuantity() :: UTCID01/02/03")
    @ParameterizedTest(name = "updateItemQuantity() :: {0}")
    @MethodSource("updateItemQuantityCases")
    void updateItemQuantity_test(String tcId,
                                 UUID cartItemId,
                                 Integer newQty,
                                 boolean customerExists,
                                 boolean cartExists,
                                 boolean itemExists,
                                 boolean stockEnough) {

        UUID customerId = UUID.randomUUID();

        UpdateCartItemQtyRequest req = new UpdateCartItemQtyRequest();
        req.setCartItemId(cartItemId);
        req.setQuantity(newQty);

        if (cartItemId == null || newQty == null || newQty < 1) {
            assertThrows(IllegalArgumentException.class, () -> cartService.updateItemQuantity(customerId, req), tcId);
            return;
        }

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);

        if (!customerExists) {
            when(customerRepo.findById(customerId)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> cartService.updateItemQuantity(customerId, req), tcId);
            return;
        }
        when(customerRepo.findById(customerId)).thenReturn(Optional.of(customer));

        if (!cartExists) {
            when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> cartService.updateItemQuantity(customerId, req), tcId);
            return;
        }

        Cart cart = mock(Cart.class);
        when(cart.getCustomer()).thenReturn(customer);
        when(cart.getStatus()).thenReturn(CartStatus.ACTIVE);

        CartItem item = mock(CartItem.class);
        when(item.getCartItemId()).thenReturn(cartItemId);

        if (!itemExists) {
            when(cart.getItems()).thenReturn(List.of());
            when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
            assertThrows(NoSuchElementException.class, () -> cartService.updateItemQuantity(customerId, req), tcId);
            return;
        }

        when(item.getType()).thenReturn(CartItemType.PRODUCT);

        Product p = mock(Product.class);
        when(p.getName()).thenReturn("P1");
        when(p.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(p.getStockQuantity()).thenReturn(stockEnough ? 999 : 0);

        when(item.getProduct()).thenReturn(p);
        when(item.getVariant()).thenReturn(null);

        // base prices to avoid NPE in calculations
        when(p.getPrice()).thenReturn(new BigDecimal("1000"));
        when(item.getUnitPrice()).thenReturn(new BigDecimal("1000"));
        when(item.getLineTotal()).thenReturn(new BigDecimal("1000"));

        when(cart.getItems()).thenReturn(new ArrayList<>(List.of(item)));
        when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        // campaign repo called in pricing logic → return empty to keep it simple
        when(platformCampaignProductRepository.findAllActiveByProductLegacy(any(), any())).thenReturn(List.of());

        if (!stockEnough) {
            assertThrows(IllegalStateException.class, () -> cartService.updateItemQuantity(customerId, req), tcId);
            return;
        }

        CartResponse resp = cartService.updateItemQuantity(customerId, req);
        assertNotNull(resp, tcId);

        verify(cartRepo, times(1)).save(cart);
        verify(cartItemRepo, times(1)).save(item);
    }

    static Stream<Arguments> updateItemQuantityCases() {
        return Stream.of(
                Arguments.of("UTCID01 - Normal", UUID.randomUUID(), 2, true, true, true, true),
                Arguments.of("UTCID02 - Abnormal (stock not enough)", UUID.randomUUID(), 999, true, true, true, false),
                Arguments.of("UTCID03 - Boundary (invalid qty=0)", UUID.randomUUID(), 0, true, true, true, true)
        );
    }

    @DisplayName("Cart.clearCart() :: UTCID01/02/03")
    @ParameterizedTest(name = "clearCart() :: {0}")
    @MethodSource("clearCartCases")
    void clearCart_test(String tcId,
                        boolean customerExists,
                        boolean cartExists,
                        boolean hasItems) {

        UUID customerId = UUID.randomUUID();
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);

        if (!customerExists) {
            when(customerRepo.findById(customerId)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> cartService.clearCart(customerId), tcId);
            return;
        }
        when(customerRepo.findById(customerId)).thenReturn(Optional.of(customer));

        if (!cartExists) {
            when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> cartService.clearCart(customerId), tcId);
            return;
        }

        Cart cart = mock(Cart.class);
        when(cart.getCustomer()).thenReturn(customer);
        when(cart.getStatus()).thenReturn(CartStatus.ACTIVE);

        if (hasItems) {
            CartItem i1 = mock(CartItem.class);
            CartItem i2 = mock(CartItem.class);
            when(cart.getItems()).thenReturn(new ArrayList<>(List.of(i1, i2)));
        } else {
            when(cart.getItems()).thenReturn(new ArrayList<>());
        }

        when(cartRepo.findByCustomerAndStatus(customer, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse resp = cartService.clearCart(customerId);
        assertNotNull(resp, tcId);

        verify(cartRepo, times(1)).save(cart);
        if (hasItems) verify(cartItemRepo, times(1)).deleteAll(anyList());
        else verify(cartItemRepo, never()).deleteAll(anyList());
    }

    static Stream<Arguments> clearCartCases() {
        return Stream.of(
                Arguments.of("UTCID01 - Normal (cart has items)", true, true, true),
                Arguments.of("UTCID02 - Abnormal (customer not found)", false, true, true),
                Arguments.of("UTCID03 - Boundary (cart empty)", true, true, false)
        );
    }

    // ==========================================================
    // ========================= Helpers ========================
    // ==========================================================
    private static Integer extractCode(Object baseResponse) {
        if (baseResponse == null) return null;
        for (String getter : new String[]{"getCode", "getStatusCode", "getStatus"}) {
            try {
                var m = baseResponse.getClass().getMethod(getter);
                Object v = m.invoke(baseResponse);
                if (v instanceof Integer) return (Integer) v;
            } catch (Exception ignored) {}
        }
        for (String field : new String[]{"code", "statusCode", "status"}) {
            try {
                var f = baseResponse.getClass().getDeclaredField(field);
                f.setAccessible(true);
                Object v = f.get(baseResponse);
                if (v instanceof Integer) return (Integer) v;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
