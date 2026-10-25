package com.placementhub.backend.service;

import com.placementhub.backend.dto.ApplicationResponse;
import com.placementhub.backend.dto.InterviewSlotRequest;
import com.placementhub.backend.dto.InterviewSlotResponse;
import com.placementhub.backend.entity.Application;
import com.placementhub.backend.entity.InterviewBooking;
import com.placementhub.backend.entity.InterviewSlot;
import com.placementhub.backend.entity.Job;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.exception.BusinessRuleException;
import com.placementhub.backend.exception.ConflictException;
import com.placementhub.backend.exception.NotFoundException;
import com.placementhub.backend.mapper.ApplicationMapper;
import com.placementhub.backend.repository.InterviewBookingRepository;
import com.placementhub.backend.repository.InterviewSlotRepository;
import com.placementhub.backend.repository.StudentRepository;
import com.placementhub.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interview slot booking. Recruiters publish time slots with a fixed number of seats for
 * one of their jobs; a student whose application reached INTERVIEW books one seat.
 *
 * The invariant "a slot never has more bookings than seats" is enforced by the database,
 * not by checking in Java and then writing (which two requests could both pass):
 *   - InterviewSlotRepository.reserveSeat is one atomic conditional UPDATE,
 *   - chk_interview_slots_booked rejects booked_count > capacity,
 *   - uk_interview_bookings_application allows one booking per application.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewSlotService {

    private final InterviewSlotRepository slotRepository;
    private final InterviewBookingRepository bookingRepository;
    private final RecruiterService recruiterService;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final ApplicationMapper applicationMapper;
    private final Clock clock;

    // ---------------------------------------------------------------- recruiter side

    @Transactional
    public InterviewSlotResponse createSlot(Long jobId, InterviewSlotRequest request) {
        Job job = recruiterService.requireOwnedJob(jobId);
        if (job.getStatus() != JobStatus.APPROVED && job.getStatus() != JobStatus.CLOSED) {
            throw new BusinessRuleException("JOB_NOT_APPROVED",
                    "Interview slots can only be added to approved or closed jobs");
        }
        if (!request.getStartsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("SLOT_IN_PAST", "The interview slot must start in the future");
        }

        InterviewSlot slot = new InterviewSlot();
        slot.setJob(job);
        slot.setStartsAt(request.getStartsAt());
        slot.setDurationMinutes(request.getDurationMinutes());
        slot.setCapacity(request.getCapacity());
        slot.setLocation(request.getLocation() == null || request.getLocation().isBlank()
                ? null : request.getLocation().trim());
        slot = slotRepository.saveAndFlush(slot);

        InterviewSlotResponse response = toResponse(slot, 0);
        response.setBookings(List.of());
        return response;
    }

    public List<InterviewSlotResponse> getSlotsForRecruiter(Long jobId) {
        Job job = recruiterService.requireOwnedJob(jobId);
        List<InterviewSlot> slots = slotRepository.findByJobIdOrderByStartsAtAsc(job.getId());
        if (slots.isEmpty()) {
            return List.of();
        }

        Map<Long, List<InterviewSlotResponse.BookedApplicant>> bookingsBySlot = bookingRepository
                .findBySlotIdIn(slots.stream().map(InterviewSlot::getId).toList()).stream()
                .collect(Collectors.groupingBy(b -> b.getSlot().getId(),
                        Collectors.mapping(this::toBookedApplicant, Collectors.toList())));

        return slots.stream().map(slot -> {
            InterviewSlotResponse response = toResponse(slot, slot.getBookedCount());
            response.setBookings(bookingsBySlot.getOrDefault(slot.getId(), List.of()));
            return response;
        }).toList();
    }

    @Transactional
    public void deleteSlot(Long jobId, Long slotId) {
        Job job = recruiterService.requireOwnedJob(jobId);
        InterviewSlot slot = slotRepository.findById(slotId)
                .filter(s -> s.getJob().getId().equals(job.getId()))
                .orElseThrow(() -> new NotFoundException("Interview slot not found"));
        if (bookingRepository.existsBySlotId(slot.getId())) {
            throw new ConflictException("SLOT_HAS_BOOKINGS",
                    "Students have already booked this slot, so it cannot be deleted");
        }
        slotRepository.delete(slot);
    }

    // ---------------------------------------------------------------- student side

    /** Upcoming slots for the job of the student's own application (INTERVIEW stage only). */
    public List<InterviewSlotResponse> getSlotsForStudent(Long applicationId) {
        Application application = studentService.requireOwnApplication(applicationId);
        requireInterviewStage(application);

        Long bookedSlotId = bookingRepository.findByApplicationId(application.getId())
                .map(b -> b.getSlot().getId())
                .orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);

        return slotRepository.findByJobIdOrderByStartsAtAsc(application.getJob().getId()).stream()
                .filter(slot -> slot.getStartsAt().isAfter(now) || slot.getId().equals(bookedSlotId))
                .map(slot -> {
                    InterviewSlotResponse response = toResponse(slot, slot.getBookedCount());
                    response.setBookedByMe(slot.getId().equals(bookedSlotId));
                    return response;
                })
                .toList();
    }

    /**
     * Books one seat. Order matters for correctness, not just speed:
     *   1. friendly early checks (stage, slot belongs to this job, in the future, not booked),
     *   2. reserveSeat: the atomic UPDATE that decides whether a seat is left,
     *   3. insert the booking; UNIQUE(application_id) stops a double booking that raced
     *      past step 1. If it fails, the whole transaction (including the seat taken in
     *      step 2) rolls back, so seats are never leaked.
     */
    @Transactional
    public ApplicationResponse bookSlot(Long applicationId, Long slotId) {
        // First statement on purpose (see lockByUserEmail): serialises this student's
        // bookings so the "already booked" and overlap checks below cannot be raced.
        studentRepository.lockByUserEmail(SecurityUtils.getCurrentUserEmail());
        Application application = studentService.requireOwnApplication(applicationId);
        requireInterviewStage(application);

        InterviewSlot slot = slotRepository.findById(slotId)
                .filter(s -> s.getJob().getId().equals(application.getJob().getId()))
                .orElseThrow(() -> new NotFoundException("Interview slot not found"));
        if (!slot.getStartsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("SLOT_IN_PAST", "This interview slot has already started");
        }
        if (bookingRepository.existsByApplicationId(application.getId())) {
            throw alreadyBooked();
        }
        rejectClashWithOtherInterviews(application, slot);

        if (slotRepository.reserveSeat(slot.getId()) == 0) {
            throw new ConflictException("SLOT_FULL", "This interview slot is already full. Please pick another one.");
        }

        InterviewBooking booking = new InterviewBooking();
        booking.setSlot(slot);
        booking.setApplication(application);
        booking.setBookedAt(LocalDateTime.now(clock));
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            if (StudentService.isConstraint(e, "uk_interview_bookings_application")) {
                throw alreadyBooked();
            }
            throw e;
        }

        return applicationMapper.toResponse(application);
    }

    /** Cancels the student's booking and gives the seat back (only before the slot starts). */
    @Transactional
    public ApplicationResponse cancelBooking(Long applicationId) {
        Application application = studentService.requireOwnApplication(applicationId);
        InterviewBooking booking = bookingRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new NotFoundException("You have no interview booked for this application"));
        if (!booking.getSlot().getStartsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("SLOT_IN_PAST", "This interview has already started and cannot be cancelled");
        }

        Long slotId = booking.getSlot().getId();
        bookingRepository.delete(booking);
        bookingRepository.flush();
        slotRepository.releaseSeat(slotId);

        return applicationMapper.toResponse(application);
    }

    // ---------------------------------------------------------------- helpers

    private void requireInterviewStage(Application application) {
        if (application.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new BusinessRuleException("NOT_IN_INTERVIEW_STAGE",
                    "Interview slots open once the recruiter moves your application to INTERVIEW");
        }
    }

    /** A student cannot be in two interviews at once, even with different companies. */
    private void rejectClashWithOtherInterviews(Application application, InterviewSlot slot) {
        LocalDateTime start = slot.getStartsAt();
        LocalDateTime end = start.plusMinutes(slot.getDurationMinutes());
        for (InterviewBooking other : bookingRepository.findByApplicationStudentId(application.getStudent().getId())) {
            InterviewSlot booked = other.getSlot();
            LocalDateTime bookedEnd = booked.getStartsAt().plusMinutes(booked.getDurationMinutes());
            if (start.isBefore(bookedEnd) && booked.getStartsAt().isBefore(end)) {
                throw new ConflictException("INTERVIEW_CLASH",
                        "This overlaps your " + booked.getJob().getCompany().getName() + " interview at "
                                + booked.getStartsAt().toLocalTime() + " on " + booked.getStartsAt().toLocalDate()
                                + ". Please pick another slot.");
            }
        }
    }

    private static ConflictException alreadyBooked() {
        return new ConflictException("ALREADY_BOOKED",
                "You already have an interview booked for this application. Cancel it first to pick another slot.");
    }

    private InterviewSlotResponse toResponse(InterviewSlot slot, int bookedCount) {
        InterviewSlotResponse response = new InterviewSlotResponse();
        response.setId(slot.getId());
        response.setJobId(slot.getJob().getId());
        response.setStartsAt(slot.getStartsAt());
        response.setDurationMinutes(slot.getDurationMinutes());
        response.setLocation(slot.getLocation());
        response.setCapacity(slot.getCapacity());
        response.setBookedCount(bookedCount);
        response.setSeatsLeft(Math.max(slot.getCapacity() - bookedCount, 0));
        return response;
    }

    private InterviewSlotResponse.BookedApplicant toBookedApplicant(InterviewBooking booking) {
        InterviewSlotResponse.BookedApplicant applicant = new InterviewSlotResponse.BookedApplicant();
        applicant.setApplicationId(booking.getApplication().getId());
        applicant.setFullName(booking.getApplication().getStudent().getFullName());
        applicant.setApplicationStatus(booking.getApplication().getStatus().name());
        return applicant;
    }
}
