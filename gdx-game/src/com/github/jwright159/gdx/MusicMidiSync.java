package com.github.jwright159.gdx;

import com.badlogic.gdx.files.*;
import com.leff.midi.*;
import com.badlogic.gdx.audio.*;
import java.io.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.leff.midi.util.*;
import com.leff.midi.event.*;

public class MusicMidiSync implements Disposable{
	private MidiFile midi;
	private Music music;
	private MidiProcessor midipro;
	private float pMs;
	private boolean looping;
	
	public MusicMidiSync(Music music, MidiFile midi, MidiEventListener noteOn, MidiEventListener noteOff){
		this.music = music;
		looping = music.isLooping();
		music.setLooping(false);
		music.setOnCompletionListener(new Music.OnCompletionListener(){
				@Override
				public void onCompletion(Music p1){
					Log.audio("Completed music", MusicMidiSync.this, p1);
					stop();
					if(looping)
						play();
				}
			});
		this.midi = midi;
		if(midi != null){
			midipro = new MidiProcessor(midi);
			if(noteOn != null)
				midipro.registerEventListener(noteOn, NoteOn.class);
			if(noteOff != null)
				midipro.registerEventListener(noteOff, NoteOff.class);
		}
	}
	public MusicMidiSync(Music music, MidiFile midi, MidiEventListener noteOn){
		this(music, midi, noteOn, null);
	}
	public MusicMidiSync(Music music){
		this(music, null, null, null);
	}
	
	public void play(){
		music.play();
		if(midipro != null)
			midipro.start();
	}
	
	public void stop(){
		music.stop();
		if(midipro != null)
			midipro.reset();
	}
	
	public void pause(){
		music.pause();
		if(midipro != null)
			midipro.stop();
	}
	
	public void setPosition(float pos){
		music.setPosition(pos);
		sync();
	}
	public float getPosition(){
		return music.getPosition();
	}
	
	public void setVolume(float vol){
		music.setVolume(vol);
	}
	public float getVolume(){
		return music.getVolume();
	}
	
	public boolean isPlaying(){
		return music.isPlaying();
	}

	protected void sync(){
		if(midipro == null)
			return;
		
		float ms = music.getPosition()*1000;
		if(ms < pMs){
			midipro.reset();
			midipro.start();
		}
		midipro.setElapsed((long)ms);
		pMs = ms;
	}
	
	@Override
	public void dispose(){
		stop();
		if(midipro != null)
			midipro.unregisterAllEventListeners();
		music.dispose();
	}
}
