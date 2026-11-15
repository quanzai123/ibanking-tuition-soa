package com.tdtu.soa.tuition.controller;

import com.tdtu.soa.common.dto.ApiResponse;
import com.tdtu.soa.common.dto.ClearDebtRequest;
import com.tdtu.soa.common.dto.TuitionDto;
import com.tdtu.soa.tuition.service.TuitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tuitions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TuitionController {

    private final TuitionService tuitionService;

    @GetMapping("/{mssv}")
    public ResponseEntity<ApiResponse<TuitionDto>> getTuitionByMssv(@PathVariable String mssv) {
        TuitionDto tuition = tuitionService.getTuitionByMssv(mssv);
        return ResponseEntity.ok(ApiResponse.ok(tuition));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TuitionDto>>> getAllTuitions() {
        List<TuitionDto> list = tuitionService.getAllTuitions();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/internal/clear-debt")
    public ResponseEntity<ApiResponse<TuitionDto>> clearDebt(@RequestBody ClearDebtRequest request) {
        TuitionDto result = tuitionService.clearDebt(request);
        return ResponseEntity.ok(ApiResponse.ok("Gạch nợ học phí thành công", result));
    }

    @PostMapping("/internal/reset")
    public ResponseEntity<ApiResponse<String>> resetData() {
        tuitionService.resetSeedData();
        return ResponseEntity.ok(ApiResponse.ok("Dữ liệu học phí đã được đặt lại thành công", "RESET_OK"));
    }
}

