package com.arn.ycyw.your_car_your_way.controller;
import com.arn.ycyw.your_car_your_way.dto.DeleteAccountRequestDto;
import com.arn.ycyw.your_car_your_way.dto.UserDto;
import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.security.UsersDetailsAdapter;
import com.arn.ycyw.your_car_your_way.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/save")
    public ResponseEntity<Map<String,Object>> save(@Valid @RequestBody UserDto userDto) {
        UserDto savedUser = userService.save(userDto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.toString())
                .toUri();

        Map<String,Object> body = new HashMap<>();
        body.put("message", "User created !");
        body.put("user :", savedUser);

        return ResponseEntity
                .created(location)
                .body(body);
    }
    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAll() {
        List<UserDto>  userDtoList = userService.findAll();
        return ResponseEntity.ok(userDtoList);
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable("id") int id) {
        UserDto userDto = userService.findById(id);
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal UsersDetailsAdapter principal,
            @Valid @RequestBody DeleteAccountRequestDto request) {

        Integer currentUserId = principal.getUser().getId();
        userService.deleteWithPassword(currentUserId, request.getPassword());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal UsersDetailsAdapter principal) {
        //On recupere l'entité Users depuis l'adapter
        Integer id = principal.getUser().getId();
        // on réutilise le service métier qui retoune un UserDto
        UserDto userDto = userService.findById(id);
        //et on enveloppe dans un ResponseEntity
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/update")
    public ResponseEntity<UserDto> update( @AuthenticationPrincipal UsersDetailsAdapter principal,
                                           @RequestBody UserDto userDto) {
        Integer currentUserId = principal.getUser().getId();
        userDto.setId(currentUserId);
        UserDto updated = userService.update(userDto);
        return ResponseEntity.ok(updated);

    }

    /**
     * Endpoint pour valider ou refuser un compte professionnel via le lien email
     * Accessible sans authentification (lien dans l'email admin)
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmployee(
            @RequestParam("token") String token,
            @RequestParam("action") String action) {

        boolean approve = "approve".equalsIgnoreCase(action);
        String result = userService.verifyEmployee(token, approve);

        // Retourner une page HTML simple pour l'admin
        String htmlResponse = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verification - Your Car Your Way</title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: #f5f5f5; }
                    .card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center; max-width: 500px; }
                    .success { color: #22c55e; }
                    .reject { color: #ef4444; }
                    h1 { margin-bottom: 20px; }
                    p { color: #64748b; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1 class="%s">%s</h1>
                    <p>%s</p>
                    <p style="margin-top: 20px;">Vous pouvez fermer cette page.</p>
                </div>
            </body>
            </html>
            """.formatted(
                approve ? "success" : "reject",
                approve ? "✓ Compte validé" : "✕ Compte refusé",
                result
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(htmlResponse);
    }
}