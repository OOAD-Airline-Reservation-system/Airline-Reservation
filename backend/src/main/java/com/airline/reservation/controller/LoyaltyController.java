package com.airline.reservation.controller;

import com.airline.reservation.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for loyalty programme endpoints.
 * Controller (GRASP): delegates all logic to LoyaltyService.
 * SRP: only HTTP routing concern.
 */
@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    /** GET /api/loyalty/me */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(loyaltyService.getAccountSummary(userDetails.getUsername()));
    }

    /**
     * POST /api/loyalty/redeem
     * Body: { "points": 500, "bookingReference": "BK-XXXX" }
     */
    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Object>> redeemPoints(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        int points = Integer.parseInt(body.get("points").toString());
        String bookingRef = body.getOrDefault("bookingReference", "").toString();
        var discount = loyaltyService.redeemPoints(userDetails.getUsername(), points, bookingRef);
        var summary = loyaltyService.getAccountSummary(userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "discountINR", discount,
                "remainingPoints", summary.get("pointsBalance")
        ));
    }
}
