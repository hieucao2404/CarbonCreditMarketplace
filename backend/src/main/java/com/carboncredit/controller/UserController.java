package com.carboncredit.controller;

import com.carboncredit.dto.UserDTO;
import com.carboncredit.entity.User;
import com.carboncredit.service.UserService;
import com.carboncredit.util.DTOMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            UserDTO userDTO = DTOMapper.toUserDTO(createdUser);
            return ResponseEntity.ok(userDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok(DTOMapper.toUserDTO(user)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
            .map(user -> ResponseEntity.ok(DTOMapper.toUserDTO(user)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable User.UserRole role) {
        List<User> users = userService.findByRole(role);
        List<UserDTO> userDTOs = DTOMapper.toUserDTOList(users);
        return ResponseEntity.ok(userDTOs);
    }
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = DTOMapper.toUserDTOList(users);
        return ResponseEntity.ok(userDTOs);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody User user) {
        return userService.findById(id)
            .map(existingUser -> {
                user.setId(id);
                User updatedUser = userService.updateUser(user);
                UserDTO userDTO = DTOMapper.toUserDTO(updatedUser);
                return ResponseEntity.ok(userDTO);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (userService.findById(id).isPresent()) {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
