package com.carboncredit.service;

import com.carboncredit.entity.User;
import com.carboncredit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUser_success_encodesPasswordAndSaves() {
        User input = new User();
        input.setId(UUID.randomUUID());
        input.setUsername("testuser");
        input.setEmail("test@example.com");
        input.setPasswordHash("plainpwd");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainpwd")).thenReturn("encodedpwd");

        User saved = new User();
        saved.setId(input.getId());
        saved.setUsername(input.getUsername());
        saved.setEmail(input.getEmail());
        saved.setPasswordHash("encodedpwd");

        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.createUser(input);

        assertNotNull(result);
        assertEquals("encodedpwd", result.getPasswordHash());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertEquals("encodedpwd", captured.getPasswordHash());
    }

    @Test
    void createUser_duplicateUsername_throws() {
        User input = new User();
        input.setUsername("dupuser");
        input.setEmail("a@b.com");
        input.setPasswordHash("pwd");

        when(userRepository.existsByUsername("dupuser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(input));
        assertTrue(ex.getMessage().contains("Username already exists"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_duplicateEmail_throws() {
        User input = new User();
        input.setUsername("userx");
        input.setEmail("dup@b.com");
        input.setPasswordHash("pwd");

        when(userRepository.existsByUsername("userx")).thenReturn(false);
        when(userRepository.existsByEmail("dup@b.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(input));
        assertTrue(ex.getMessage().contains("Email already exists"));

        verify(userRepository, never()).save(any());
    }


    public static void main(String[] arges) {
        UserServiceTest userServiceTest = new UserServiceTest();
        userServiceTest.testLoginFromTerminal();
    }

    @Test
    void testLoginFromTerminal() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter username: ");
        String inputUsername = scanner.nextLine();
        System.out.println("Enter password: ");
        String inputPassword = scanner.nextLine();

        Optional<User> foundUser = userService.findByUsername(inputUsername);

        if(foundUser.isPresent()) {
            User user = foundUser.get();
            if(user.getPasswordHash().equals(inputPassword)){
                System.out.println("login successfully");
                assertTrue(true);
            }else {
                System.out.println("Wrong password");
                fail("password mismatch");
            }
                
        }else{
            System.out.println("User not found");
            fail("User not found");
        }
    }

    
}
