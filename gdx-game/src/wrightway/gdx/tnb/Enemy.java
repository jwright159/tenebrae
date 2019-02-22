package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.utils.*;
import wrightway.gdx.tnb.MenuItem.GameItem;
import com.leff.midi.util.*;
import com.leff.midi.event.*;

public class Enemy extends Character{
	String enctrText;
	boolean fleeable;
	int turns;
	MidiWavSync music;
	ArrayMap<Integer,EnemyWeapon> weapons;

	Enemy(JVSValue.Function script, Scope parent, boolean lol){
		super("die");//super(parent);
		turns = 0;
		enemy = Tenebrae.player;

		name = "Enemy";
		box = new EntityBox(this, Tenebrae.t.getSkin());
		setStats(baseStats, 0, 0, 0, 0, 1, 1);
		exp = 0;
		g = 0;
		enctrText = "An enemy approaches!";
		fleeable = true;
		weapons = new ArrayMap<>();

		vars.put("setMusic", new Function(new String[]{"filename"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							music = Tenebrae.mp.loadMidiWav(scope.getVal("filename", String.class, null), new MidiEventListener(){
									@Override
									public void onStart(boolean fromBeginning){
										// TODO: Implement this method
									}
									@Override
									public void onEvent(MidiEvent event, long ms){
										EnemyWeapon weapon = weapons.get(((NoteOn)event).getChannel());
										if(weapon != null)
											weapon.spawn();
									}
									@Override
									public void onStop(boolean finished){
										// TODO: Implement this method
									}
								});
							return music;
						}
					}}));
		vars.put("channelWeapon", new Function(new String[]{"channel", "filename"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							EnemyWeapon weapon = Tenebrae.mp.loadEnemyWeapon(scope.getVal("filename", String.class, null), Enemy.this);
							int channel = scope.getVal("channel", Integer.class, null);
							weapons.put(channel, weapon);
							return weapon;
						}
					}}));
		vars.put("encounter", new JVSValue.WValue(){
				@Override
				public Object get(){
					return enctrText;
				}
				@Override
				public void put(Object value){
					enctrText = value.toString();
				}
			});
		vars.put("canFlee", new JVSValue.WValue(){
				@Override
				public Object get(){
					return fleeable;
				}
				@Override
				public void put(Object value){
					fleeable = value;
				}
			});

		script.get(vars, null);

		hp = maxhp();
		mp = maxmp();
		box.updateHP();

		//Tenebrae.debug("Enemy made! "+this);
	}

	public String battleText(){
		if(turns == 0)
			return enctrText;
		return enctrText;
	}
	@Override
	public void die(){
		vars.run("onDie");
		Tenebrae.player.endEncounter();
	}

	public void useItem(){
		float totChance = 0;
		for(GameItem item : items)
			totChance += item.eChance;
		float random = (float)Math.random() * totChance;
		totChance = 0;
		for(GameItem item : items){
			totChance += item.eChance;
			if(totChance > random){
				item.run("onUse");
				break;
			}
		}
	}

	@Override
	public void dispose(){
		Log.debug("Enemy disposed");
		super.dispose();
		music.dispose();
		for(EnemyWeapon weapon : weapons.values())
			weapon.dispose();
	}
}
