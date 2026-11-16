package com.tdtu.soa.tuition.service;

import com.tdtu.soa.common.dto.ClearDebtRequest;
import com.tdtu.soa.common.dto.TuitionDto;
import com.tdtu.soa.tuition.entity.Student;
import com.tdtu.soa.tuition.entity.Tuition;
import com.tdtu.soa.tuition.repository.StudentRepository;
import com.tdtu.soa.tuition.repository.TuitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TuitionService implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final TuitionRepository tuitionRepository;

    @Transactional(readOnly = true)
    public TuitionDto getTuitionByMssv(String mssv) {
        Student student = studentRepository.findById(mssv)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có MSSV: " + mssv));

        Tuition tuition = tuitionRepository.findByMssv(mssv)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin học phí cho MSSV: " + mssv));

        return TuitionDto.builder()
                .mssv(student.getMssv())
                .studentName(student.getFullName())
                .faculty(student.getFaculty())
                .major(student.getMajor())
                .semester(tuition.getSemester())
                .amount(tuition.getAmount())
                .status(tuition.getStatus())
                .paidAt(tuition.getPaidAt())
                .transactionRef(tuition.getTransactionRef())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TuitionDto> getAllTuitions() {
        return tuitionRepository.findAll().stream().map(tuition -> {
            Student student = studentRepository.findById(tuition.getMssv()).orElse(null);
            return TuitionDto.builder()
                    .mssv(tuition.getMssv())
                    .studentName(student != null ? student.getFullName() : "N/A")
                    .faculty(student != null ? student.getFaculty() : "N/A")
                    .major(student != null ? student.getMajor() : "N/A")
                    .semester(tuition.getSemester())
                    .amount(tuition.getAmount())
                    .status(tuition.getStatus())
                    .paidAt(tuition.getPaidAt())
                    .transactionRef(tuition.getTransactionRef())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * GẠCH NỢ NGUYÊN TỬ (ATOMIC DEBT CLEARING)
     * Ngăn chặn hoàn toàn tình huống nhiều tài khoản cùng thanh toán cho 1 MSSV
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TuitionDto clearDebt(ClearDebtRequest request) {
        log.info("[CONCURRENCY-PROTECT] Thực hiện gạch nợ MSSV: {} - TxRef: {}",
                request.getMssv(), request.getTransactionRef());

        int updatedRows = tuitionRepository.atomicClearDebt(request.getMssv(), request.getTransactionRef());

        if (updatedRows == 0) {
            log.warn("[CONCURRENCY-PROTECT] Gạch nợ thất bại! Khoản nợ MSSV {} đã bị thanh toán trước đó!", request.getMssv());
            throw new IllegalStateException("KHOAN_HOC_PHI_DA_DUOC_THANH_TOAN");
        }

        log.info("[CONCURRENCY-PROTECT] Gạch nợ thành công cho MSSV: {}", request.getMssv());
        return getTuitionByMssv(request.getMssv());
    }

    @Transactional
    public TuitionDto resetSingleTuition(String mssv) {
        log.info("[TEST-ASSIST] Đặt lại trạng thái học phí cho MSSV: {}", mssv);
        Tuition tuition = tuitionRepository.findByMssv(mssv)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin học phí cho MSSV: " + mssv));
        tuition.setStatus("UNPAID");
        tuition.setPaidAt(null);
        tuition.setTransactionRef(null);
        tuitionRepository.save(tuition);
        log.info("[TEST-ASSIST] Đặt lại thành công MSSV {} về trạng thái UNPAID", mssv);
        return getTuitionByMssv(mssv);
    }

    @Transactional
    public void resetSeedData() {
        log.info("[MAINTENANCE] Đặt lại dữ liệu mẫu cho Tuition Service...");
        tuitionRepository.deleteAll();
        studentRepository.deleteAll();
        run();
    }

    @Override
    public void run(String... args) {
        if (studentRepository.count() == 0) {
            log.info("==> Đang khởi tạo dữ liệu mẫu cho Tuition Service (H2 Database)...");

            // SV 1: Nguyễn Anh Quân (UNPAID)
            studentRepository.save(Student.builder()
                    .mssv("524H0122")
                    .fullName("Nguyễn Anh Quân")
                    .faculty("Công Nghệ Thông Tin")
                    .major("Kỹ Thuật Phần Mềm")
                    .build());
            tuitionRepository.save(Tuition.builder()
                    .mssv("524H0122")
                    .semester("HK1/2026-2027")
                    .amount(new BigDecimal("9500000.00")) // 9.5 triệu
                    .status("UNPAID")
                    .build());

            // SV 2: Nguyễn Hồng Quân (UNPAID)
            studentRepository.save(Student.builder()
                    .mssv("524H0123")
                    .fullName("Nguyễn Hồng Quân")
                    .faculty("Công Nghệ Thông Tin")
                    .major("Khoa Học Máy Tính")
                    .build());
            tuitionRepository.save(Tuition.builder()
                    .mssv("524H0123")
                    .semester("HK1/2026-2027")
                    .amount(new BigDecimal("8200000.00")) // 8.2 triệu
                    .status("UNPAID")
                    .build());

            // SV 3: Lê Bảo Trâm (UNPAID - Học phí cao để test)
            studentRepository.save(Student.builder()
                    .mssv("524H0999")
                    .fullName("Lê Bảo Trâm")
                    .faculty("Ngoại Ngữ")
                    .major("Ngôn Ngữ Anh Chất Lượng Cao")
                    .build());
            tuitionRepository.save(Tuition.builder()
                    .mssv("524H0999")
                    .semester("HK1/2026-2027")
                    .amount(new BigDecimal("12000000.00")) // 12 triệu
                    .status("UNPAID")
                    .build());

            // SV 4: Phạm Minh Đức (Đã thanh toán trước - để test case tra cứu báo đã đóng)
            studentRepository.save(Student.builder()
                    .mssv("524H0888")
                    .fullName("Phạm Minh Đức")
                    .faculty("Điện - Điện Tử")
                    .major("Kỹ Thuật Điều Khiển & Tự Động Hóa")
                    .build());
            tuitionRepository.save(Tuition.builder()
                    .mssv("524H0888")
                    .semester("HK1/2026-2027")
                    .amount(new BigDecimal("7800000.00"))
                    .status("PAID")
                    .transactionRef("TX-SAMPLE-ALREADY-PAID")
                    .paidAt(LocalDateTime.now().minusDays(2))
                    .build());

            log.info("==> Khởi tạo 4 sinh viên & khoản học phí mẫu TDTU hoàn tất!");
        }
    }
}

