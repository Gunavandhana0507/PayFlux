package com.payflux.common;

public final class ApiExceptions {
    private ApiExceptions() {}

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
    public static class ConflictException extends RuntimeException {
        public final String code;
        public ConflictException(String code, String message) { super(message); this.code = code; }
    }
    public static class BusinessRuleException extends RuntimeException {
        public final String code;
        public BusinessRuleException(String code, String message) { super(message); this.code = code; }
    }
}
