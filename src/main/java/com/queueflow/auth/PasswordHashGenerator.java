package com.queueflow.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PasswordHashGenerator <password>");
            System.exit(1);
        }

        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
