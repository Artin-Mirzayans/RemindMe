package com.remindme.local;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/local")
public class LocalEventsController {

    // used when we can't work out where the visitor is
    private static final GeoLocation DEFAULT_LOCATION =
            new GeoLocation("New York", "New York", "United States", 40.7128, -74.0060, "America/New_York");

    private final LocalEventsService localEventsService;
    private final GeoLocationClient geoClient;

    public LocalEventsController(LocalEventsService localEventsService, GeoLocationClient geoClient) {
        this.localEventsService = localEventsService;
        this.geoClient = geoClient;
    }

    @GetMapping
    public ResponseEntity<LocalEventsWindow> nearby(HttpServletRequest request,
            @RequestParam(required = false) String location,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        GeoLocation resolved;
        if (location != null && !location.isBlank()) {
            resolved = new GeoLocation(location.trim(), null, null, 0, 0, null);
        } else {
            resolved = geoClient.resolve(clientIp(request)).orElse(DEFAULT_LOCATION);
        }
        return ResponseEntity.ok(localEventsService.forLocation(resolved, refresh));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
