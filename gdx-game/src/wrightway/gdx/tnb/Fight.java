package wrightway.gdx.tnb;

import wrightway.gdx.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.input.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;

public class Fight extends WScreen{
	Player player;
	Enemy enemy;
	WActor.WTexture sprite;
	WActor.WRect board;

	public Fight(final Player player, final Enemy enemy, Stage hudstage){
		this.player = player;
		this.enemy = enemy;
		this.sprite = new WActor.WTexture(player){
			float px, py;
			@Override
			public void act(float delta){
				super.act(delta);
				
				enemy.music.sync();
				
				float delay = player.delay;
				if(delay > 0){
					//Tenebrae.debug("Delay "+delay+", delta "+delta);
					delay -= delta;
					if(delay < 0)
						delay = 0;
				}
				player.triggerAction(false);
				
				setX(Tenebrae.constrain(getX(), board.getX(), board.getX() + board.getWidth() - getTrueWidth()));
				setY(Tenebrae.constrain(getY(), board.getY(), board.getY() + board.getHeight() - getTrueHeight()));
				player.updateSkins((getX() - px)/player.map.tilewidth, (getY() - py)/player.map.tileheight);
				px = getX(); py = getY();
			}
		};
		sprite.sizeBy(-sprite.getWidth()/2, -sprite.getHeight()/2);
		this.board = null;
		sprite.setPosition(board.getX()+board.getWidth()/2, board.getY()+board.getHeight()/2, Align.center);
		worldStage.addActor(enemy);
		worldStage.addActor(board);
		worldStage.addActor(sprite);
		//enemy.setVisible(false);
		this.uiStage = hudstage;

		multiplexer.addProcessor(0, new InputAdapter(){
				@Override
				public boolean touchDown(int x, int y, int pointer, int button){
					Tenebrae.verbose("Pressed down!");
					return false;
				}
				@Override
				public boolean touchUp(int x, int y, int pointer, int button){
					Tenebrae.verbose("Pressed up!");
					return false;
				}
				@Override
				public boolean keyDown(int keycode){
					if(keycode == Input.Keys.BACK){
						//if(!player.performBack()){
						Gdx.app.exit();
						return true;
						//}
					}
					return false;
				}
			});
		multiplexer.addProcessor(2, new GestureDetector(new GestureDetector.GestureListener(){
					@Override
					public boolean touchDown(float p1, float p2, int p3, int p4){
						// TODO: Implement this method
						return false;
					}
					@Override
					public boolean tap(float p1, float p2, int p3, int p4){
						//Tenebrae.player.triggerBestTrigger();
						return false;
					}
					@Override
					public boolean longPress(float p1, float p2){
						// TODO: Implement this method
						return false;
					}
					@Override
					public boolean fling(float p1, float p2, int p3){
						// TODO: Implement this method
						return false;
					}
					@Override
					public boolean pan(float x, float y, float dx, float dy){
						sprite.moveBy(dx, -dy);
						return true;
					}
					@Override
					public boolean panStop(float p1, float p2, int p3, int p4){
						//updateZoom();
						return false;
					}
					@Override
					public boolean zoom(float origdist, float dist){
						// ((OrthographicCamera)worldStage.getCamera()).zoom = origdist / dist * zoom;
						return false;
					}
					@Override
					public boolean pinch(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4){
						// TODO: Implement this method
						return false;
					}
					@Override
					public void pinchStop(){
						// TODO: Implement this method
					}
				}));
		multiplexer.removeProcessor(1);
		multiplexer.addProcessor(1, hudstage);
	}

	@Override
	public void show(){
		super.show();
		enemy.music.play();
	}
	@Override
	public void hide(){
		super.hide();
		enemy.music.stop();
	}
	
	public void spawn(Projectile projectile){
		worldStage.addActor(projectile);
		//worldStage.addActor(projectile.debug());
		//Tenebrae.debug("Spawning projectile", projectile.toRect());
		Tenebrae.debug(projectile.region.get(0).getTexture());
	}

	@Override
	public void dispose(){
		uiStage = null;
		enemy.remove();
		super.dispose();
	}
}
