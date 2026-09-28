package com.quoth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Minimal PCM WAV reading and writing, so Quoth can decode, pitch and trim its blips in memory
 * and hand RuneLite's AudioPlayer a finished file. Everything inside Quoth is 16-bit mono at
 * {@link #RATE}.
 */
final class Wav
{
	static final int RATE = 44100;

	private static final int FORMAT_PCM = 1;
	private static final int FORMAT_EXTENSIBLE = 0xFFFE;

	private Wav()
	{
	}

	/**
	 * Decodes a PCM WAV (8, 16 or 24-bit, any channel count, any rate) to 16-bit little-endian
	 * mono at {@link #RATE}.
	 */
	static byte[] decode(byte[] wav) throws IOException
	{
		ByteBuffer b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
		if (wav.length < 12 || b.getInt(0) != 0x46464952 || b.getInt(8) != 0x45564157) // "RIFF", "WAVE"
		{
			throw new IOException("not a WAV file");
		}
		int format = -1;
		int channels = 0;
		int rate = 0;
		int bits = 0;
		int dataStart = -1;
		int dataLength = 0;
		int pos = 12;
		while (pos + 8 <= wav.length)
		{
			int id = b.getInt(pos);
			int size = b.getInt(pos + 4);
			int body = pos + 8;
			if (size < 0 || body + size > wav.length)
			{
				size = wav.length - body; // tolerate a truncated last chunk
			}
			if (id == 0x20746d66 && size >= 16) // "fmt "
			{
				format = b.getShort(body) & 0xffff;
				channels = b.getShort(body + 2) & 0xffff;
				rate = b.getInt(body + 4);
				bits = b.getShort(body + 14) & 0xffff;
				if (format == FORMAT_EXTENSIBLE && size >= 26)
				{
					format = b.getShort(body + 24) & 0xffff; // sub-format GUID starts with the format code
				}
			}
			else if (id == 0x61746164) // "data"
			{
				dataStart = body;
				dataLength = size;
			}
			pos = body + size + (size & 1); // chunks are padded to even length
		}
		if (format != FORMAT_PCM || dataStart < 0 || channels < 1 || rate <= 0
			|| (bits != 8 && bits != 16 && bits != 24))
		{
			throw new IOException("unsupported WAV: format=" + format + " bits=" + bits + " channels=" + channels);
		}

		int bytesPer = bits / 8;
		int frames = dataLength / (bytesPer * channels);
		byte[] mono = new byte[frames * 2];
		for (int f = 0; f < frames; f++)
		{
			int sum = 0;
			for (int c = 0; c < channels; c++)
			{
				int at = dataStart + (f * channels + c) * bytesPer;
				int s;
				if (bits == 8)
				{
					s = ((wav[at] & 0xff) - 128) << 8; // 8-bit WAV is unsigned
				}
				else if (bits == 16)
				{
					s = (short) ((wav[at] & 0xff) | (wav[at + 1] << 8));
				}
				else
				{
					s = ((wav[at] & 0xff) | ((wav[at + 1] & 0xff) << 8) | (wav[at + 2] << 16)) >> 8;
				}
				sum += s;
			}
			int s = Math.round(sum / (float) channels);
			mono[2 * f] = (byte) s;
			mono[2 * f + 1] = (byte) (s >> 8);
		}
		return rate == RATE ? mono : Resample.shift(mono, rate / (double) RATE);
	}

	/** Wraps 16-bit little-endian mono PCM at {@link #RATE} in a WAV header. */
	static byte[] encode(byte[] pcm)
	{
		ByteBuffer b = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(0x46464952).putInt(36 + pcm.length).putInt(0x45564157); // RIFF size WAVE
		b.putInt(0x20746d66).putInt(16).putShort((short) FORMAT_PCM).putShort((short) 1) // fmt, mono
			.putInt(RATE).putInt(RATE * 2).putShort((short) 2).putShort((short) 16);
		b.putInt(0x61746164).putInt(pcm.length).put(pcm); // data
		return b.array();
	}
}
