package net.trustgames.middleware.managers.rabbit.extras;

public enum RabbitHeaders {
    MESSAGE_TYPE("messageType");

    public final String value;

    RabbitHeaders(String value) {
        this.value = value;
    }
}
