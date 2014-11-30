package com.github.rosjava_test.rosjava_audio_util;

import javax.sound.sampled.*;

public class MicTest extends Thread {

	public static final int SAMPLE_RATE = 8000;
	public static final int TIME_STEP = 500;
	public static final int FRAME = 2;
	public static final int BUFFER_SIZE = (int)(SAMPLE_RATE * (TIME_STEP/1000.0) * FRAME);
	private TargetDataLine targetDataLine;
	private SourceDataLine sourceDataLine;

	public MicTest() {
	}
	
	public void syncRead(byte[] buf, int start, int end){
		long tm1 = System.currentTimeMillis();
		if ( this.targetDataLine != null){
			this.targetDataLine.read(buf,start,end);
		}
		long tm2 = System.currentTimeMillis();
		if ( tm2 - tm1 > TIME_STEP ){
			System.out.println("[MicTest] syncRead overslept ... ");
		} else {
			try {
				Thread.sleep( TIME_STEP - (tm2 - tm1));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void syncWrite(byte[] buf, int start, int end){
		long tm1 = System.currentTimeMillis();
		if ( this.sourceDataLine != null){
			this.sourceDataLine.write(buf,start,end);
		}
		long tm2 = System.currentTimeMillis();
		if ( tm2 - tm1 > TIME_STEP ){
			System.out.println("[MicTest] syncRead overslept ... ");
		} else {
			try {
				Thread.sleep( TIME_STEP - (tm2 - tm1));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			MicTest m = new MicTest();
			m.setupSource();
			m.setupTarget();
			byte[] buffer = new byte[BUFFER_SIZE];
			while (true) {
				long tm1 = System.currentTimeMillis();
				int line = m.targetDataLine.read(buffer, 0, buffer.length);
				long tm2 = System.currentTimeMillis();
				if ((int) (0 * Math.random()) == 0)
					m.sourceDataLine.write(buffer, 0, buffer.length);
				long tm3 = System.currentTimeMillis();
				System.out.println("[" + (tm2 - tm1) +", " + (tm3 - tm2) + "] " + line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		if (this.sourceDataLine != null)
			this.sourceDataLine.close();
		if (this.targetDataLine != null)
			this.targetDataLine.close();
	}
	
	public AudioFormat genAudioFormat(){
//		return  new AudioFormat(
//				AudioFormat.Encoding.PCM_SIGNED, MicTest.SAMPLE_RATE, 8, 1, 1, MicTest.SAMPLE_RATE,
//				false);
		return  new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED, MicTest.SAMPLE_RATE, 8 *MicTest.FRAME, 1, MicTest.FRAME, MicTest.SAMPLE_RATE,
				false);

	}

	public TargetDataLine setupTarget() throws LineUnavailableException {
		AudioFormat audioFormat = this.genAudioFormat();

		DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class,
				audioFormat);
		this.targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
		this.targetDataLine.open(audioFormat);
		this.targetDataLine.start();
		return this.targetDataLine;
	}

	public SourceDataLine setupSource() throws LineUnavailableException {
		AudioFormat audioFormat = this.genAudioFormat();

		DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class,
				audioFormat);
		this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
		this.sourceDataLine.open(audioFormat);
		this.sourceDataLine.start();
		return this.sourceDataLine;
	}

}
