package com.scanmyfood.backend.controllers;

import com.scanmyfood.backend.constants.ResponseCodeConstants;
import com.scanmyfood.backend.models.*;
import com.scanmyfood.backend.services.UserIntakeService;
import com.scanmyfood.backend.services.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static com.scanmyfood.backend.constants.ResponseCodeConstants.ERROR;


@Slf4j
@RestController
@RequestMapping("/api/users/intake")
public class UserIntakeController {

    @Autowired
    UserIntakeService userIntakeService;

    @Autowired
    FileStorageService fileStorageService;

    @PostMapping("/scanned-food")
    public ResponseEntity<ApiResponse<SaveIntakeOutput>> saveScannedFood(
            @RequestPart(value = "foodImage", required = false) MultipartFile foodImage,
            @RequestPart("saveScannedFoodInput") SaveScannedFoodInput saveScannedFoodInput) {

        log.info("Saving scanned food intake");
        log.info("Food Analysis: {}", saveScannedFoodInput);

        try {
            SaveIntakeOutput saveIntakeOutput = new SaveIntakeOutput();
            String accessUrl = "";
            if (foodImage != null && !foodImage.isEmpty()) {
                log.info("Food Image: {}", foodImage.getOriginalFilename());
                try {
                    String storedPath = fileStorageService.store(foodImage, "food-images");
                    accessUrl = fileStorageService.getAccessUrl(storedPath);
                } catch (Exception se) {
                    log.warn("Could not store food image ({}), proceeding to save intake: {}", se.getMessage(), se.getMessage());
                }
            }

            int dailyIntakeRecordId = userIntakeService.saveScannedFoodIntake(saveScannedFoodInput, accessUrl);
            saveIntakeOutput.setDailyIntakeId(dailyIntakeRecordId);
            log.info("Scanned food intake saved successfully");
            return ResponseEntity.ok(ApiResponse.success(saveIntakeOutput, ResponseCodeConstants.SCANNED_FOOD_SAVED, "Scanned food intake saved successfully."));

        } catch (Exception e) {
            log.error("Error saving scanned food: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ERROR, "Failed to save scanned food"));
        }
    }

    @PostMapping("/scanned-label")
    public ResponseEntity<ApiResponse<SaveIntakeOutput>> saveScannedLabel(
            @RequestPart("productImage") MultipartFile productImage,
            @RequestPart("saveScannedLabelInput") SaveScannedLabelInput saveScannedLabelInput) {

        log.info("Saving scanned label intake");
        log.info("Product Image: {}", productImage.getOriginalFilename());
        log.info("Product Analysis: {}", saveScannedLabelInput);

        try {
            SaveIntakeOutput saveIntakeOutput = new SaveIntakeOutput();
            String accessUrl = "";
            if (productImage != null && !productImage.isEmpty()) {
                try {
                    String storedPath = fileStorageService.store(productImage, "food-images");
                    accessUrl = fileStorageService.getAccessUrl(storedPath);
                } catch (Exception se) {
                    log.warn("Could not store product image ({}), proceeding to save intake: {}", se.getMessage(), se.getMessage());
                }
            }

            int dailyIntakeRecordId = userIntakeService.saveScannedLabelIntake(saveScannedLabelInput, accessUrl);
            saveIntakeOutput.setDailyIntakeId(dailyIntakeRecordId);
            log.info("Scanned product intake saved successfully");
            return ResponseEntity.ok(ApiResponse.success(saveIntakeOutput, ResponseCodeConstants.SCANNED_LABEL_SAVED, "Scanned product intake saved successfully"));

        } catch (Exception e) {
            log.error("Error saving scanned food: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ERROR, "Failed to save scanned food"));
        }
    }

    @GetMapping("/daily-intake")
    public ResponseEntity<ApiResponse<UserIntakeOutput>> getDailyIntake(
            @RequestParam("userId") String userId,
            @RequestParam("fromDate") LocalDate fromDate,
            @RequestParam("toDate") LocalDate toDate
    ) throws Exception {
        log.info("Fetching daily intake for userId: {} fromDate: {} toDate: {}", userId, fromDate, toDate);
        UserIntakeOutput dailyIntake = userIntakeService.getUserIntakeRange(userId, fromDate, toDate);
        log.info("Daily intake fetched successfully for userId: {} fromDate: {} toDate: {}", userId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(dailyIntake, ResponseCodeConstants.DAILY_INTAKE_FETCHED, "Daily intake fetched successfully"));
    }

    @GetMapping("/intake-record")
    public ResponseEntity<ApiResponse<FoodAnalysisResponse>> getIntake(
            @RequestParam("userId") String userId,
            @RequestParam("dailyIntakeId") int dailyIntakeId
    ) throws Exception {
        log.info("Fetching intake record for userId: {}, dailyIntakeId: {}", userId, dailyIntakeId);
        FoodAnalysisResponse intakeDetails = userIntakeService.getIntakeDetails(userId, dailyIntakeId);
        log.info("Intake record fetched for userId: {}, dailyIntakeId: {}", userId, dailyIntakeId);
        return ResponseEntity.ok(ApiResponse.success(intakeDetails, ResponseCodeConstants.INTAKE_ITEM_FETCHED, "Intake item fetched successfully"));
    }

    @DeleteMapping("/{dailyIntakeId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteIntakePath(
            @PathVariable("dailyIntakeId") int dailyIntakeId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) throws Exception {
        String userId = (headerUserId != null && !headerUserId.trim().isEmpty()) ? headerUserId : paramUserId;
        log.info("Deleting intake id: {} for user: {}", dailyIntakeId, userId);
        boolean deleted = userIntakeService.deleteIntake(userId, dailyIntakeId);
        return ResponseEntity.ok(ApiResponse.success(deleted, "INTAKE_DELETED", "Food intake deleted successfully"));
    }

    @DeleteMapping("/record")
    public ResponseEntity<ApiResponse<Boolean>> deleteIntakeParam(
            @RequestParam("dailyIntakeId") int dailyIntakeId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) throws Exception {
        String userId = (headerUserId != null && !headerUserId.trim().isEmpty()) ? headerUserId : paramUserId;
        log.info("Deleting intake id: {} for user: {}", dailyIntakeId, userId);
        boolean deleted = userIntakeService.deleteIntake(userId, dailyIntakeId);
        return ResponseEntity.ok(ApiResponse.success(deleted, "INTAKE_DELETED", "Food intake deleted successfully"));
    }

}
