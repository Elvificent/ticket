package com.Elvis.ticket.service;

import com.Elvis.ticket.model.Ticket;
import com.Elvis.ticket.model.TicketStatus;
import com.Elvis.ticket.model.CustomerRole;
import com.Elvis.ticket.model.Session;
import com.Elvis.ticket.model.Engineer;
import com.Elvis.ticket.model.Customer;
import com.Elvis.ticket.model.Product;
import com.Elvis.ticket.model.TicketType;
import com.Elvis.ticket.model.TeslaModel;
import com.Elvis.ticket.model.User;
import com.Elvis.ticket.repository.TicketRepository;
import com.Elvis.ticket.repository.EngineerRepository;
import com.Elvis.ticket.repository.CustomerRepository;
import com.Elvis.ticket.repository.ProductRepository;
import com.Elvis.ticket.repository.TicketTypeRepository;
import com.Elvis.ticket.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.Elvis.ticket.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.Elvis.ticket.model.TicketAttachment;
import com.Elvis.ticket.repository.TicketAttachmentRepository;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.Elvis.ticket.model.ServilityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private final TicketRepository ticketRepository;
    private final EngineerRepository engineerRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SessionRepository sessionRepository;
    private final UserService userService;
    private final TicketAttachmentRepository ticketAttachmentRepository;

    public TicketService(TicketRepository ticketRepository, 
                        EngineerRepository engineerRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        TicketTypeRepository ticketTypeRepository,
                        SessionRepository sessionRepository,
                        UserService userService,
                        TicketAttachmentRepository ticketAttachmentRepository) {
        this.ticketRepository = ticketRepository;
        this.engineerRepository = engineerRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.sessionRepository = sessionRepository;
        this.userService = userService;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
    }

    @Transactional
    public Ticket createTicket(Ticket ticket) {
        // If customer is not set, use the authenticated user
        if (ticket.getCustomer() == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Customer customer = customerRepository.findByEmail(email);
            if (customer == null) {
                throw new RuntimeException("Authenticated customer not found");
            }
            ticket.setCustomer(customer);
        }
        // Validate all foreign key references exist
        Customer customer = customerRepository.findById(ticket.getCustomer().getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        TicketType type = ticketTypeRepository.findById(ticket.getType().getId())
                .orElseThrow(() -> new RuntimeException("Ticket type not found"));
        Session session = sessionRepository.findById(ticket.getSession().getId())
                .orElseThrow(() -> new RuntimeException("Session not found"));
        // Set the validated entities
        ticket.setCustomer(customer);
        ticket.setType(type);
        ticket.setSession(session);
        // --- Engineer assignment logic ---
        if (ticket.getEngineer() == null || ticket.getEngineer().getId() == null) {
            TeslaModel category = ticket.getCategory();
            Engineer assignedEngineer = null;
            List<Engineer> availableEngineers = null;
            if (category != null) {
                // Use only available engineers (currentTickets < maxTickets) and level 1
                availableEngineers = engineerRepository.findByCategoryAndCurrentTicketsLessThanMaxTickets(category)
                    .stream().filter(e -> e.getLevel() == 1).toList();
            }
            if (availableEngineers != null && !availableEngineers.isEmpty()) {
                // Find the engineer with the least currentTickets
                assignedEngineer = availableEngineers.get(0);
                for (Engineer eng : availableEngineers) {
                    if (eng.getCurrentTickets() < assignedEngineer.getCurrentTickets()) {
                        assignedEngineer = eng;
                    }
                }
            } else {
                // Fallback: get all available level 1 engineers and pick the one with the least tickets
                List<Engineer> allLevel1 = engineerRepository.findByLevel(1)
                    .stream().filter(e -> e.getCurrentTickets() < e.getMaxTickets()).toList();
                if (allLevel1 != null && !allLevel1.isEmpty()) {
                    assignedEngineer = allLevel1.get(0);
                    for (Engineer eng : allLevel1) {
                        if (eng.getCurrentTickets() < assignedEngineer.getCurrentTickets()) {
                            assignedEngineer = eng;
                        }
                    }
                }
            }
            if (assignedEngineer != null) {
                ticket.setEngineer(assignedEngineer);
                ticket.setCategory(assignedEngineer.getCategory());
                assignedEngineer.setCurrentTickets(assignedEngineer.getCurrentTickets() + 1);
                engineerRepository.save(assignedEngineer);
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            } else {
                throw new RuntimeException("No available engineer found for this product category.");
            }
        } else {
            // Ensure engineer is a managed entity
            Engineer engineer = engineerRepository.findById(ticket.getEngineer().getId())
                .orElseThrow(() -> new RuntimeException("Engineer not found"));
            ticket.setEngineer(engineer);
            ticket.setCategory(engineer.getCategory());
            // Increment engineer's currentTickets
            engineer.setCurrentTickets(engineer.getCurrentTickets() + 1);
            engineerRepository.save(engineer);
        }
        // Set timestamps
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);
        // Add engineer welcome message to ticket history
        if (ticket.getEngineer() != null && ticket.getCustomer() != null) {
            Engineer engineer = ticket.getEngineer();
            String engineerName = engineer.getUser().getName();
            String customerName = customer.getUser().getName();
            String text = String.format("Hi %s, I am %s. I will be helping you to solve your problem, please let me have a look at the problem first.", customerName, engineerName);
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> ticketHistory = new ArrayList<>();
            try {
                if (savedTicket.getHistory() != null && !savedTicket.getHistory().isEmpty()) {
                    ticketHistory = mapper.readValue(savedTicket.getHistory(), new TypeReference<List<Map<String, Object>>>() {});
                }
            } catch (Exception e) {
                ticketHistory = new ArrayList<>();
            }
            Map<String, Object> engineerMsg = new HashMap<>();
            engineerMsg.put("role", "engineer");
            engineerMsg.put("content", text);
            engineerMsg.put("timestamp", java.time.LocalDateTime.now().toString());
            ticketHistory.add(engineerMsg);
            try {
                savedTicket.setHistory(mapper.writeValueAsString(ticketHistory));
                ticketRepository.save(savedTicket);
            } catch (Exception e) {
                // handle error
            }
        }
        return savedTicket;
    }

    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByCustomerId(Long customerId) {
        return ticketRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByEngineerId(Long engineerId) {
        return ticketRepository.findByEngineerId(engineerId);
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }

    @Transactional
    public Ticket assignTicket(Long ticketId, Long engineerId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        Engineer newEngineer = engineerRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));
        if (newEngineer.getCurrentTickets() >= newEngineer.getMaxTickets()) {
            throw new RuntimeException("Engineer has reached maximum ticket capacity");
        }
        Engineer oldEngineer = ticket.getEngineer();
        if (oldEngineer != null && !oldEngineer.getId().equals(newEngineer.getId())) {
            // Decrement old engineer's ticket count
            oldEngineer.setCurrentTickets(oldEngineer.getCurrentTickets() - 1);
            engineerRepository.save(oldEngineer);
        }
        ticket.setEngineer(newEngineer);
        ticket.setCategory(newEngineer.getCategory());
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        newEngineer.setCurrentTickets(newEngineer.getCurrentTickets() + 1);
        engineerRepository.save(newEngineer);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicketStatus(Long ticketId, TicketStatus status) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        if (status == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(LocalDateTime.now());
            if (ticket.getEngineer() != null) {
                Engineer engineer = ticket.getEngineer();
                engineer.setCurrentTickets(engineer.getCurrentTickets() - 1);
                engineerRepository.save(engineer);
                // ticket.setEngineer(null); // <-- Do NOT remove engineer assignment
            }
        } else if (status == TicketStatus.CLOSED) {
            ticket.setStatus(TicketStatus.CLOSED);
            if (ticket.getEngineer() != null) {
                Engineer engineer = ticket.getEngineer();
                engineer.setCurrentTickets(engineer.getCurrentTickets() - 1);
                engineerRepository.save(engineer);
                // ticket.setEngineer(null); // <-- Do NOT remove engineer assignment
            }
        } else if (status == TicketStatus.IN_PROGRESS) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        } else if (status == TicketStatus.ESCALATED) {
            ticket.setStatus(TicketStatus.ESCALATED);
        } else {
            ticket.setStatus(status);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicketUrgency(Long ticketId, String urgency) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        ticket.setUrgency(urgency);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(Long id, Ticket ticketDetails) {
        return ticketRepository.findById(id)
                .map(existingTicket -> {
                    // Validate and update ticket type if changed
                    if (ticketDetails.getType() != null && 
                        !ticketDetails.getType().getId().equals(existingTicket.getType().getId())) {
                        TicketType type = ticketTypeRepository.findById(ticketDetails.getType().getId())
                                .orElseThrow(() -> new RuntimeException("Ticket type not found"));
                        existingTicket.setType(type);
                    }

                    existingTicket.setTitle(ticketDetails.getTitle());
                    existingTicket.setUrgency(ticketDetails.getUrgency());
                    if (ticketDetails.getServilityLevel() != null) {
                        existingTicket.setServilityLevel(ticketDetails.getServilityLevel());
                    }
                    existingTicket.setUpdatedAt(LocalDateTime.now());
                    return ticketRepository.save(existingTicket);
                })
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
    }

    public List<Ticket> getTicketsByUrgency(CustomerRole urgency) {
        return ticketRepository.findByUrgency(urgency);
    }

    public List<Ticket> getTicketsByCategory(TeslaModel category) {
        return ticketRepository.findByCategory(category);
    }

    public List<Ticket> getTicketsByType(Long typeId) {
        return ticketRepository.findByTypeId(typeId);
    }

    @Transactional
    public Ticket escalateTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    Engineer currentEngineer = ticket.getEngineer();
                    if (currentEngineer == null) {
                        throw new RuntimeException("Ticket has no assigned engineer to escalate from");
                    }
                    TeslaModel category = currentEngineer.getCategory();
                    int currentLevel = currentEngineer.getLevel();
                    int nextLevel = currentLevel + 1;
                    if (nextLevel > 3) {
                        throw new RuntimeException("This ticket is already assigned to the highest level engineer and cannot be escalated further.");
                    }
                    List<Engineer> nextLevelEngineers = engineerRepository.findByCategoryAndLevel(category, nextLevel)
                        .stream()
                        .filter(e -> e.getCurrentTickets() < e.getMaxTickets())
                        .toList();
                    if (nextLevelEngineers.isEmpty()) {
                        // No available higher-level engineer, set status to ESCALATED and keep current engineer
                        ticket.setStatus(TicketStatus.ESCALATED);
                        ticket.setUpdatedAt(LocalDateTime.now());
                        return ticketRepository.save(ticket);
                    }
                    // Select the engineer with the least current tickets
                    Engineer newEngineer = nextLevelEngineers.get(0);
                    for (Engineer eng : nextLevelEngineers) {
                        if (eng.getCurrentTickets() < newEngineer.getCurrentTickets()) {
                            newEngineer = eng;
                        }
                    }
                    // Decrement old engineer's ticket count
                    currentEngineer.setCurrentTickets(currentEngineer.getCurrentTickets() - 1);
                    engineerRepository.save(currentEngineer);
                    // Update ticket with new engineer
                    ticket.setEngineer(newEngineer);
                    ticket.setCategory(newEngineer.getCategory());
                    ticket.setStatus(TicketStatus.IN_PROGRESS);
                    ticket.setUpdatedAt(LocalDateTime.now());
                    // Increment new engineer's ticket count
                    newEngineer.setCurrentTickets(newEngineer.getCurrentTickets() + 1);
                    engineerRepository.save(newEngineer);
                    return ticketRepository.save(ticket);
                })
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByCustomer(Long customerId) {
        return ticketRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByEngineer(Long engineerId) {
        return ticketRepository.findByEngineerId(engineerId);
    }

    @Transactional
    public Ticket appendCustomerMessageToHistory(Long ticketId, User user, String content) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<java.util.Map<String, Object>> history;
        try {
            if (ticket.getHistory() != null && !ticket.getHistory().isEmpty()) {
                history = mapper.readValue(ticket.getHistory(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});
            } else {
                history = new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            history = new java.util.ArrayList<>();
        }
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("role", "customer");
        msg.put("content", content);
        msg.put("timestamp", java.time.LocalDateTime.now().toString());
        history.add(msg);
        try {
            ticket.setHistory(mapper.writeValueAsString(history));
        } catch (Exception e) {
            // ignore
        }
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket appendMessageToHistory(Long ticketId, User user, String content, String role) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<java.util.Map<String, Object>> history;
        try {
            if (ticket.getHistory() != null && !ticket.getHistory().isEmpty()) {
                history = mapper.readValue(ticket.getHistory(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});
            } else {
                history = new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            history = new java.util.ArrayList<>();
        }
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        msg.put("timestamp", java.time.LocalDateTime.now().toString());
        history.add(msg);
        try {
            ticket.setHistory(mapper.writeValueAsString(history));
        } catch (Exception e) {
            // ignore
        }
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        // Update lastResponseTime if engineer replied
        if ("engineer".equalsIgnoreCase(role)) {
            ticket.setLastResponseTime(java.time.LocalDateTime.now());
        }
        return ticketRepository.save(ticket);
    }

    // Add a method to auto-close tickets with last update > 7 days
    @Transactional
    public void autoCloseOldTickets() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<Ticket> oldTickets = ticketRepository.findAll().stream()
            .filter(t -> t.getStatus() != TicketStatus.CLOSED && t.getUpdatedAt().isBefore(threshold))
            .toList();
        for (Ticket ticket : oldTickets) {
            if (ticket.getEngineer() != null) {
                Engineer engineer = ticket.getEngineer();
                engineer.setCurrentTickets(engineer.getCurrentTickets() - 1);
                engineerRepository.save(engineer);
                // ticket.setEngineer(null); // <-- Do NOT remove engineer assignment
            }
            ticket.setStatus(TicketStatus.CLOSED);
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
        }
    }

    // --- Attachment methods ---
    private static final String ATTACHMENT_BASE_PATH = "attachments";

    @Transactional
    public TicketAttachment saveAttachment(Long ticketId, MultipartFile file) throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        if (file == null) {
            throw new IllegalArgumentException("File is missing");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }
        String sanitizedFilename = Paths.get(originalFilename).getFileName().toString();
        if (sanitizedFilename.isBlank()) {
            throw new IllegalArgumentException("Filename contains invalid path components");
        }
        String timestampedFilename = System.currentTimeMillis() + "_" + sanitizedFilename;
        // Ensure directory exists
        Path dir = Paths.get(ATTACHMENT_BASE_PATH, String.valueOf(ticketId)).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        // Save file to disk
        Path filePath = dir.resolve(timestampedFilename).normalize();
        if (!filePath.startsWith(dir)) {
            logger.warn("Path traversal attempt detected for ticket {} and filename {}", ticketId, originalFilename);
            throw new IllegalArgumentException("Path traversal attempt detected");
        }
        Files.copy(file.getInputStream(), filePath);
        // Save metadata to DB
        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setFilename(sanitizedFilename);
        attachment.setContentType(file.getContentType());
        attachment.setFilePath(filePath.toString());
        attachment.setUploadedAt(java.time.LocalDateTime.now());
        return ticketAttachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public java.util.List<TicketAttachment> getAttachments(Long ticketId) {
        return ticketAttachmentRepository.findByTicketId(ticketId);
    }

    @Transactional(readOnly = true)
    public TicketAttachment getAttachment(Long attachmentId) {
        return ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    public boolean isEngineerAssignedToSession(Long engineerId, Long sessionId) {
        return ticketRepository.existsByEngineerIdAndSessionId(engineerId, sessionId);
    }

    @Transactional
    public Ticket updateTicketServility(Long ticketId, ServilityLevel level) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setServilityLevel(level);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }
}