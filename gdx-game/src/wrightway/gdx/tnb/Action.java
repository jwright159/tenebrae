package wrightway.gdx.tnb;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import wrightway.gdx.*;
import org.luaj.vm2.*;

public abstract class Action implements Runnable{
	protected Character chara;
	protected boolean manualOverride;
	public boolean stop(boolean touched){
		return true;
	}
	
	public static class DelayAction extends Action{
		protected float delay;
		public DelayAction(Character chara, float delay, boolean tap){
			this.chara = chara;
			this.delay = delay;
			this.manualOverride = tap;
		}
		@Override
		public void run(){
			chara.delay += delay;
		}
		@Override
		public boolean stop(boolean touched){
			if(chara.delay == 0 || (manualOverride && touched)){
				chara.delay = 0; //redundant for interps
				return true;
			}
			return false;
		}
	}
	
	public static class DialogAction extends DelayAction{
		private String text;
		private boolean tap, tapDelay;//tap is whether we should wait for a tap, tapDelay is whether we can skip the delay and consecutive tapDelays
		public DialogAction(Character chara, String text, float delay, boolean tap, boolean tapDelay){
			super(chara, delay, tap);
			this.text = text;
			this.tap = tap;
			this.tapDelay = tapDelay;
		}
		@Override
		public void run(){
			super.run();
			Tenebrae.player.dialogBox.setText(text);
		}
		@Override
		public boolean stop(boolean touched){
			if((chara.delay == 0 && (!tap || (tap && touched))) || (chara.delay > 0 && tapDelay && touched)){
				if(tapDelay)
					while(chara.actions.size > 0 && chara.actions.get(0) instanceof DialogAction && ((DialogAction)chara.actions.get(0)).tapDelay)
						chara.actions.removeIndex(0);
				Tenebrae.player.dialogBox.setText(null);
				Log.verbose2("Ending Dialog!", chara.delay, tap, touched, tapDelay);
				return true;
			}
			return false;
		}
	}
	
	public static class FunctionAction extends Action{
		private LuaFunction func;
		public FunctionAction(LuaFunction funcToRun){
			func = funcToRun;
		}
		@Override
		public void run(){
			func.call();
		}
	}
	
	public static abstract class InterpAction extends DelayAction{
		protected Interpolation interp;
		protected float start, end;
		public InterpAction(Character chara, Interpolation interp, float time, float startValue, float endValue, boolean tap){
			super(chara, time, tap);
			this.interp = interp;
			this.start = startValue;
			this.end = endValue;
		}
		public abstract void mutate()//{var = get();}//bc floats are immutable
		public float get(){
			//Log.debug("InterpAction get!", interp, start, end, delay, player.delay);
			return interp.apply(start, end, delay == 0 ? 1 : (delay - chara.delay) / delay);
		}
		@Override
		public boolean stop(boolean touched){
			boolean rtn = super.stop(touched); //might set player.delay to 0
			mutate();
			return rtn;
		}
	}
	
	public static class CameraAction extends DelayAction{
		private float x, y, z, oldx, oldy, oldz;
		private Interpolation interpx, interpy, interpz;
		private OrthographicCamera cam;
		public CameraAction(OrthographicCamera cam, float x, float y, float z, Interpolation interpx, Interpolation interpy, Interpolation interpz, float time, boolean tap){
			super(Tenebrae.player, time, tap);
			this.cam = cam;
			oldx = cam.position.x;
			oldy = cam.position.y;
			oldz = cam.zoom;
			this.x = x;
			this.y = y;
			this.z = z;
			this.interpx = interpx;
			this.interpy = interpy;
			this.interpz = interpz;
		}
		CameraAction(OrthographicCamera cam, float x, float y, float z, Interpolation interp, float time, boolean tap){
			this(cam, x, y, z, interp, interp, interp, time, tap);
		}
		CameraAction(OrthographicCamera cam, float x, float y, Interpolation interp, float time, boolean tap){
			this(cam, x, y, cam.zoom, interp, interp, interp, time, tap);
		}
		CameraAction(OrthographicCamera cam, float z, Interpolation interp, float time, boolean tap){
			this(cam, cam.position.x, cam.position.y, z, interp, interp, interp, time, tap);
		}
		@Override
		public boolean stop(boolean touched){
			boolean rtn = super.stop(touched); //might set player.delay to 0
			float a = delay == 0 ? 1 : (delay - chara.delay) / delay;
			cam.position.x = interpx.apply(oldx, x, a);
			cam.position.y = interpy.apply(oldy, y, a);
			cam.zoom = interpz.apply(oldz, z, a);
			Tenebrae.t.updateZoom();
			return rtn;
		}
	}
	
	public static class MoveAction extends DelayAction{
		private float x, y, oldx, oldy;
		private Interpolation interp;
		private boolean relative, done;
		public MoveAction(Character chara, float x, float y, float speed, boolean relative, boolean tap){
			super(chara, speed, tap);
			this.x = x;
			this.y = y;
			interp = Interpolation.linear;
			this.relative = relative;
			//Log.verbose("To move!", x, y, speed);
		}
		@Override
		public void run(){
			oldx = chara.x;
			oldy = chara.y;
			if(relative){
				x += oldx;
				y += oldy;
			}
			delay = delay == 0 ? 0 : (float)Math.hypot(x - oldx, y - oldy) / delay;
			//Log.verbose("Moving!", oldx, oldy, x, y, delay);
			super.run();
		}
		@Override
		public boolean stop(boolean touched){
			//Log.verbose("Actually moving.", this, delay, oldx, oldy, x, y, done);
			if(done)
				return true;
			if(manualOverride && touched)
				chara.delay = 0;
			float a = delay == 0 ? 1 : (delay - chara.delay) / delay;
			chara.targetX = interp.apply(oldx, x, a);
			chara.targetY = interp.apply(oldy, y, a);
			done = chara.delay == 0;
			return false;
		}
	}
}
