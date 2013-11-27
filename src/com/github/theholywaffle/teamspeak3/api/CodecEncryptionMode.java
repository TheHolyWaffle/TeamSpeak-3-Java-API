package com.github.theholywaffle.teamspeak3.api;

public enum CodecEncryptionMode {

	CODEC_CRYPT_INDIVIDUAL(0),
	CODEC_CRYPT_DISABLED(1),
	CODEC_CRYPT_ENABLED(2),
	UNKNOWN(-1);

	private int i;

	CodecEncryptionMode(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
