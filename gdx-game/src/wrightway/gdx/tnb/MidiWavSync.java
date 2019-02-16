package wrightway.gdx.tnb;

import com.badlogic.gdx.files.*;
import com.leff.midi.*;
import com.badlogic.gdx.audio.*;
import java.io.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.leff.midi.util.*;
import com.leff.midi.event.*;
import wrightway.gdx.*;

public class MidiWavSync implements Disposable{
	private MidiFile midi;
	private Music wav;
	private MidiProcessor midipro;
	private float pMs;
	
	public MidiWavSync(FileHandle midifile, FileHandle wavfile, MidiEventListener noteOn, MidiEventListener noteOff){
		try{
			midi = new MidiFile(midifile.file());
		}catch(FileNotFoundException|IOException e){
			Log.error("Didn't find midi file.", midifile);
		}
		wav = Gdx.audio.newMusic(wavfile);
		midipro = new MidiProcessor(midi);
		if(noteOn != null)
			midipro.registerEventListener(noteOn, NoteOn.class);
		if(noteOff != null)
			midipro.registerEventListener(noteOff, NoteOff.class);
	}
	public MidiWavSync(FileHandle midifile, FileHandle wavfile, MidiEventListener noteOn){
		this(midifile, wavfile, noteOn, null);
	}
	
	public void play(){
		wav.play();
		midipro.start();
	}
	
	public void stop(){
		wav.stop();
		midipro.reset();
	}
	
	public void pause(){
		wav.pause();
		midipro.stop();
	}

	public void sync(){
		float ms = wav.getPosition();
		if(ms < pMs){
			midipro.reset();
			midipro.start();
		}
		midipro.setMsElapsed((long)(ms*1000));
		pMs = ms;
	}
	
	@Override
	public void dispose(){
		wav.dispose();
	}
}
