package com.Elvis.ticket.controller;

import com.Elvis.ticket.dto.TicketResponse;
import com.Elvis.ticket.model.*;
import com.Elvis.ticket.service.TicketService;
import com.Elvis.ticket.service.UserService;
import com.Elvis.ticket.repository.CustomerRepository;
import com.Elvis.ticket.repository.ManagerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;

    @Autowired
    public TicketController(TicketService ticketService, UserService userService, CustomerRepository customerRepository, ManagerRepository managerRepository) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.customerRepository = customerRepository;
        this.managerRepository = managerRepository;
    }

    @GetMapping
    public List<TicketResponse> getAllTickets(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String username = authentication.getName();
        if (role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER")) {
            return ticketService.getAllTickets().stream()
                    .map(TicketResponse::fromTicket)
                    .collect(Collectors.toList());
        } else if (role.equals("ROLE_CUSTOMER")) {
            Long customerId = userService.getUserByEmail(username).get().getId();
            return ticketService.getTicketsByCustomerId(customerId).stream()
                    .map(TicketResponse::fromTicket)
                    .collect(Collectors.toList());
        } else if (role.equals("ROLE_ENGINEER")) {
            Long engineerId = userService.getUserByEmail(username).get().getId();
            return ticketService.getTicketsByEngineerId(engineerId).stream()
                    .map(TicketResponse::fromTicket)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ticketService.getTicketById(id)
                .map(TicketResponse::fromTicket)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TicketResponse createTicket(@RequestBody Ticket ticket) {
        return TicketResponse.fromTicket(ticketService.createTicket(ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long id, @RequestBody Ticket ticket) {
        try {
            Ticket updatedTicket = ticketService.updateTicket(id, ticket);
            return ResponseEntity.ok(TicketResponse.fromTicket(updatedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id, Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER")) {
            ticketService.deleteTicket(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/customer/{customerId}")
    public List<TicketResponse> getTicketsByCustomerId(@PathVariable Long customerId) {
        return ticketService.getTicketsByCustomerId(customerId).stream()
                .map(TicketResponse::fromTicket)
                .collect(Collectors.toList());
    }

    @GetMapping("/engineer/{engineerId}")
    public List<TicketResponse> getTicketsByEngineerId(@PathVariable Long engineerId) {
        return ticketService.getTicketsByEngineerId(engineerId).stream()
                .map(TicketResponse::fromTicket)
                .collect(Collectors.toList());
    }

    @GetMapping("/status/{status}")
    public List<TicketResponse> getTicketsByStatus(@PathVariable TicketStatus status) {
        return ticketService.getTicketsByStatus(status).stream()
                .map(TicketResponse::fromTicket)
                .collect(Collectors.toList());
    }

    @GetMapping("/urgency/{urgency}")
    public List<TicketResponse> getTicketsByUrgency(@PathVariable CustomerRole urgency) {
        return ticketService.getTicketsByUrgency(urgency).stream()
                .map(TicketResponse::fromTicket)
                .collect(Collectors.toList());
    }

    @GetMapping("/type/{typeId}")
    public List<TicketResponse> getTicketsByType(@PathVariable Long typeId) {
        return ticketService.getTicketsByType(typeId).stream()
                .map(TicketResponse::fromTicket)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(@PathVariable Long id, @RequestBody TicketStatus status) {
        try {
            Ticket updatedTicket = ticketService.updateTicketStatus(id, status);
            return ResponseEntity.ok(TicketResponse.fromTicket(updatedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/urgency")
    public ResponseEntity<TicketResponse> updateTicketUrgency(@PathVariable Long id, @RequestBody String urgency) {
        try {
            Ticket updatedTicket = ticketService.updateTicketUrgency(id, urgency);
            return ResponseEntity.ok(TicketResponse.fromTicket(updatedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignEngineer(@PathVariable Long id, @RequestBody Long engineerId) {
        try {
            Ticket updatedTicket = ticketService.assignTicket(id, engineerId);
            return ResponseEntity.ok(TicketResponse.fromTicket(updatedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<TicketResponse> escalateTicket(@PathVariable Long id) {
        try {
            Ticket escalatedTicket = ticketService.escalateTicket(id);
            return ResponseEntity.ok(TicketResponse.fromTicket(escalatedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/own")
    public List<TicketResponse> getOwnTickets(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        if (role.equals("ROLE_CUSTOMER")) {
            Long customerId = customerRepository.findByEmail(username).getId();
            return ticketService.getTicketsByCustomerId(customerId).stream()
                    .map(TicketResponse::fromTicket)
                    .collect(Collectors.toList());
        } else if (role.equals("ROLE_ENGINEER")) {
            Long engineerId = userService.getUserByEmail(username).get().getId();
            return ticketService.getTicketsByEngineerId(engineerId).stream()
                    .map(TicketResponse::fromTicket)
                    .collect(Collectors.toList());
        }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<TicketResponse> addMessageToTicketHistory(
            @PathVariable Long id,
            @RequestBody(required = true) java.util.Map<String, String> message,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Ticket ticket = ticketService.getTicketById(id).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }
            boolean isAdminOrManager = "ADMIN".equals(user.getRole()) || "MANAGER".equals(user.getRole());
            boolean isTicketOwner = ticket.getCustomer() != null && ticket.getCustomer().getUser().getId().equals(user.getId());
            boolean isAssignedEngineer = ticket.getEngineer() != null && ticket.getEngineer().getUser().getId().equals(user.getId());
            if (!isAdminOrManager && !isTicketOwner && !isAssignedEngineer) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            String content = message.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // Determine message role
            String msgRole = isAdminOrManager ? user.getRole().toLowerCase() : (isAssignedEngineer ? "engineer" : "customer");
            Ticket updatedTicket = ticketService.appendMessageToHistory(id, user, content, msgRole);
            return ResponseEntity.ok(TicketResponse.fromTicket(updatedTicket));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Attachment endpoints ---
    @PostMapping("/{ticketId}/attachments")
    public ResponseEntity<TicketAttachment> uploadAttachment(@PathVariable Long ticketId, @RequestParam("file") MultipartFile file) {
        try {
            TicketAttachment attachment = ticketService.saveAttachment(ticketId, file);
            return ResponseEntity.ok(attachment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{ticketId}/attachments")
    public ResponseEntity<List<TicketAttachment>> listAttachments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getAttachments(ticketId));
    }

    @GetMapping("/{ticketId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        try {
            TicketAttachment attachment = ticketService.getAttachment(attachmentId);
            if (attachment.getTicket() == null || !ticketId.equals(attachment.getTicket().getId())) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, attachment.getContentType() != null ? attachment.getContentType() : "application/octet-stream")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/manager/category")
    public List<TicketResponse> getTicketsByManagerCategory(Authentication authentication) {
        try {
            String email = authentication.getName();
            com.Elvis.ticket.model.Manager manager = managerRepository.findByEmail(email);
            if (manager == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, 
                    "User is not a manager"
                );
            }
            TeslaModel category = manager.getCategory();
            if (category == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, 
                    "Manager has no assigned category"
                );
            }
            return ticketService.getTicketsByCategory(category).stream()
                    .map(TicketResponse::fromTicket)
                    .collect(java.util.stream.Collectors.toList());
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Error fetching tickets: " + e.getMessage()
            );
        }
    }

    @GetMapping("/category/{category}")
    public List<TicketResponse> getTicketsByCategory(@PathVariable String category) {
        TeslaModel model = TeslaModel.valueOf(category);
        return ticketService.getTicketsByCategory(model).stream()
                .map(TicketResponse::fromTicket)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/servility")
    public ResponseEntity<TicketResponse> updateTicketServility(@PathVariable Long id, @RequestBody String level) {
        try {
            com.Elvis.ticket.model.ServilityLevel servLevel = com.Elvis.ticket.model.ServilityLevel.valueOf(level.trim().toUpperCase());
            Ticket updatedTicket = ticketService.updateTicketServility(id, servLevel);
            return ResponseEntity.ok(TicketResponse.fromTicket(updatedTicket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 