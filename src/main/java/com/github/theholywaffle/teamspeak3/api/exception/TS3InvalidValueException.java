package com.github.theholywaffle.teamspeak3.api.exception;

import java.util.Optional;

/**
 * A ServerQuery response or event lacks a required value, or a value cannot be interpreted as
 * the requested type. The command itself succeeded; the raw value remains available from the model.
 */
public class TS3InvalidValueException extends TS3Exception {

	private static final long serialVersionUID = 1L;

	private final String property;
	private final String rawValue;

	/**
	 * @param property the property name
	 * @param rawValue the received value, or {@code null} if the property is missing
	 * @param expected a description of the expected value, such as {@code "an integer"}
	 * @param cause the parsing failure, or {@code null}
	 */
	public TS3InvalidValueException(String property, String rawValue, String expected, Throwable cause) {
		super(rawValue == null
			? "Missing required property " + property
			: "Property " + property + " is not " + expected + ": \"" + rawValue + "\"", cause);
		this.property = property;
		this.rawValue = rawValue;
	}

	/** @return the name of the missing or invalid property */
	public String getProperty() { return property; }

	/** @return the received value, or empty if the property is missing */
	public Optional<String> getRawValue() { return Optional.ofNullable(rawValue); }
}
