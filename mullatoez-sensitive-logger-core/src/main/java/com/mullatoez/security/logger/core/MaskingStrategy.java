package com.mullatoez.security.logger.core;

public class MaskingStrategy {

    private static final String FULL_MASK = "********";

    public String mask(Object value, MaskingMode mode) {
        if (value == null) {
            return null;
        }

        String rawValue = String.valueOf(value);

        if (rawValue.isBlank()) {
            return rawValue;
        }

        if (mode == MaskingMode.FULL) {
            return FULL_MASK;
        }

        if (looksLikeEmail(rawValue)) {
            return partiallyMaskEmail(rawValue);
        }

        return partiallyMask(rawValue);
    }

    private boolean looksLikeEmail(String value) {
        return value.contains("@") && value.indexOf("@") > 0 && value.indexOf("@") < value.length() - 1;
    }

    private String partiallyMaskEmail(String email) {
        String[] parts = email.split("@", 2);

        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocalPart = maskLocalPart(localPart);
        String maskedDomainPart = maskDomainPart(domainPart);

        return maskedLocalPart + "@" + maskedDomainPart;
    }

    private String maskLocalPart(String localPart) {
        if (localPart.length() <= 1) {
            return "*";
        }

        return localPart.charAt(0) + "***";
    }

    private String maskDomainPart(String domainPart) {
        int lastDotIndex = domainPart.lastIndexOf(".");

        if (lastDotIndex <= 0) {
            return partiallyMask(domainPart);
        }

        String domainName = domainPart.substring(0, lastDotIndex);
        String extension = domainPart.substring(lastDotIndex);

        String maskedDomainName;

        if (domainName.length() <= 1) {
            maskedDomainName = "*";
        } else {
            maskedDomainName = domainName.charAt(0) + "****";
        }

        return maskedDomainName + extension;
    }

    private String partiallyMask(String value) {
        int length = value.length();

        if (length <= 2) {
            return FULL_MASK;
        }

        if (length <= 4) {
            return value.charAt(0) + "****";
        }

        if (length <= 8) {
            return value.substring(0, 2) + "****" + value.substring(length - 1);
        }

        return value.substring(0, 4) + "****" + value.substring(length - 4);
    }
}