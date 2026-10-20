package com.artstudio.artstudio.service;

import com.artstudio.artstudio.dto.PotteryForm;
import com.artstudio.artstudio.dto.PotteryOptionsResponse;
import com.artstudio.artstudio.dto.PotteryResponse;
import com.artstudio.artstudio.entity.Glaze;
import com.artstudio.artstudio.entity.PotteryItem;
import com.artstudio.artstudio.entity.PotteryRequest;
import com.artstudio.artstudio.entity.User;
import com.artstudio.artstudio.exception.ApiException;
import com.artstudio.artstudio.repository.GlazeRepository;
import com.artstudio.artstudio.repository.PotteryItemRepository;
import com.artstudio.artstudio.repository.PotteryRequestRepository;
import com.artstudio.artstudio.repository.UserRepository;
import com.artstudio.artstudio.security.AuthUser;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PotteryService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final PotteryRequestRepository requestRepository;
    private final PotteryItemRepository itemRepository;
    private final GlazeRepository glazeRepository;
    private final UserRepository userRepository;

    public PotteryService(PotteryRequestRepository requestRepository,
                          PotteryItemRepository itemRepository,
                          GlazeRepository glazeRepository,
                          UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.glazeRepository = glazeRepository;
        this.userRepository = userRepository;
    }

    public PotteryOptionsResponse options() {
        return new PotteryOptionsResponse(
                itemRepository.findAll(Sort.by("id")),
                glazeRepository.findAll(Sort.by("id")));
    }

    public PotteryResponse create(Long userId, PotteryForm form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in again"));

        checkDate(form.neededBy());
        PotteryItem item = findItem(form.itemId());
        Glaze glaze = findGlaze(form.glazeId());

        PotteryRequest request = new PotteryRequest();
        request.setUser(user);
        request.setItem(item);
        request.setGlaze(glaze);
        request.setQuantity(form.quantity());
        request.setNeededBy(form.neededBy());
        request.setStatus("requested");
        request.setCreatedAt(LocalDateTime.now(IST));
        requestRepository.save(request);

        return toResponse(request);
    }

    public List<PotteryResponse> list(AuthUser user) {
        List<PotteryRequest> requests = "studio".equals(user.role())
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByUserIdOrderByCreatedAtDesc(user.id());
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional
    public PotteryResponse update(Long userId, Long requestId, PotteryForm form) {
        // Not yours (or not found) gives 404.
        requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));

        checkDate(form.neededBy());
        PotteryItem item = findItem(form.itemId());
        Glaze glaze = findGlaze(form.glazeId());

        int updated = requestRepository.updateIfRequested(
                requestId, item, glaze, form.quantity(), form.neededBy());
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This request can no longer be edited because it is not 'requested' anymore");
        }
        return reload(requestId);
    }

    @Transactional
    public PotteryResponse cancel(Long userId, Long requestId) {
        requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));

        if (requestRepository.cancelIfRequested(requestId) == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This request can no longer be cancelled because it is not 'requested' anymore");
        }
        return reload(requestId);
    }

    @Transactional
    public PotteryResponse accept(Long requestId) {
        findById(requestId);
        if (requestRepository.acceptIfRequested(requestId) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Only requested items can be accepted");
        }
        return reload(requestId);
    }

    @Transactional
    public PotteryResponse decline(Long requestId) {
        findById(requestId);
        if (requestRepository.declineIfRequested(requestId) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Only requested items can be declined");
        }
        return reload(requestId);
    }

    private void checkDate(LocalDate neededBy) {
        LocalDate earliest = LocalDate.now(IST).plusDays(14);
        if (neededBy.isBefore(earliest)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Needed-by date must be at least 14 days from today");
        }
    }

    private PotteryItem findItem(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown pottery item"));
    }

    private Glaze findGlaze(String glazeId) {
        return glazeRepository.findById(glazeId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown glaze"));
    }

    private PotteryRequest findById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    private PotteryResponse reload(Long requestId) {
        return toResponse(findById(requestId));
    }

    private PotteryResponse toResponse(PotteryRequest r) {
        return new PotteryResponse(
                r.getId(),
                r.getItem().getId(),
                r.getItem().getName(),
                r.getGlaze().getId(),
                r.getGlaze().getName(),
                r.getQuantity(),
                r.getNeededBy(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUser().getName(),
                r.getUser().getEmail()
        );
    }
}