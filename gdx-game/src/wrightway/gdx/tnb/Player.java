package wrightway.gdx.tnb;

import wrightway.gdx.*;
import wrightway.gdx.JVSValue.Scope;
import wrightway.gdx.JVSValue.WObject;
import wrightway.gdx.JVSValue.Function;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.objects.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.*;
import wrightway.gdx.tnb.EntityBox.*;
import wrightway.gdx.tnb.MenuItem.*;
import wrightway.gdx.tnb.Action.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;

public class Player extends Character{
	TileMap map;
	boolean firstFrame;// True for first frame after map load, for skipping exittriggers (just call them manually if you need them instead of auto detection)
	float speedMult = 1;
	int lv = -1;
	ArrayMap<Float, ArrayMap<Stats, Float>> statTable;
	final static int actstep = 1;
	ButtonBox buttonBox;
	StatBox smolStatBox;
	Label dialogBox;
	Stack paneStack;
	Container<Stack> mapRect;
	String trueName;
	boolean expandDeltaRecord, wasExpandedBeforeDialog;
	Rectangle activeDeadzone;
	Array<RectangleMapObject> ptriggers;
	Vector3 lastCameraPos;
	static Rectangle dzRect, bigdzRect;
	Table table;

	public Player(){
		super("player");
		Log.verbose("Making Player");
		ptriggers = new Array<RectangleMapObject>();
		lastCameraPos = new Vector3();

		com.badlogic.gdx.scenes.scene2d.ui.Skin skin = Tenebrae.t.getSkin();
		table = Tenebrae.t.getTable();
		table.setDebug(Tenebrae.tableDebug);

		mapRect = new Container<Stack>().fill();
		Cell mapCell = table.add(mapRect).pad(Tenebrae.margin).growX();
		mapCell.height(Value.percentWidth(1f, mapRect));

		paneStack = new Stack();
		final Table pane = new Table(skin);
		pane.setDebug(Tenebrae.tableDebug);
		pane.background("window");
		pane.add(box = new PlayerBox(this, skin)).pad(Tenebrae.margin - pane.getBackground().getTopHeight(), Tenebrae.margin/*-pane.getBackground().getLeftWidth() i want the space here*/, Tenebrae.margin, Tenebrae.margin - pane.getBackground().getRightWidth()).grow().uniform();
		pane.row();
		pane.add(buttonBox = new ButtonBox(this, skin)).pad(0, Tenebrae.margin - pane.getBackground().getLeftWidth(), Tenebrae.margin - pane.getBackground().getBottomHeight(), Tenebrae.margin - pane.getBackground().getRightWidth()).grow().uniform();
		paneStack.add(pane);

		Stack menuStack = new Stack();
		mapRect.setActor(menuStack);
		menuStack.add(buttonBox.menu.box);
		menuStack.add(buttonBox.items.box);

		final Table diatable = new Table(skin);
		dialogBox = new Label("", skin, "dialog"){
			@Override
			public void setText(CharSequence text){
				Log.debug("Settext", getX(), getWidth());
				super.setText(text);
				if(text == null || text.length() == 0){
					setExpanded(wasExpandedBeforeDialog);
					wasExpandedBeforeDialog = false;
					setVisible(false);//hasDialog
					diatable.setVisible(false);
				}else{
					wasExpandedBeforeDialog = isExpanded();
					setExpanded(true);
					setVisible(true);
					diatable.setVisible(true);
				}
			}
		};
		dialogBox.setWrap(true);
		dialogBox.setAlignment(Align.topLeft);
		diatable.setDebug(Tenebrae.tableDebug);
		diatable.background("window");
		diatable.add(dialogBox).pad(Tenebrae.margin).grow();
		paneStack.add(diatable);

		table.row();
		table.add(paneStack).pad(Tenebrae.margin).padTop(0).grow();

		table.row();
		table.add(smolStatBox = new StatBox(box.healthBar, box.manaBar, skin)).expandX().fill().height(Tenebrae.margin * 2);

		vars.put("encounter", new Function(new String[]{"filename"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							Enemy enemy = Tenebrae.mp.loadEnemy(scope.getVal("filename", String.class, null));
							encounter(enemy);
							return enemy;
						}
					}}));
		vars.put("setStatLv", new Function(new String[]{"exp", "str", "intl", "def", "agl", "maxhp", "maxmp"}, new JVSValue[]{new JVSValue(){
						@Override
						public Object get(Scope scope){
							setStatLv(scope.getVal("exp", Float.class, null), scope.getVal("str", Float.class, null), scope.getVal("intl", Float.class, null), scope.getVal("def", Float.class, null), scope.getVal("agl", Float.class, null), scope.getVal("maxhp", Float.class, null), scope.getVal("maxmp", Float.class, null));
							return null;
						}
					}}));
		vars.remove("name");
		vars.put("name", new JVSValue.WValue(){
				@Override
				public Object get(){
					Log.debug("GetName", trueName);
					return trueName;
				}
				@Override
				public void put(Object value){
					String newName = value.toString();
					Log.debug("PutName", newName);
					setPlayerName(newName);
				}
			});
		vars.put("speed", new JVSValue.WValue(){
				@Override
				public Object get(){
					return speedMult;
				}
				@Override
				public void put(Object value){
					speedMult = value;
				}
			});

		statTable = new ArrayMap<Float, ArrayMap<Stats, Float>>();
		setPlayerName("Player");
		name = "You";
		setStats(baseStats, 0, 0, 0, 0, 0, 0);
		exp = 0;
		g = 0;
		box.updateHP();

		setExpanded(false);
	}
	@Override
	public void changeMap(TileMap map, float spawnx, float spawny){
		Log.debug("Changing map!", map, spawnx, spawny);
		firstFrame = true;
		if(this.map != null){
			this.map.dispose();
			this.map = null;
		}
		this.map = map;
		Tenebrae.t.getStage().addActor(map);

		super.changeMap(map, spawnx, spawny);
		for(NPC npc : Tenebrae.mp.npcs)
			npc.changeMap(map, -1, -1);

		setExpanded(isExpanded());

		//Log.debug("Changing player! " + toString());

		Tenebrae.mp.vars.run("onCreate");
	}

	public void setPlayerName(String name){
		trueName = name;
		box.name.setText(trueName);
		Log.debug("SetName", name, box.name.getText());
	}

	@Override
	public void move(float newX, float newY, float speed, boolean relative){
		//if(Tenebrae.fight != null)
		//	Tenebrae.fight.sprite.moveBy(newX, newY);
		//else
			super.move(newX, newY, speed, relative);
	}

	public static RectangleMapObject copy(RectangleMapObject o){
		RectangleMapObject r = new RectangleMapObject();
		r.setColor(o.getColor());
		r.setName(o.getName());
		r.setOpacity(o.getOpacity());
		r.setVisible(o.isVisible());
		r.getRectangle().set(o.getRectangle());
		r.getProperties().putAll(o.getProperties());
		return r;
	}

	public boolean isColliding(){
		for(int i = (int)x; i <= x + width; i++)
			for(int j = (int)y; j <= y + height; j++)
				if(isColliding(i, j))
					return true;
		return false;
	}
	public boolean isColliding(float tilex, float tiley){
		MapObjects objs = map.getCollisionObjects(tilex, tiley);
		if(objs != null){
			for(RectangleMapObject obj : objs)
				if(toTileRect().overlaps(obj.getRectangle())){
					Log.verbose2("Collision!", toTileRect(), obj.getRectangle());
					return true;
				}
		}
		return false;
	}
	public Vector2 getClosestCell(){
		int rx,ry;
		if(Math.ceil(x) - x >= (x + width) - Math.floor(x + width))
			rx = (int)x;
		else
			rx = (int)(x + width);
		if(Math.ceil(y) - y >= (y + height) - Math.floor(y + height))
			ry = (int)y;
		else
			ry = (int)(y + height);
		return new Vector2(rx, ry);
	}
	public MapObjects getCollidingTriggerObjects(){
		MapObjects rtn = map.getCollidingTriggerObjects(x * map.tilebasewidth, y * map.tilebaseheight, width * map.tilebasewidth, height * map.tilebaseheight);
		//Log.debug("Requesting triggers! " + rtn.getCount());
		return rtn;
	}
	public MapObjects getCollidingEnteranceObjects(){
		MapObjects rtn = map.getCollidingEnteranceObjects(x * map.tilebasewidth, y * map.tilebaseheight, width * map.tilebasewidth, height * map.tilebaseheight);
		for(MapObject obj : getCollidingNPCTriggerObjects("onEnter"))
			rtn.add(obj);
		//Log.debug("Requesting enters! " + rtn.getCount());
		return rtn;
	}
	public MapObjects getCollidingNPCTriggerObjects(String prop){
		MapObjects rtn = new MapObjects();
		Rectangle player = new Rectangle(x * map.tilebasewidth, y * map.tilebaseheight, width * map.tilebasewidth, height * map.tilebaseheight);
		for(NPC npc : Tenebrae.mp.npcs){
			MapObjects objs = npc.tile.get(0).getObjects();
			for(MapObject obj : objs){
				if(obj.getProperties().get(prop, "", String.class).isEmpty())
					continue;
				Log.verbose2("NPC trigger", npc, npc.tile.get(0).getId(), objs.getCount(), obj);
				if(obj != null){
					RectangleMapObject r = copy((RectangleMapObject)obj);
					Log.verbose2("NPC Raw", r.getRectangle());
					r.getRectangle().setPosition(npc.x * map.tilebasewidth + r.getRectangle().getX(), npc.y * map.tilebaseheight + r.getRectangle().getY());
					Log.verbose2("NPC Scaled", r.getRectangle());
					Log.verbose2("NPC Player", player);
					r.getProperties().put("__npc", npc);
					if(r.getRectangle().overlaps(player))
						rtn.add(r);
				}
			}
		}
		return rtn;
	}
	public RectangleMapObject getBestTrigger(){
		MapObjects objs = getCollidingTriggerObjects();
		for(MapObject obj : getCollidingNPCTriggerObjects("onTrigger"))
			objs.add(obj);
		RectangleMapObject rtn = null;
		Rectangle player = new Rectangle(x * map.tilebasewidth, y * map.tilebaseheight, width * map.tilebasewidth, height * map.tilebaseheight);
		float best = 0;
		Rectangle inter = new Rectangle();

		for(MapObject mapobj : objs){
			Log.verbose(mapobj);
			RectangleMapObject obj = (RectangleMapObject)mapobj;
			boolean disabled = obj.getProperties().get("disabled", false, java.lang.Boolean.class);
			Rectangle rect = obj.getRectangle();
			Log.verbose("Obj", rect);
			Log.verbose("Player", player);
			Intersector.intersectRectangles(rect, player, inter);
			Log.verbose("Intersect", inter);
			//Log.verbose("Intersecting triggers! " + obj.getName() + " " + rect + " " + player + " " + inter + " " + disabled);
			//rect.set(rect.getX() * map.tilebasewidth, rect.getY() * map.tilebaseheight, rect.getWidth() * map.tilebasewidth, rect.getHeight() * map.tilebaseheight);
			Log.verbose("Rescaled", rect);

			if(inter.area() > best && !disabled){
				best = inter.area();
				rtn = obj;
			}
		}

		return rtn;
	}
	public void triggerBestTrigger(){
		RectangleMapObject trig = getBestTrigger();
		Log.verbose("Requesting trigger! " + (trig != null ? trig.getName() : trig));
		if(trig != null){
			NPC npc = trig.getProperties().get("__npc", null, NPC.class);
			if(npc == null)
				Tenebrae.mp.vars.run(trig.getProperties().get("onTrigger", "", String.class));
			else
				npc.vars.run(trig.getProperties().get("onTrigger", "", String.class));
		}
	}

	public void addDialog(String dialog){
		addDialog(dialog, 0, true, false);
	}
	public void addDialog(String dialog, float delay){
		addDialog(dialog, delay, false, false);
	}
	public void addDialog(String dialog, float delay, boolean tap, boolean tapDelay){
		Log.verbose2("New dialog!", '"' + dialog + '"', delay, tap, tapDelay);
		addAction(new DialogAction(this, dialog, delay, tap, tapDelay));
	}

	@Override
	public void triggerAction(){
		triggerAction(false);
	}
	public void triggerAction(boolean touched){
		Log.verbose2("Wanting an action! Was " + currentAction);
		if(currentAction != null && currentAction.stop(touched)){
			delay = 0;
			currentAction = null;
		}
		if(actions.size == 0 || !Tenebrae.doneLoading || delay != 0 || (currentAction != null && !currentAction.stop(touched))){
			//Log.debug("..But nobody came.");
			return;
		}
		//Log.debug("Iterate!");
		currentAction = actions.removeIndex(0);
		if(currentAction != null){
			currentAction.run();
			Log.verbose2("Current action!", currentAction, delay, currentAction.manualOverride);
			if(delay != 0 || (currentAction != null && currentAction.manualOverride))
				;//map.cover();
			else if(currentAction == null)//on loading maps, currentAction gets nulled by loading of new map's scripts
				return;
			else
				triggerAction(false);
		}else{
			//Log.debug("Trigger is null!");
			triggerAction(touched);
		}
	}
	public boolean performBack(){
		//Log.debug("Performing back!");
		if(buttonBox.getActiveBox() != null){
			MenuBox openedBox = buttonBox.getActiveBox();
			MenuBox active = openedBox.findActiveLeaf();
			//Log.debug("Backing up from "+active+"!");
			active.setVisible(false);
			if(buttonBox.getActiveBox() == null)
				setUiDisabled(false);
			return true;
		}else if(currentAction != null){
			triggerAction(true);
			if(currentAction == null || enemy == null)
				return true;
		}else if(isExpanded()){
			setExpanded(false);
			return true;
		}
		//Log.debug("But you are backed against the wall.");
		return false;
	}
	public void closeMenus(){
		while(buttonBox.getActiveBox() != null)
			performBack();
	}

	public boolean isExpanded(){
		return paneStack.isVisible();
	}
	public void setExpanded(boolean expanded){
		activeDeadzone = expanded ? dzRect : bigdzRect;
		//Log.debugRect.orient(); //when the debugrect is for deadzone
		paneStack.setVisible(expanded);
		//Log.debug("Expanding! " + expanded);
		//smolStatBox.setVisible(!expanded);
	}
	public void setUiDisabled(boolean disabled){
		buttonBox.menu.setDisabled(disabled);
		buttonBox.items.setDisabled(disabled);
		smolStatBox.setDisabled(disabled);
	}

	public void addG(float g){
		this.g += g;
	}

	public void addExp(float exp){
		addExp(exp, false);
	}
	public void addExp(float exp, boolean silent){
		this.exp += exp;
		checkExpLv(true);
	}
	public void checkExpLv(boolean silent){
		Log.verbose("Checking exp lv!");
		int newlv = 0;
		for(int i = 0; i < statTable.size; i++){
			Log.verbose("StatTable exp! " + i + " " + statTable.getKeyAt(i));
			if(statTable.getKeyAt(i) > exp){
				newlv = i - 1;
				break;
			}
		}
		Log.debug("Levels: " + lv + " -> " + newlv);
		for(int i = lv + 1; i <= newlv; i++){
			if(!silent)
				addDialog("You have gone up a level! (" + (lv + 1) + " -> " + (i + 1) + ")");
			setStats(i);
		}
		finishAffect();
	}
	public void setStats(int newlv){
		Log.debug("Setting a lv! " + newlv);
		/*float n = newlv < lv ? -1 : 1;
		 lv = newlv;
		 ArrayMap<Stats,Float> paststats = stats.get(baseStats);
		 ArrayMap<Stats,Float> stats = statTable.getValueAt(newlv);
		 Log.debug("Past and new stats! " + paststats + " " + stats);
		 setStats(baseStats, n * stats.get(Stats.str) + paststats.get(Stats.str), n * stats.get(Stats.intl) + paststats.get(Stats.intl), n * stats.get(Stats.def) + paststats.get(Stats.def), n * stats.get(Stats.agl) + paststats.get(Stats.agl), n * stats.get(Stats.maxhp) + paststats.get(Stats.maxhp), n * stats.get(Stats.maxmp) + paststats.get(Stats.maxmp));
		 heal(maxhp() - hp, true, true);
		 invigor(maxmp() - mp, true, true);*/
		float maxhp = maxhp(), maxmp = maxmp();
		ArrayMap<Stats,Float> stats = statTable.getValueAt(newlv);
		setStats(baseStats, stats.get(Stats.str), stats.get(Stats.intl), stats.get(Stats.def), stats.get(Stats.agl), stats.get(Stats.maxhp), stats.get(Stats.maxmp));
		heal(maxhp() - maxhp, true, true);
		invigor(maxmp() - maxmp, true, true);
		Log.debug("setStats", maxhp, maxhp());
	}
	public void setStatLv(float exp, float str, float intl, float def, float agl, float maxhp, float maxmp){
		Log.verbose("Got a new statlv! " + maxhp);
		ArrayMap<Stats, Float> stats = new ArrayMap<Stats, Float>();
		stats.put(Stats.str, str);
		stats.put(Stats.intl, intl);
		stats.put(Stats.def, def);
		stats.put(Stats.agl, agl);
		stats.put(Stats.maxhp, maxhp);
		stats.put(Stats.maxmp, maxmp);
		statTable.put(exp, stats);
		checkExpLv(true);
	}
	public float expNow(){
		if(statTable.size == 0)
			return 0;

		for(int i = 0; i < statTable.size; i++)
			if(statTable.getKeyAt(i) > exp)
				return exp - (i != 0 ? statTable.getKeyAt(i - 1) : 0);

		return statTable.getKeyAt(statTable.size - 1);
	}
	public float expAtNextLv(){
		if(statTable.size == 0)
			return 0;
		for(int i = 0; i < statTable.size; i++)
			if(statTable.getKeyAt(i) > exp)
				return statTable.getKeyAt(i) - statTable.getKeyAt(i - 1);
		return statTable.getKeyAt(statTable.size - 1);
	}

	@Override
	public void finishAffect(){
		box.updateHP();
		smolStatBox.updateHP();
		if(isDead())
			die();
	}

	/*
	 //NEED SOMETHING TO RUN W/O PRESSING ANYTHING BC THIS HURT TEXT AND GRAPHIC ARE AT DIFF TIMES
	 //ARRRRGH CAN'T USE TRIGGERACTION() BC WE'RE IN THE MIDDLE OF A TRIGGER, FAILS CHECK!
	 //Runnable is a Java class... extend it?
	 //BOI GET REKT!
	 //Hmm... don't think I need the act list to be SeqActs, just RunActs
	 //Wait, I already have a class for this! The Item class, which used to be BoolRun. Going back to that then
	 //Ended up subclassing BoolRun to Item anyway
	 //Almost there
	 //So when superfunc "triggers", just adds to queue, doesn't run thus doesn't get thru
	 //... I could rebuild my triggering system to not use actions and fire rn. Risky, but worth a shot
	 //THAT WORKED. Kinda. Still doing it. But it's not trash! :D
	 //GET. REKT. BOI. I'VE DONE IT BY REWRITING THE ACTIONS TO NOT USE THEM
	 //*really far in the future* wow what is this. i dont even use actions anymore, just run scripts.
	 //okay so i /might/ have rewritten actions again, but its still mostly just run on the fly
	 //News flash: rewriting actions again to run simultaneously for camera movements (not even doing anything with enemies, why is this here)
	 //News flash 2: not doing that, just adding more generalized actions. might still be a good idea tho.
	 //Most of this convo is invalid, not using this turn-based thing anymore. Keeping this for postarity
	 */

	public void encounter(Enemy enemy){
		this.enemy = enemy;
		//Tenebrae.setFight(new Fight(this, enemy, Tenebrae.t.uiStage));
	}
	public void endEncounter(){
		//Tenebrae.removeFight();
		addExp(enemy.exp);
		enemy.endSelf();
		this.enemy = null;
	}

	@Override
	public void die(){
		Log.debug("Dead.");
		addDialog("You have died! :(");
		addAction(new Action(){public void run(){
					Tenebrae.t.loadSave();
				}});
	}

	@Override
	public void addItem(GameItem item){
		super.addItem(item);
		EntityBox.addItemToBox(item, buttonBox.items.box);
	}
	@Override
	public void removeItem(GameItem item){
		super.removeItem(item);
		buttonBox.items.box.removeOption(item.option);
		removeStats(item.type);
		equippedItems.removeKey(item.type);
	}

	public void constrain(){
		x = MathUtils.clamp(x, 0f, map.width - width);
		y = MathUtils.clamp(y, 0f, map.height - height);
	}
	private Rectangle camRect = new Rectangle(), dz = new Rectangle(), dzr = new Rectangle();
	public void moveCamera(){
		//Log.debug("Moving camera! currentAction", currentAction);
		if(currentAction != null || firstFrame)
			return;

		OrthographicCamera cam = Tenebrae.t.getCamera();
		camRect.set(cam.position.x - Tenebrae.screenRect.width * cam.zoom / 2, cam.position.y - Tenebrae.screenRect.height * cam.zoom / 2, Tenebrae.screenRect.width * cam.zoom, Tenebrae.screenRect.height * cam.zoom);
		dz.set(activeDeadzone.x * cam.zoom, activeDeadzone.y * cam.zoom, activeDeadzone.width * cam.zoom, activeDeadzone.height * cam.zoom);
		Log.verbose2("CamRect:", camRect, "Cam:", cam);

		//float smolestWidth = Math.min(dz.width, dz.height);
		float dzx = (dz.width / 2 - getTrueWidth() / 2) * Tenebrae.deadzone, dzy = (dz.height / 2 - getTrueHeight() / 2) * Tenebrae.deadzone;
		dzr.set(dz.x + dzx, dz.y + dzy, dz.width - dzx * 2, dz.height - dzy * 2);
		camRect.x = MathUtils.clamp(camRect.x, getX() + getTrueWidth()  - (dzr.x + dzr.width),  getX() - dzr.x);
		camRect.y = MathUtils.clamp(camRect.y, getY() + getTrueHeight() - (dzr.y + dzr.height), getY() - dzr.y);
		Log.verbose2("DZ:", dz, "DZR:", dzr, "CamRect:", camRect, "Player:", toRect());

		float d = map.getWidth() <= dz.width ? map.getWidth() / 2 - dz.width / 2 - dz.x : MathUtils.clamp(camRect.x, -dz.x, map.getWidth() - (dz.x + dz.width));
		cam.position.x = d + camRect.width / 2;
		Log.verbose2("x:", d, "Mapw:", map.getWidth(), "Cam:", cam);

		d = map.getHeight() <= dz.height ? map.getHeight() / 2 - dz.height / 2 - dz.y : MathUtils.clamp(camRect.y, -dz.y, map.getHeight() - (dz.y + dz.height));
		cam.position.y = d + camRect.height / 2;
		Log.verbose2("cr.y", camRect.y, "dz.y", dz.y, "dz.h", dz.height, "top", camRect.y + (dz.y + dz.height), "map.h", map.getHeight());
		Log.verbose2("y:", d, "Maph:", map.getHeight(), "Cam:", cam);

		lastCameraPos.set(cam.position.x, cam.position.y, cam.zoom);
	}

	@Override
	public void act(float delta){
		boolean moved = targetX != -1 || targetY != -1;
		super.act(delta);

		if(dzRect == null && !firstFrame){
			dzRect = new Rectangle(mapRect.getX(), mapRect.getY(), mapRect.getWidth(), mapRect.getHeight());
			bigdzRect = new Rectangle(paneStack.getX(), paneStack.getY(), paneStack.getWidth(), paneStack.getHeight() + Tenebrae.margin + mapRect.getHeight());
			Log.debug("MapRect!", dzRect, bigdzRect);
			setExpanded(isExpanded());
		}

		if(moved || firstFrame){
			/*float rand = (float)Math.random();
			 float chance = speed * 1f;
			 if(rand < chance){
			 WScreen.b = 1f;//VISUAL NOTICE FOR DEBUGGING
			 Vector2 cellvec = getClosestCell();
			 Enemy enemy = map.getTileEnemy(cellvec.x, cellvec.y);
			 //Log.debug("Battle! " + rand + ", " + chance+", "+enemy);
			 if(enemy != null){
			 encounter(enemy);
			 }
			 }*/

			MapObjects triggersInMapObj = getCollidingEnteranceObjects();
			Array<RectangleMapObject> triggersIn = new Array<RectangleMapObject>();
			for(RectangleMapObject obj : triggersInMapObj)
				triggersIn.add(obj);
			for(RectangleMapObject obj : triggersIn){
				Log.verbose2("Found an enter/exit object inside! " + obj.getName());
				if(!ptriggers.contains(obj, true) && !obj.getProperties().get("onEnter", "", String.class).isEmpty()){
					Log.verbose2("It was an enter object!");
					(obj.getProperties().containsKey("__npc") ? ((NPC)obj.getProperties().get("__npc")).vars : Tenebrae.mp.vars).run(obj.getProperties().get("onEnter", "", String.class));
				}
			}
			if(!firstFrame)
				for(RectangleMapObject obj : ptriggers){
					Log.verbose2("Found an enter/exit object outside! " + obj.getName());
					if(!triggersIn.contains(obj, true) && !obj.getProperties().get("onExit", "", String.class).isEmpty()){
						Log.verbose2("It was an exit object!");
						(obj.getProperties().containsKey("__npc") ? ((NPC)obj.getProperties().get("__npc")).vars : Tenebrae.mp.vars).run(obj.getProperties().get("onExit", "", String.class));
					}
				}
			ptriggers = triggersIn;
		}

		moveCamera();

		firstFrame = false;
	}
	@Override
	public void doMovement(){
		if(targetX != -1 || targetY != -1){
			float adx = (targetX - x) / actstep, ady = (targetY - y) / actstep;
			targetX = targetY = -1;
			float ppx = x, ppy = y;
			for(int i = 0; i < actstep; i++){
				float px = x, py = y;
				x += adx;
				constrain();
				if(isColliding() && currentAction == null)
					x = px;
				y += ady;
				constrain();
				if(isColliding() && currentAction == null)
					y = py;
			}
			updateSkins(x - ppx, y - ppy);
		}else{
			updateSkins(0, 0);
		}

		setPosition(x * map.tilewidth, y * map.tilewidth);
	}

	@Override
	public void endSelf(){
		super.endSelf();
		table.remove();
	}
}
