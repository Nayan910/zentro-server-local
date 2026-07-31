package com.zentro.server.service;

import com.zentro.server.model.Gig;
import com.zentro.server.model.GigApplication;
import com.zentro.server.model.User;
import com.zentro.server.repository.GigApplicationRepository;
import com.zentro.server.repository.GigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GigService {

    private final GigRepository gigRepository;
    private final GigApplicationRepository applicationRepository;
    private final ActivityLogService logService;

    public GigService(GigRepository gigRepository,
                      GigApplicationRepository applicationRepository,
                      ActivityLogService logService) {
        this.gigRepository = gigRepository;
        this.applicationRepository = applicationRepository;
        this.logService = logService;
    }

    public Map<String, Object> createGig(User user, String title, String description,
                                          double budget, String location, String tradeCategory,
                                          String deadline, String ipAddress) {
        Gig gig = new Gig();
        gig.setTitle(title);
        gig.setDescription(description);
        gig.setBudget(budget);
        gig.setLocation(location);
        gig.setTradeCategory(tradeCategory);
        gig.setUser(user);

        if (deadline != null && !deadline.isEmpty()) {
            try {
                gig.setDeadlineAt(LocalDateTime.parse(deadline));
            } catch (Exception ignored) {}
        }

        gig = gigRepository.save(gig);
        logService.log(user, "GIG_CREATE", "Created gig: " + title, ipAddress);

        return sanitizeGig(gig);
    }

    public List<Map<String, Object>> getAllOpenGigs() {
        return gigRepository.findByStatusOrderByCreatedAtDesc("open").stream()
                .map(this::sanitizeGig)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMyGigs(User user) {
        List<Gig> posted = gigRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Gig> accepted = gigRepository.findByAcceptedByIdOrderByCreatedAtDesc(user.getId());

        return java.util.stream.Stream.concat(posted.stream(), accepted.stream())
                .distinct()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::sanitizeGig)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getGigById(Long gigId) {
        Gig gig = gigRepository.findById(gigId).orElse(null);
        return gig != null ? sanitizeGig(gig) : null;
    }

    @Transactional
    public Map<String, Object> applyForGig(User user, Long gigId, String message, String ipAddress) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new RuntimeException("Gig not found"));

        if (applicationRepository.existsByGigIdAndUserId(gigId, user.getId())) {
            throw new RuntimeException("Already applied for this gig");
        }

        GigApplication application = new GigApplication();
        application.setGig(gig);
        application.setUser(user);
        application.setMessage(message);
        application = applicationRepository.save(application);

        gig.setApplicantCount(gig.getApplicantCount() + 1);
        gigRepository.save(gig);

        logService.log(user, "GIG_APPLY", "Applied for gig: " + gig.getTitle(), ipAddress);

        return sanitizeApplication(application);
    }

    @Transactional
    public Map<String, Object> acceptApplication(User gigOwner, Long applicationId, String ipAddress) {
        GigApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        Gig gig = application.getGig();
        if (!gig.getUser().getId().equals(gigOwner.getId())) {
            throw new RuntimeException("Not authorized to accept this application");
        }

        application.setStatus("accepted");
        applicationRepository.save(application);

        gig.setStatus("in_progress");
        gig.setAcceptedBy(application.getUser());
        gigRepository.save(gig);

        logService.log(gigOwner, "GIG_ACCEPT", "Accepted application for: " + gig.getTitle(), ipAddress);

        return sanitizeGig(gig);
    }

    public List<Map<String, Object>> getGigApplications(Long gigId) {
        return applicationRepository.findByGigIdOrderByCreatedAtDesc(gigId).stream()
                .map(this::sanitizeApplication)
                .collect(Collectors.toList());
    }

    private Map<String, Object> sanitizeGig(Gig gig) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", gig.getId());
        map.put("title", gig.getTitle());
        map.put("description", gig.getDescription());
        map.put("budget", gig.getBudget());
        map.put("location", gig.getLocation());
        map.put("tradeCategory", gig.getTradeCategory());
        map.put("status", gig.getStatus());
        map.put("applicantCount", gig.getApplicantCount());
        map.put("createdAt", gig.getCreatedAt());
        map.put("deadlineAt", gig.getDeadlineAt());

        if (gig.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", gig.getUser().getId());
            userMap.put("username", gig.getUser().getUsername());
            userMap.put("tradeCategory", gig.getUser().getTradeCategory());
            userMap.put("rating", gig.getUser().getRating());
            map.put("user", userMap);
        }

        if (gig.getAcceptedBy() != null) {
            Map<String, Object> acceptedMap = new HashMap<>();
            acceptedMap.put("id", gig.getAcceptedBy().getId());
            acceptedMap.put("username", gig.getAcceptedBy().getUsername());
            map.put("acceptedBy", acceptedMap);
        }

        return map;
    }

    private Map<String, Object> sanitizeApplication(GigApplication app) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", app.getId());
        map.put("message", app.getMessage());
        map.put("status", app.getStatus());
        map.put("createdAt", app.getCreatedAt());

        if (app.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", app.getUser().getId());
            userMap.put("username", app.getUser().getUsername());
            userMap.put("tradeCategory", app.getUser().getTradeCategory());
            userMap.put("rating", app.getUser().getRating());
            map.put("user", userMap);
        }

        if (app.getGig() != null) {
            map.put("gigId", app.getGig().getId());
            map.put("gigTitle", app.getGig().getTitle());
        }

        return map;
    }
}
