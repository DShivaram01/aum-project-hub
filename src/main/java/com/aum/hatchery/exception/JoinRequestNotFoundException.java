package com.aum.hatchery.exception;

public class JoinRequestNotFoundException extends RuntimeException {

    public JoinRequestNotFoundException(Long id) {
        super("Join request not found with id " + id);
    }
}
