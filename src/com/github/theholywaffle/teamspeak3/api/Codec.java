package com.github.theholywaffle.teamspeak3.api;

public enum Codec {
	CODEC_SPEEX_NARROWBAND(0),
	CODEC_SPEEX_WIDEBAND(1),
	CODEC_SPEEX_ULTRAWIDEBAND(2),
	CODEC_CELT_MONO(3), UNKNOWN(-1);

	private int i;

	Codec(int i) {
		this.i = i;
	}

	public int getIndex() {
		return i;
	}

}
