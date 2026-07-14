package com.hasan.gateway.controllers;

import com.hasan.gateway.dtos.RegistrationRequest;
import com.hasan.gateway.services.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // 1. CREATE: The original registration endpoint
    @PostMapping("/register")
    public ResponseEntity<String> registerClient(@Valid @RequestBody RegistrationRequest request) {
        String rawApiKey = clientService.registerClientAndGenerateKey(
                request.companyName(), request.email(), request.tierType());
        return ResponseEntity.status(HttpStatus.CREATED).body(rawApiKey);
    }

    // 2. READ: Get info about a client by their ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getClient(@PathVariable Long id) {
        // Calls the service, returns a 200 OK with the client data as JSON
        Object clientData = clientService.findById(id);
        return ResponseEntity.ok(clientData);
    }

    // 3. UPDATE: Change their tier (e.g., upgrade to PRO)
    @PutMapping("/{id}/tier")
    public ResponseEntity<String> updateTier(
            @PathVariable Long id, 
            @RequestParam String newTier) {
        
        clientService.updateTier(id, newTier);
        return ResponseEntity.ok("Client " + id + " successfully upgraded to " + newTier + " tier.");
    }

    // 4. DELETE: Ban a client or remove their access
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteById(id);
        // 204 No Content is the REST standard for a successful deletion
        return ResponseEntity.noContent().build(); 
    }
}