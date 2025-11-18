package org.example.audio_ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * FakeDataSeeder
 * ------------------------------------------------------------
 * Tự động seed dữ liệu giả DEV cho hầu hết các bảng chính trong hệ thống.
 * Mỗi bảng tối thiểu 5 dòng, tuân thủ các ràng buộc NOT NULL / UNIQUE cơ bản.
 * Chỉ chạy ở profile "dev" (kích hoạt bằng spring.profiles.active=dev)
 * ------------------------------------------------------------
 * LƯU Ý: Đây là dữ liệu GIẢ phục vụ test giao diện / luồng nghiệp vụ.
 * Không dùng cho môi trường production.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Transactional
public class FakeDataSeeder {

    // ===== Repositories =====
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final WalletRepository walletRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final WarrantyRepository warrantyRepository;
    // Added repositories
    private final PlatformFeeRepository platformFeeRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ShopVoucherRepository shopVoucherRepository;

    private final Random random = new Random();

    // ===== Hằng số giả =====
    private static final String[] CATEGORY_NAMES = {
            "Loa Bluetooth", "Tai nghe Gaming", "Micro Thu Âm", "Ampli Stereo", "DAC Hi-End", "Mixer Live", "Sound Card", "Turntable", "Phụ kiện", "Loa Karaoke"
    };
    private static final String[] BRANDS = {"JBL", "Sony", "Sennheiser", "Yamaha", "AudioTechnica", "Shure", "Focusrite", "Behringer", "Bose", "Logitech"};
    private static final String[] COLORS = {"Black", "White", "Red", "Blue", "Silver", "Gray"};
    private static final String[] PROVINCES = {"Hà Nội", "TP.HCM", "Đà Nẵng", "Hải Phòng", "Cần Thơ"};
    private static final String[] DISTRICTS = {"Quận 1", "Quận 3", "Cầu Giấy", "Thanh Xuân", "Hải Châu"};
    private static final String[] WARDS = {"Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5"};

    @Deprecated // dùng seedAll() thủ công hoặc bật app.seed.enabled=true
    @PostConstruct
    public void legacyAutoSeed() { /* no-op: giữ để tránh lỗi nếu ai trông chờ @PostConstruct */ }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartupIfEnabled() {
        // Khi app.seed.enabled=true mới thực thi
        seedAll();
    }

    // Cho phép gọi thủ công từ controller hoặc script
    public void seedAll() {
        log.info("================= FAKE DATA SEEDING (dev) START =================");
        seedCategories();
        seedAccountsStoresCustomers();
        seedPlatformWallet();
        seedPlatformFees();
        seedShippingMethods();
        seedProductsAndVariants();
        seedShopVouchers();
        seedCartsAndItems();
        seedWarranties();
        log.info("================= FAKE DATA SEEDING (dev) DONE =================");
    }

    // ------------------------------------------------------------
    // Category
    // ------------------------------------------------------------
    private void seedCategories() {
        if (categoryRepository.count() >= 5) {
            log.info("Skip seed categories (already have {})", categoryRepository.count());
            return;
        }
        List<Category> categories = new ArrayList<>();
        int order = 1;
        for (String rawName : CATEGORY_NAMES) {
            if (categories.size() >= 10) break; // limit
            String name = rawName.trim();
            if (categoryRepository.findAll().stream().anyMatch(c -> c.getName().equalsIgnoreCase(name))) continue; // avoid duplicate
            categories.add(Category.builder()
                    .name(name)
                    .slug(slugify(name))
                    .description("Danh mục " + name)
                    .iconUrl("https://cdn.fake/icon/" + slugify(name) + ".png")
                    .sortOrder(order++)
                    .build());
        }
        categoryRepository.saveAll(categories);
        log.info("Seeded {} categories", categories.size());
    }

    // ------------------------------------------------------------
    // Accounts + Stores + Customers + Wallets
    // ------------------------------------------------------------
    private void seedAccountsStoresCustomers() {
        // Tạo 2 admin, 3 store owners, 5 customers
        if (accountRepository.count() >= 10) {
            log.info("Skip seed accounts (already have {})", accountRepository.count());
            return;
        }
        List<Account> accounts = new ArrayList<>();
        // Admins
        for (int i = 1; i <= 2; i++) {
            accounts.add(Account.builder()
                    .name("Admin " + i)
                    .email(buildEmail("admin", i))
                    .password("{noop}admin123")
                    .phone("0900" + String.format("%04d", i))
                    .role(RoleEnum.ADMIN)
                    .build());
        }
        // Store Owners
        for (int i = 1; i <= 3; i++) {
            accounts.add(Account.builder()
                    .name("Store Owner " + i)
                    .email(buildEmail("store", i))
                    .password("{noop}store123")
                    .phone("0911" + String.format("%04d", i))
                    .role(RoleEnum.STOREOWNER)
                    .build());
        }
        // Customers (linked later)
        for (int i = 1; i <= 5; i++) {
            accounts.add(Account.builder()
                    .name("Customer Acc " + i)
                    .email(buildEmail("custacc", i))
                    .password("{noop}cust123")
                    .phone("0922" + String.format("%04d", i))
                    .role(RoleEnum.CUSTOMER)
                    .build());
        }
        accountRepository.saveAll(accounts);
        log.info("Seeded {} accounts", accounts.size());

        // Stores + StoreWallet
        List<Account> storeAccounts = accounts.stream().filter(a -> a.getRole() == RoleEnum.STOREOWNER).collect(Collectors.toList());
        List<Store> stores = new ArrayList<>();
        for (int i = 0; i < storeAccounts.size(); i++) {
            Account acc = storeAccounts.get(i);
            Store store = Store.builder()
                    .account(acc)
                    .storeName("Audio Store " + (i + 1))
                    .description("Cửa hàng audio demo " + (i + 1))
                    .logoUrl("https://cdn.fake/logo/store" + (i + 1) + ".png")
                    .coverImageUrl("https://cdn.fake/cover/store" + (i + 1) + ".jpg")
                    .address(randomAddressString())
                    .phoneNumber("0988" + String.format("%04d", i))
                    .email(acc.getEmail())
                    .rating(BigDecimal.valueOf(4.0 + random.nextDouble()))
                    .status(StoreStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            stores.add(store);
        }
        storeRepository.saveAll(stores);
        log.info("Seeded {} stores", stores.size());

        // Attach wallets for stores
        List<StoreWallet> storeWallets = new ArrayList<>();
        for (Store s : stores) {
            StoreWallet w = StoreWallet.builder()
                    .store(s)
                    .availableBalance(BigDecimal.valueOf(random.nextInt(5000000) + 1_000_000))
                    .depositBalance(BigDecimal.valueOf(500_000))
                    .pendingBalance(BigDecimal.valueOf(300_000))
                    .totalRevenue(BigDecimal.valueOf(5_000_000 + random.nextInt(10_000_000)))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            storeWallets.add(w);
        }
        storeWalletRepository.saveAll(storeWallets);
        log.info("Seeded {} store wallets", storeWallets.size());

        // Customers + Wallet
        List<Account> customerAccounts = accounts.stream().filter(a -> a.getRole() == RoleEnum.CUSTOMER).collect(Collectors.toList());
        List<Customer> customers = new ArrayList<>();
        int cIndex = 1;
        for (Account acc : customerAccounts) {
            Customer c = Customer.builder()
                    .fullName("Customer " + cIndex)
                    .userName("cust" + cIndex)
                    .email(acc.getEmail())
                    .phoneNumber("0933" + String.format("%04d", cIndex))
                    .passwordHash("{noop}cust123")
                    .gender(Gender.MALE)
                    .dateOfBirth(LocalDate.of(1995, 1, Math.min(28, cIndex)))
                    .status(CustomerStatus.ACTIVE)
                    .kycStatus(KycStatus.NONE)
                    .loyaltyLevel(LoyaltyLevel.BRONZE)
                    .account(acc)
                    .build();
            customers.add(c);
            cIndex++;
        }
        customerRepository.saveAll(customers);
        log.info("Seeded {} customers", customers.size());

        // Customer Addresses
        List<CustomerAddress> addresses = new ArrayList<>();
        for (Customer c : customers) {
            for (int i = 1; i <= 2; i++) { // 2 địa chỉ mỗi customer
                CustomerAddress addr = CustomerAddress.builder()
                        .customer(c)
                        .receiverName(c.getFullName())
                        .phoneNumber(c.getPhoneNumber())
                        .label(AddressLabel.HOME)
                        .country("Việt Nam")
                        .province(randomArray(PROVINCES))
                        .district(randomArray(DISTRICTS))
                        .ward(randomArray(WARDS))
                        .street("Số " + (10 + random.nextInt(50)) + " Đường Demo")
                        .addressLine("Địa chỉ đầy đủ " + c.getFullName())
                        .postalCode("10000")
                        .note("Địa chỉ seed giả")
                        .isDefault(i == 1)
                        .provinceCode("01")
                        .districtId(760)
                        .wardCode("26734")
                        .build();
                addresses.add(addr);
            }
        }
        customerAddressRepository.saveAll(addresses);
        log.info("Seeded {} customer addresses", addresses.size());

        // Wallets
        List<Wallet> wallets = new ArrayList<>();
        for (Customer c : customers) {
            Wallet w = Wallet.builder()
                    .customer(c)
                    .balance(BigDecimal.valueOf(random.nextInt(2_000_000) + 200_000))
                    .currency("VND")
                    .status(WalletStatus.ACTIVE)
                    .lastTransactionAt(LocalDateTime.now())
                    .build();
            wallets.add(w);
        }
        walletRepository.saveAll(wallets);
        log.info("Seeded {} customer wallets", wallets.size());
    }

    // ------------------------------------------------------------
    // Platform Wallet + Transactions (if not already via DataInitializer)
    // ------------------------------------------------------------
    private void seedPlatformWallet() {
        if (platformWalletRepository.count() == 0) {
            PlatformWallet pf = PlatformWallet.builder()
                    .ownerType(WalletOwnerType.PLATFORM)
                    .ownerId(null)
                    .totalBalance(BigDecimal.ZERO)
                    .pendingBalance(BigDecimal.ZERO)
                    .doneBalance(BigDecimal.ZERO)
                    .receivedTotal(BigDecimal.ZERO)
                    .refundedTotal(BigDecimal.ZERO)
                    .currency("VND")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            platformWalletRepository.save(pf);
            log.info("Seeded platform wallet");
        }
        // add a few fake transactions
        PlatformWallet wallet = platformWalletRepository.findAll().stream().findFirst().orElse(null);
        if (wallet != null && platformTransactionRepository.count() < 5) {
            List<PlatformTransaction> txns = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                txns.add(PlatformTransaction.builder()
                        .wallet(wallet)
                        .amount(BigDecimal.valueOf(100_000L * i))
                        .type(TransactionType.INITIALIZE)
                        .status(TransactionStatus.DONE)
                        .description("Fake txn " + i)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
            platformTransactionRepository.saveAll(txns);
            log.info("Seeded {} platform transactions", txns.size());
        }
    }

    // New: Platform Fees
    private void seedPlatformFees() {
        if (platformFeeRepository.count() >= 5) {
            log.info("Skip seed platform fees (already have {})", platformFeeRepository.count());
            return;
        }
        List<PlatformFee> fees = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fees.add(PlatformFee.builder()
                    .percentage(BigDecimal.valueOf(3 + i))
                    .effectiveDate(LocalDateTime.now().minusDays(30L * i))
                    .description("Phí nền tảng demo kỳ " + i)
                    .isActive(i == 5) // chỉ bản ghi cuối active
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
        platformFeeRepository.saveAll(fees);
        log.info("Seeded {} platform fees", fees.size());
    }

    // New: Shipping Methods
    private void seedShippingMethods() {
        if (shippingMethodRepository.count() >= 5) {
            log.info("Skip seed shipping methods (already have {})", shippingMethodRepository.count());
            return;
        }
        String[][] data = {
                {"Giao Hàng Nhanh", "GHN"},
                {"Giao Hàng Tiết Kiệm", "GHTK"},
                {"Viettel Post", "VTPost"},
                {"EMS", "EMS"},
                {"J&T Express", "JNT"}
        };
        List<ShippingMethod> methods = new ArrayList<>();
        for (String[] row : data) {
            methods.add(ShippingMethod.builder()
                    .name(row[0])
                    .code(row[1])
                    .logoUrl("https://cdn.fake/shipping/" + row[1] + ".png")
                    .baseFee(BigDecimal.valueOf(25000))
                    .feePerKg(BigDecimal.valueOf(5000))
                    .estimatedDeliveryDays(2 + random.nextInt(3))
                    .supportCOD(true)
                    .supportInsurance(random.nextBoolean())
                    .isActive(true)
                    .description("Phương thức vận chuyển " + row[0])
                    .contactPhone("1900" + (10000 + random.nextInt(8999)))
                    .websiteUrl("https://" + row[1].toLowerCase() + ".vn")
                    .build());
        }
        shippingMethodRepository.saveAll(methods);
        log.info("Seeded {} shipping methods", methods.size());
    }

    // New: Shop Vouchers
    private void seedShopVouchers() {
        if (shopVoucherRepository.count() >= 5) {
            log.info("Skip seed shop vouchers (already have {})", shopVoucherRepository.count());
            return;
        }
        List<Store> stores = storeRepository.findAll();
        if (stores.isEmpty()) {
            log.warn("Cannot seed shop vouchers (stores empty)");
            return;
        }
        List<ShopVoucher> vouchers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Store store = stores.get(random.nextInt(stores.size()));
            vouchers.add(ShopVoucher.builder()
                    .shop(store)
                    .code("SALE" + (i * 10) + "K")
                    .title("Giảm giá " + (i * 10) + "K")
                    .description("Voucher demo giảm " + (i * 10) + "K")
                    .type(VoucherType.FIXED)
                    .discountValue(BigDecimal.valueOf(i * 10_000L))
                    .maxDiscountValue(BigDecimal.valueOf(i * 10_000L))
                    .minOrderValue(BigDecimal.valueOf(100_000))
                    .totalVoucherIssued(100)
                    .usagePerUser(2)
                    .remainingUsage(100)
                    .startTime(LocalDateTime.now().minusDays(1))
                    .endTime(LocalDateTime.now().plusDays(30))
                    .status(VoucherStatus.ACTIVE)
                    .createdBy(UUID.randomUUID())
                    .updatedBy(UUID.randomUUID())
                    .build());
        }
        shopVoucherRepository.saveAll(vouchers);
        log.info("Seeded {} shop vouchers", vouchers.size());
    }

    // ------------------------------------------------------------
    // Products + Variants
    // ------------------------------------------------------------
    private void seedProductsAndVariants() {
        if (productRepository.count() >= 5) {
            log.info("Skip seed products (already have {})", productRepository.count());
            return;
        }
        List<Category> categories = categoryRepository.findAll();
        List<Store> stores = storeRepository.findAll();
        if (categories.isEmpty() || stores.isEmpty()) {
            log.warn("Cannot seed products (categories or stores empty)");
            return;
        }
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Category cat = categories.get(random.nextInt(categories.size()));
            Store store = stores.get(random.nextInt(stores.size()));
            String brand = randomArray(BRANDS);
            Product p = Product.builder()
                    .store(store)
                    .category(cat)
                    .brandName(brand)
                    .name(brand + " Model " + i)
                    .slug(slugify(brand + " Model " + i))
                    .shortDescription("Sản phẩm demo " + i)
                    .description("<p>Mô tả chi tiết sản phẩm demo " + i + "</p>")
                    .model("M" + i)
                    .color(randomArray(COLORS))
                    .material("ABS Plastic")
                    .dimensions("20x10x5 cm")
                    .weight(BigDecimal.valueOf(0.5 + random.nextDouble()))
                    .images(Arrays.asList("https://cdn.fake/img/p" + i + "-1.jpg", "https://cdn.fake/img/p" + i + "-2.jpg"))
                    .videoUrl("https://youtube.com/watch?v=fake" + i)
                    .sku("SKU-" + i + "-" + System.currentTimeMillis())
                    .price(BigDecimal.valueOf(1_000_000 + random.nextInt(5_000_000)))
                    .discountPrice(BigDecimal.valueOf(800_000 + random.nextInt(2_000_000)))
                    .promotionPercent(BigDecimal.valueOf(5 + random.nextInt(30)))
                    .priceAfterPromotion(BigDecimal.valueOf(700_000 + random.nextInt(1_000_000)))
                    .finalPrice(BigDecimal.valueOf(650_000 + random.nextInt(800_000)))
                    .platformFeePercent(BigDecimal.valueOf(5))
                    .currency("VND")
                    .stockQuantity(50 + random.nextInt(200))
                    .warehouseLocation("Kho Demo")
                    .provinceCode("01")
                    .districtCode("760")
                    .wardCode("26734")
                    .shippingAddress(randomAddressString())
                    .shippingFee(BigDecimal.valueOf(30000 + random.nextInt(20000)))
                    .bulkDiscounts(Arrays.asList(
                            new Product.BulkDiscount(2,5,BigDecimal.valueOf(600_000)),
                            new Product.BulkDiscount(6,10,BigDecimal.valueOf(550_000))
                    ))
                    .status(ProductStatus.ACTIVE)
                    .isFeatured(i % 2 == 0)
                    .ratingAverage(BigDecimal.valueOf(4 + random.nextDouble()))
                    .reviewCount(10 * i)
                    .viewCount(100 * i)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .lastUpdatedAt(LocalDateTime.now())
                    .lastUpdateIntervalDays(0L)
                    .createdBy(UUID.randomUUID())
                    .updatedBy(UUID.randomUUID())
                    .frequencyResponse("60Hz - 18kHz")
                    .sensitivity("90dB")
                    .impedance("32Ω")
                    .powerHandling("100W")
                    .connectionType("Bluetooth 5.0")
                    .voltageInput("5V/2A")
                    .warrantyPeriod("24 tháng")
                    .warrantyType("1 đổi 1")
                    .manufacturerName("Hãng " + brand)
                    .manufacturerAddress("Địa chỉ Hãng " + brand)
                    .productCondition("New")
                    .isCustomMade(false)
                    .driverConfiguration("2-way")
                    .driverSize("6.5 inch")
                    .enclosureType("Bass Reflex")
                    .coveragePattern("180°")
                    .crossoverFrequency("2.5kHz")
                    .placementType("Bookshelf")
                    .headphoneType("Over-ear")
                    .compatibleDevices("PC, Mobile")
                    .isSportsModel(false)
                    .headphoneFeatures("ANC")
                    .batteryCapacity("1000mAh")
                    .hasBuiltInBattery(true)
                    .isGamingHeadset(false)
                    .headphoneAccessoryType("Cable")
                    .headphoneConnectionType("Wireless")
                    .plugType("3.5mm")
                    .sirimApproved(true)
                    .sirimCertified(true)
                    .mcmcApproved(true)
                    .micType("Dynamic")
                    .polarPattern("Cardioid")
                    .maxSPL("120dB")
                    .micOutputImpedance("150Ω")
                    .micSensitivity("-40dB")
                    .amplifierType("Class D")
                    .totalPowerOutput("500W")
                    .thd("0.05%")
                    .snr("100dB")
                    .inputChannels(4)
                    .outputChannels(2)
                    .supportBluetooth(true)
                    .supportWifi(true)
                    .supportAirplay(false)
                    .platterMaterial("Aluminum")
                    .motorType("Direct Drive")
                    .tonearmType("S-shaped")
                    .autoReturn(true)
                    .dacChipset("ESS")
                    .sampleRate("192kHz")
                    .bitDepth("24-bit")
                    .balancedOutput(true)
                    .inputInterface("USB")
                    .outputInterface("RCA")
                    .channelCount(2)
                    .hasPhantomPower(false)
                    .eqBands("10-band")
                    .faderType("Motorized")
                    .builtInEffects(true)
                    .usbAudioInterface(true)
                    .midiSupport(false)
                    .build();
            products.add(p);
        }
        productRepository.saveAll(products);
        log.info("Seeded {} products", products.size());

        // Variants
        List<ProductVariantEntity> variants = new ArrayList<>();
        for (Product p : products) {
            for (int i = 1; i <= 2; i++) {
                ProductVariantEntity v = new ProductVariantEntity();
                v.setOptionName("Color");
                v.setOptionValue(randomArray(COLORS));
                v.setVariantPrice(p.getFinalPrice());
                v.setVariantStock(10 + random.nextInt(50));
                v.setVariantUrl("https://cdn.fake/variant/" + p.getSku() + "/" + i);
                v.setVariantSku(p.getSku() + "-V" + i);
                v.setProduct(p);
                variants.add(v);
            }
        }
        productVariantRepository.saveAll(variants);
        log.info("Seeded {} product variants", variants.size());
    }

    // ------------------------------------------------------------
    // Carts + Items
    // ------------------------------------------------------------
    private void seedCartsAndItems() {
        if (cartRepository.count() >= 5) {
            log.info("Skip seed carts (already have {})", cartRepository.count());
            return;
        }
        List<Customer> customers = customerRepository.findAll();
        List<Product> products = productRepository.findAll();
        if (customers.isEmpty() || products.isEmpty()) {
            log.warn("Cannot seed carts (customers or products empty)");
            return;
        }
        List<Cart> carts = new ArrayList<>();
        for (int i = 0; i < Math.min(5, customers.size()); i++) {
            Customer c = customers.get(i);
            Cart cart = Cart.builder()
                    .customer(c)
                    .status(CartStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            carts.add(cart);
        }
        cartRepository.saveAll(carts);
        log.info("Seeded {} carts", carts.size());

        List<CartItem> items = new ArrayList<>();
        for (Cart cart : carts) {
            List<Product> picked = randomPick(products, 3);
            for (Product p : picked) {
                int qty = 1 + random.nextInt(3);
                BigDecimal unit = p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice();
                CartItem ci = CartItem.builder()
                        .cart(cart)
                        .type(CartItemType.PRODUCT)
                        .product(p)
                        .quantity(qty)
                        .unitPrice(unit)
                        .lineTotal(unit.multiply(BigDecimal.valueOf(qty)))
                        .nameSnapshot(p.getName())
                        .imageSnapshot(p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null)
                        .build();
                items.add(ci);
            }
        }
        cartItemRepository.saveAll(items);
        log.info("Seeded {} cart items", items.size());
    }

    // ------------------------------------------------------------
    // Warranties (giả lập 5 record)
    // ------------------------------------------------------------
    private void seedWarranties() {
        if (warrantyRepository.count() >= 5) {
            log.info("Skip seed warranties (already have {})", warrantyRepository.count());
            return;
        }
        List<Customer> customers = customerRepository.findAll();
        List<Store> stores = storeRepository.findAll();
        List<Product> products = productRepository.findAll();
        if (customers.isEmpty() || stores.isEmpty() || products.isEmpty()) {
            log.warn("Cannot seed warranties (missing dependencies)");
            return;
        }
        List<Warranty> warranties = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Warranty w = Warranty.builder()
                    .customer(customers.get(random.nextInt(customers.size())))
                    .store(stores.get(random.nextInt(stores.size())))
                    .product(products.get(random.nextInt(products.size())))
                    .serialNumber("SN-" + System.currentTimeMillis() + "-" + i)
                    .policyCode("POLICY" + i)
                    .durationMonths(12)
                    .purchaseDate(LocalDate.now().minusDays(10 + i))
                    .startDate(LocalDate.now().minusDays(10 + i))
                    .endDate(LocalDate.now().minusDays(10 + i).plusMonths(12))
                    .covered(true)
                    .status(WarrantyStatus.ACTIVE)
                    .notes("Bảo hành seed giả " + i)
                    .build();
            warranties.add(w);
        }
        warrantyRepository.saveAll(warranties);
        log.info("Seeded {} warranties", warranties.size());
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private String buildEmail(String prefix, int index) {
        return prefix + index + "@seed.local";
    }

    private String slugify(String s) {
        return s.toLowerCase().replace(" ", "-");
    }

    private String randomArray(String[] arr) { return arr[random.nextInt(arr.length)]; }

    private String randomAddressString() {
        return randomArray(STREETS()) + ", " + randomArray(WARDS) + ", " + randomArray(DISTRICTS) + ", " + randomArray(PROVINCES);
    }

    private String[] STREETS() { return new String[]{"Nguyễn Trãi", "Lý Thái Tổ", "Điện Biên Phủ", "Trần Hưng Đạo", "Phạm Văn Đồng"}; }

    private <T> List<T> randomPick(List<T> source, int max) {
        Collections.shuffle(source);
        return source.stream().limit(max).collect(Collectors.toList());
    }
}

