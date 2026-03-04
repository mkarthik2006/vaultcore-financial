package com.vaultcore.core.fraud;

public class FraudChallengeRequiredException extends RuntimeException {

    private final String channel;

    public FraudChallengeRequiredException(String message, String channel) {
        super(message);
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}