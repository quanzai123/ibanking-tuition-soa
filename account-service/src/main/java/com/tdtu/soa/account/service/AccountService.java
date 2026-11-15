package com.tdtu.soa.account.service;

import com.tdtu.soa.account.entity.Account;
import com.tdtu.soa.account.entity.User;
import com.tdtu.soa.account.repository.AccountRepository;
import com.tdtu.soa.account.repository.UserRepository;
import com.tdtu.soa.account.security.JwtUtil;
import com.tdtu.soa.common.dto.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService implements CommandLineRunner {

        private final UserRepository userRepository;
        private final AccountRepository accountRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;

        @Transactional(readOnly = true)
        public LoginResponse login(LoginRequest request) {
                User user = userRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Tên đăng nhập hoặc mật khẩu không chính xác"));

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác");
                }

                Account account = accountRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Không tìm thấy thông tin tài khoản ngân hàng"));

                String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getFullName(),
                                account.getAccountNumber());

                UserDto userDto = UserDto.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .phone(user.getPhone())
                                .email(user.getEmail())
                                .accountNumber(account.getAccountNumber())
                                .balance(account.getBalance())
                                .build();

                return LoginResponse.builder()
                                .token(token)
                                .user(userDto)
                                .build();
        }

        @Transactional(readOnly = true)
        public UserDto getProfileFromToken(String token) {
                if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                }
                Claims claims = jwtUtil.extractClaims(token);
                String username = claims.getSubject();

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
                Account account = accountRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản ngân hàng"));

                return UserDto.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .phone(user.getPhone())
                                .email(user.getEmail())
                                .accountNumber(account.getAccountNumber())
                                .balance(account.getBalance())
                                .build();
        }

        @Transactional(readOnly = true)
        public UserDto getAccountByNumber(String accountNumber) {
                Account account = accountRepository.findByAccountNumber(accountNumber)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Số tài khoản không tồn tại: " + accountNumber));
                User user = userRepository.findById(account.getUserId())
                                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại"));

                return UserDto.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .phone(user.getPhone())
                                .email(user.getEmail())
                                .accountNumber(account.getAccountNumber())
                                .balance(account.getBalance())
                                .build();
        }

        /**
         * TRỪ TIỀN VỚI PESSIMISTIC WRITE LOCK
         * Đảm bảo không bị Double Spending khi có nhiều giao dịch đồng thời trên 1 tài
         * khoản
         */
        @Transactional(isolation = Isolation.READ_COMMITTED)
        public UserDto deductBalance(DeductBalanceRequest request) {
                log.info("[CONCURRENCY-PROTECT] Bắt đầu trừ tiền STK: {} - Số tiền: {} - TxId: {}",
                                request.getAccountNumber(), request.getAmount(), request.getTransactionId());

                // Khóa ghi hàng tài khoản với SELECT ... FOR UPDATE
                Account account = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tài khoản: " + request.getAccountNumber()));

                if (account.getBalance().compareTo(request.getAmount()) < 0) {
                        log.warn("[CONCURRENCY-PROTECT] Số dư không đủ! Hiện tại: {}, Yêu cầu trừ: {}",
                                        account.getBalance(), request.getAmount());
                        throw new IllegalStateException("Số dư khả dụng không đủ để thực hiện giao dịch (Hiện có: "
                                        + account.getBalance() + " VNĐ)");
                }

                account.setBalance(account.getBalance().subtract(request.getAmount()));
                account.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account);

                log.info("[CONCURRENCY-PROTECT] Trừ tiền thành công! Số dư mới: {}", account.getBalance());

                User user = userRepository.findById(account.getUserId()).orElse(null);
                return UserDto.builder()
                                .id(account.getUserId())
                                .username(user != null ? user.getUsername() : "")
                                .fullName(user != null ? user.getFullName() : "")
                                .accountNumber(account.getAccountNumber())
                                .balance(account.getBalance())
                                .build();
        }

        /**
         * HOÀN TIỀN (SAGA COMPENSATING TRANSACTION)
         * Gọi khi gạch nợ học phí thất bại do race condition
         */
        @Transactional(isolation = Isolation.READ_COMMITTED)
        public UserDto refundBalance(RefundBalanceRequest request) {
                log.info("[COMPENSATION] Bồi hoàn tiền STK: {} - Số tiền: {} - TxId: {}",
                                request.getAccountNumber(), request.getAmount(), request.getTransactionId());

                Account account = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tài khoản: " + request.getAccountNumber()));

                account.setBalance(account.getBalance().add(request.getAmount()));
                account.setUpdatedAt(LocalDateTime.now());
                accountRepository.save(account);

                log.info("[COMPENSATION] Hoàn tiền thành công! Số dư phục hồi: {}", account.getBalance());

                User user = userRepository.findById(account.getUserId()).orElse(null);
                return UserDto.builder()
                                .id(account.getUserId())
                                .accountNumber(account.getAccountNumber())
                                .balance(account.getBalance())
                                .build();
        }

        @Transactional
        public void resetSeedData() {
                log.info("[MAINTENANCE] Đặt lại dữ liệu mẫu cho Account Service...");
                accountRepository.deleteAll();
                userRepository.deleteAll();
                run();
        }

        @Override
        public void run(String... args) {
                if (userRepository.count() == 0) {
                        log.info("==> Đang khởi tạo dữ liệu mẫu cho Account Service (H2 Database)...");

                        // User 1: Nguyễn Văn An - SV TDTU kiêm người nộp tiền
                        User u1 = userRepository.save(User.builder()
                                        .username("user1")
                                        .password(passwordEncoder.encode("password123"))
                                        .fullName("Nguyễn Văn An")
                                        .phone("0901234567")
                                        .email("an.nguyen@example.com")
                                        .build());
                        accountRepository.save(Account.builder()
                                        .userId(u1.getId())
                                        .accountNumber("1000111222")
                                        .balance(new BigDecimal("35000000.00")) // 35 triệu để thoải mái test nhiều kịch
                                                                                // bản Concurrency
                                        .build());

                        // User 2: Trần Thị Bích - Số dư 10 triệu để test kịch bản 1 Concurrency Double
                        // Spending
                        User u2 = userRepository.save(User.builder()
                                        .username("user2")
                                        .password(passwordEncoder.encode("password123"))
                                        .fullName("Trần Thị Bích")
                                        .phone("0912345678")
                                        .email("bich.tran@example.com")
                                        .build());
                        accountRepository.save(Account.builder()
                                        .userId(u2.getId())
                                        .accountNumber("2000333444")
                                        .balance(new BigDecimal("10000000.00")) // 10 triệu (nhỏ hơn 9.5tr + 8.2tr =
                                                                                // 17.7tr)
                                        .build());

                        // User 3: Lê Hoàng Nam - Phụ huynh nộp hộ học phí
                        User u3 = userRepository.save(User.builder()
                                        .username("parent1")
                                        .password(passwordEncoder.encode("password123"))
                                        .fullName("Lê Hoàng Nam (Phụ huynh)")
                                        .phone("0987654321")
                                        .email("nam.le@example.com")
                                        .build());
                        accountRepository.save(Account.builder()
                                        .userId(u3.getId())
                                        .accountNumber("3000555666")
                                        .balance(new BigDecimal("50000000.00")) // 50 triệu
                                        .build());

                        log.info("==> Khởi tạo 3 tài khoản iBanking mẫu hoàn tất!");
                }
        }
}
