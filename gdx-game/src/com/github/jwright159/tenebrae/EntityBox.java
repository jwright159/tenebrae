package com.github.jwright159.tenebrae;

import com.github.jwright159.gdx.*;
import com.github.jwright159.gdx.actor.*;
import com.github.jwright159.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.scenes.scene2d.actions.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import org.luaj.vm2.*;
import java.io.*;

public class EntityBox extends Table{
	private Image icon;
	public HealthBar healthBar, manaBar;
	public Label name;
	protected Table bars;
	public Character parent;
	public EntityBox(Character parent, boolean vertical, Skin skin){
		super(skin);
		this.parent = parent;
		this.setDebug(Tenebrae.TABLEDEBUG);
		Table pic = new Table(skin);
		pic.setDebug(Tenebrae.TABLEDEBUG);
		pic.add(icon = new Image(parent.getDrawable())).size(128, 128);
		pic.row();
		pic.add(name = new Label(parent.name, skin));
		this.add(pic).pad(0, 0, vertical ? Tenebrae.MARGIN : 0, vertical ? 0 : Tenebrae.MARGIN);
		if(vertical) this.row();
		bars = new Table(skin);
		bars.setDebug(Tenebrae.TABLEDEBUG);
		bars.add(healthBar = new HealthBar("HP", vertical, skin, "health")).grow();
		if(!vertical) bars.row();
		bars.add(manaBar = new HealthBar("MP", vertical, skin, "mana")).grow();
		this.add(bars).grow();
	}
	public void updateHP(){
		healthBar.setHealth(parent.hp, parent.maxhp());
		manaBar.setHealth(parent.mp, parent.maxmp());
	}

	@SuppressWarnings("unchecked")
	public static ArrayMap sortMap(ArrayMap map){
		Array keys = new Array();
		for(int i = 0; i < map.size; i++)
			keys.add(map.getKeyAt(i));
		keys.sort();
		ArrayMap rtn = new ArrayMap();
		for(int i = 0; i < keys.size; i++)
			rtn.put(keys.get(i), map.get(keys.get(i)));
		return rtn;
	}

	public static class PlayerBox extends EntityBox{
		public LevelBar levelBar;
		public PlayerBox(Player player, Skin skin){
			super(player, true, skin);
			name.setText(player.trueName);
			bars.add(levelBar = new LevelBar(true, skin)).grow();
		}
		@Override
		public void updateHP(){
			Player p = (Player)parent;
			super.updateHP();
			Log.verbose("Lv pts! " + p.expNow());
			levelBar.setHealth(p.lv, p.expNow(), p.expAtNextLv());
		}
	}

	public static class SizableProgressBar extends ProgressBar{
		private boolean round;
		private float position;
		public SizableProgressBar(float min, float max, float step, boolean vertical, Skin skin){
			super(min, max, step, vertical, skin);
		}
		@Override
		public float getPrefWidth(){
			return 0;
		}
		@Override
		public float getPrefHeight(){
			return 0;
		}
		@Override
		public void setRound(boolean round){
			super.setRound(round);
			this.round = round;
		}
		@Override
		protected float getKnobPosition(){
			return position;
		}
		@Override
		public void draw(Batch batch, float parentAlpha){
			ProgressBarStyle style = getStyle();
			boolean disabled = isDisabled();
			final Drawable knob = getKnobDrawable();
			final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
			final Drawable knobBefore = (disabled && style.disabledKnobBefore != null) ? style.disabledKnobBefore : style.knobBefore;
			final Drawable knobAfter = (disabled && style.disabledKnobAfter != null) ? style.disabledKnobAfter : style.knobAfter;

			Color color = getColor();
			float x = getX();
			float y = getY();
			float width = getWidth();
			float height = getHeight();
			float percent = getVisualPercent();

			batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

			if(isVertical()){
				float positionHeight = height;
				float knobHeight = knob == null ? 0 : knob.getMinHeight();

				float bgTopHeight = 0, bgBottomHeight = 0;
				if(bg != null){
					bg.draw(batch, x, y, width, height);
					bgTopHeight = bg.getTopHeight();
					bgBottomHeight = bg.getBottomHeight();
					positionHeight -= bgTopHeight + bgBottomHeight;
				}

				float knobHeightHalf = 0;
				if(knob == null){
					knobHeightHalf = knobBefore == null ? 0 : knobBefore.getMinHeight() * 0.5f;
					position = (positionHeight - knobHeightHalf) * percent;
					position = Math.min(positionHeight - knobHeightHalf, position);
				}else{
					knobHeightHalf = knobHeight * 0.5f;
					position = (positionHeight - knobHeight) * percent;
					position = Math.min(positionHeight - knobHeight, position) + bgBottomHeight;
				}
				position = Math.max(Math.min(0, bgBottomHeight), position);

				if(knobBefore != null){
					knobBefore.draw(batch, x, y + bgTopHeight, width, position + knobHeightHalf);
				}
				if(knobAfter != null){
					knobAfter.draw(batch, x, y + position + knobHeightHalf, width, height - position - knobHeightHalf);
				}
				if(knob != null){
					knob.draw(batch, x, y + position, width, knobHeight);
				}
			}else{
				float positionWidth = width;
				float knobWidth = knob == null ? 0 : knob.getMinWidth();

				float bgLeftWidth = 0, bgRightWidth = 0;
				if(bg != null){
					bg.draw(batch, x, y, width, height);
					bgLeftWidth = bg.getLeftWidth();
					bgRightWidth = bg.getRightWidth();
					positionWidth -= bgLeftWidth + bgRightWidth;
				}

				float knobWidthHalf = 0;
				if(knob == null){
					knobWidthHalf = knobBefore == null ? 0 : knobBefore.getMinWidth() * 0.5f;
					position = (positionWidth - knobWidthHalf) * percent;
					position = Math.min(positionWidth - knobWidthHalf, position);
				}else{
					knobWidthHalf = knobWidth * 0.5f;
					position = (positionWidth - knobWidth) * percent;
					position = Math.min(positionWidth - knobWidth, position) + bgLeftWidth;
				}
				position = Math.max(Math.min(0, bgLeftWidth), position);

				if(knobBefore != null){
					knobBefore.draw(batch, x + bgLeftWidth, y, position + knobWidthHalf, height);
				}
				if(knobAfter != null){
					knobAfter.draw(batch, x + position + knobWidthHalf, y, width - position - knobWidthHalf, height);
				}
				if(knob != null){
					knob.draw(batch, x + position, y, knobWidth, height);
				}
			}
		}
	}

	public static class TextBar extends Stack{
		protected ProgressBar bar;
		public Label text;
		public TextBar(String text, float min, float max, float step, boolean vertical, Skin skin, String skinName){
			this(text, min, max, step, vertical, skin);
			if(skinName != null && skin.has(skinName, ProgressBar.ProgressBarStyle.class))
				bar.setStyle(skin.get(skinName, ProgressBar.ProgressBarStyle.class));
		}
		public TextBar(String text, float min, float max, float step, boolean vertical, Skin skin){
			setDebug(Tenebrae.TABLEDEBUG);
			this.add(bar = new SizableProgressBar(min, max, step, vertical, skin));
			this.add(new Container<Label>(this.text = new Label(text, skin, "bar")).padLeft(Tenebrae.MARGIN / 2f).padRight(Tenebrae.MARGIN / 2f).fill());
			this.text.setAlignment(Align.left);
		}
	}
	public static class HealthBar extends TextBar{
		public String name;
		public float value, max;//cant change max in bar
		public HealthBar(String name, boolean vertical, Skin skin, String skinName){
			super(name, 0, 1, 0.01f, vertical, skin, skinName);
			this.name = name;
		}
		public void setHealth(float value, float max){
			this.value = value;
			this.max = max;
			bar.setValue(value / max);
			text.setText(toString());
		}
		@Override
		public String toString(){//bc why not
			if(!bar.isVertical())
				return name + ": " + f(value) + "/" + f(max);
			else
				return name + "\n" + f(value) + "\n—\n" + f(max);
		}
		public static String f(float val){
			return String.format("%.0f", val);
		}
	}
	public static class LevelBar extends HealthBar{
		private int lv;
		public LevelBar(boolean vertical, Skin skin){
			super("LV", vertical, skin, "level");
		}
		public void setHealth(int lv, float value, float max){
			this.lv = lv;
			setHealth(value, max);
		}
		@Override
		public String toString(){
			if(!bar.isVertical())
				return "LV: " + (lv + 1) + "; EX: " + f(value) + "/" + f(max);
			else
				return "LV\n" + (lv + 1) + "\n\nEX\n" + f(value) + "\n—\n" + f(max);
		}
	}
	public static class MiniHealthBar extends HealthBar{
		private HealthBar hbar;
		public MiniHealthBar(HealthBar bar, Skin skin){
			super("", false, skin, skin.find(bar.bar.getStyle()));
			this.hbar = bar;
			this.text.remove();
		}
		public void setHealth(){
			setHealth(hbar.value, hbar.max);
		}
	}

	public static class MenuOption extends Button implements Comparable<MenuOption>{//Not TextButton bc the padding widens the button
		public MenuBox box;
		public Label text;
		public MenuOption(String textStr, MenuBox box, int align, boolean enabled, Skin skin){
			super(skin);
			this.box = box;
			if(box != null)
				box.setVisible(false);
			text = new Label(textStr, skin);
			text.setAlignment(align);
			text.setWrap(true);
			setClip(true);
			addActor(text);
			//text.setEllipsis(true);
			setDisabled(!enabled);
			addListener(new ChangeListener(){
					@Override
					public void changed(ChangeEvent event, Actor a){
						open();
					}
				});
		}
		public MenuOption(String text, MenuBox box, Skin skin){
			this(text, box, Align.topLeft, true, skin);
		}
		public boolean isEmpty(){
			Log.verbose2("Is " + this + " empty? " + (box != null ? box.list.size : -1));
			if(box == null)
				return false;
			for(MenuOption opt : box.list)
				if(opt.isOpt())
					return false;
			return true;
		}
		public boolean isOpt(){
			return !(MenuBox.isEmpty(this) || MenuBox.isNextPage(this) || MenuBox.isPrevPage(this) || isEmpty());
		}
		public void open(){
			if(box != null){
				box.setVisible(true);
				box.setFocusTableToActive();
			}
		}
		public String getText(){
			return text.getText().toString();
		}
		@Override
		public float getPrefWidth(){
			return 0;
		}
		@Override
		public float getPrefHeight(){
			return 0;
		}
		@Override
		public void layout(){
			super.layout();
			Drawable bg = getBackground();
			text.setBounds(
				bg.getLeftWidth() + Tenebrae.MARGIN * 0.5f,
				bg.getBottomHeight() + Tenebrae.MARGIN * 0.25f,
				getWidth() - bg.getLeftWidth() - bg.getRightWidth() - Tenebrae.MARGIN,
				getHeight() - bg.getBottomHeight() - bg.getTopHeight() - Tenebrae.MARGIN * 0.5f
			);
		}
		@Override
		public int compareTo(MenuOption p1){
			return getText().compareToIgnoreCase(p1.getText());
		}
		@Override
		public String toString(){
			return super.toString() + "§" + getText();
		}
	}
	/*public static void clipWidth(final Actor bounds, final Container cell, float pad){
	 cell.setClip(true);
	 if(pad != -1)cell.padLeft(pad).padRight(pad);
	 cell.width(new Value(){
	 @Override
	 public float get(Actor a){
	 return bounds.getWidth() - cell.getPadX();
	 }
	 });
	 }
	 public static void clipHeight(final Actor bounds, final Container cell, float pad){
	 cell.setClip(true);
	 if(pad != -1)cell.padTop(pad).padBottom(pad);
	 cell.height(new Value(){
	 @Override
	 public float get(Actor a){
	 return bounds.getHeight() - cell.getPadY();
	 }
	 });
	 }
	 public static void clipWidth(final Actor bounds, final Cell cell, float pad){
	 if(pad != -1)cell.padLeft(pad).padRight(pad);
	 cell.width(new Value(){
	 @Override
	 public float get(Actor a){
	 return bounds.getWidth() - cell.getPadX();
	 }
	 });
	 }*/
	public static class MenuBox extends Stack{
		private Tenebrae game;
		public static float padding = 30f;
		public static int itemsPerHeight = 5;
		private int activePage;
		private String id;
		private Array<MenuOption> list;
		private Skin skin;
		private Array<FocusTable> pages;
		public MenuBox(Tenebrae game, String id, Skin skin){
			this.game = game;
			this.skin = skin;
			list = new Array<MenuOption>();
			pages = new Array<FocusTable>();
			this.id = id;
		}

		public void addOption(MenuOption opt){
			Log.verbose2("Adding option " + opt + " to " + this + "!");
			if(list.contains(opt, true))
				return;
			list.add(opt);
			reorganize();
		}
		public boolean removeOption(MenuOption opt){
			boolean removed = false;
			for(int i = 0; i < list.size; i++){
				if(list.get(i) == opt || (list.get(i).box != null && list.get(i).box.removeOption(opt))){
					removed = true;
					opt.remove();
					break;
				}
			}
			if(removed)
				reorganize();
			return removed;
		}
		public void removeOptions(){
			for(MenuOption opt : list)
				opt.remove();
		}

		public void reorganize(){
			Log.verbose2("Reorganizing " + this + "!");
			if(list.size == 0)
				return;

			for(int i = 0; i < list.size; i++){
				Log.verbose2("Found " + (isPrevPage(list.get(i)) ?"prevPage": isNextPage(list.get(i)) ?"nextPage": isEmpty(list.get(i)) ?"empty": "an option") + "!");
				Log.verbose2("Found " + list.get(i) + "! " + (list.get(i).box != null ?"" + list.get(i).box.list.size: "no box"));
				if(!list.get(i).isOpt()){
					Log.verbose2("Removing " + list.get(i) + "!");
					list.removeIndex(i--).remove();
				}
			}

			sortList();
			int listLengthAfter = list.size == 0 ? itemsPerHeight * 2 : ((list.size - 1) / ((itemsPerHeight - 1) * 2) + 1) * itemsPerHeight * 2;
			Log.verbose("List Lengths! Current: " + list.size + ", After: Got " + listLengthAfter + ", expected a multiple of " + itemsPerHeight * 2 + "!");
			for(int i = 0; i < listLengthAfter; i++){
				String debug = "an option";
				if(i % (itemsPerHeight * 2) == itemsPerHeight * 2 - 2){
					list.insert(i, prevPage(i, listLengthAfter));
					debug = "prevPage";
				}else if(i % (itemsPerHeight * 2) == itemsPerHeight * 2 - 1){
					list.insert(i, nextPage(i, listLengthAfter));
					debug = "nextPage";
				}else if(i >= list.size){
					list.insert(i, empty());
					debug = "empty";
				}
				Log.verbose2("Adding " + debug + " at " + i + "! Size now! " + list.size);
			}

			activePage = 0;
			double pagefloat = (float)list.size / (float)(itemsPerHeight * 2);
			int pages = list.size == 0 ? 1 : (int)Math.ceil(pagefloat);
			Log.verbose2("Pages! " + list.size + ", " + (itemsPerHeight * 2) + ", " + pagefloat + " = " + pages);

			clearChildren();
			this.pages.clear();
			for(int i = 0; i < pages; i++){
				final FocusTable page = new FocusTable(skin);
				page.setDebug(Tenebrae.TABLEDEBUG);
				page.background("window");
				Drawable bg = page.getBackground();
				page.setTouchable(Touchable.enabled);
				this.add(page);
				this.pages.add(page);
				for(int j = 0; j < itemsPerHeight; j++){
					for(int k = 0; k < 2; k++){
						MenuOption opt = list.get(i * itemsPerHeight * 2 + j * 2 + k);
						final Cell<Button> c = page.add((Button)opt).pad(
							j == 0 ? Tenebrae.MARGIN - bg.getTopHeight() : 0,
							k == 0 ? Tenebrae.MARGIN - bg.getLeftWidth() : 0,
							Tenebrae.MARGIN - (j == itemsPerHeight - 1 ? bg.getBottomHeight() : 0),
							Tenebrae.MARGIN - (k == 1 ? bg.getRightWidth() : 0)
						).grow();
						page.registerFocus(c);
						/*c.width(new Value(){
						 @Override
						 public float get(Actor a){
						 return (page.getWidth() - Tenebrae.margin) * 0.5f - Tenebrae.margin;// (total - middle)/2 - (drawpad+optpad)
						 }
						 });
						 c.height(new Value(){
						 @Override
						 public float get(Actor a){
						 return (page.getHeight() - Tenebrae.margin * (itemsPerHeight + 1)) / (float)itemsPerHeight;
						 }
						 });*/
						if(opt.box != null)
							this.add(opt.box);
					}
					page.row();
				}
				page.setVisible(false);
			}

			changePage(activePage);
		}

		public void sortList(){
			//Tenebrae.debug("Sorting list of "+this+"!");
			if(list.size == 0)
				return;

			list.sort();
		}

		public MenuBox findActiveLeaf(){
			MenuBox active = null;
			for(MenuOption opt : list)
				if(opt.box != null && active == null)
					active = opt.box.findActiveLeaf();
			active = active == null && isVisible() ? this : active;
			//Tenebrae.debug("Finding active leaf! "+active+" from "+this);
			return active;
		}

		public void setFocusTableToActive(){
			if(!isVisible())
				return;
			Log.debug("Setting focus page to", activePage, pages.get(activePage));
			game.setFocusTable(pages.get(activePage));
		}

		public static final String prevText = "<", nextText = ">", emptyText = Tenebrae.SHOWEMPTY ? "[empty]" : "";
		public MenuOption empty(){
			return new MenuOption(emptyText, null, Align.center, false, skin);
		}
		public MenuOption prevPage(int i, int size){
			return new MenuOption(prevText, null, Align.left, i != itemsPerHeight * 2 - 2, skin){
				@Override
				public void open(){
					changePage(activePage - 1);
				}
			};
		}
		public MenuOption nextPage(int i, int size){
			return new MenuOption(nextText, null, Align.right, i != size - 1, skin){
				@Override
				public void open(){
					changePage(activePage + 1);
				}
			};
		}
		public static boolean isEmpty(MenuOption opt){
			return opt.getText().equals(emptyText) && opt.box == null;
		}
		public static boolean isPrevPage(MenuOption opt){
			return opt.getText().equals(prevText) && opt.box == null;
		}
		public static boolean isNextPage(MenuOption opt){
			return opt.getText().equals(nextText) && opt.box == null;
		}

		public void changePage(int page){
			Log.verbose2("Changing page on " + this + " of size " + list.size + " from " + activePage + " to " + page + " of " + pages + "!");
			if(list.size == 0)
				return;
			if(page < 0 || page >= pages.size){
				changePage(page < 0 ? pages.size - (Math.abs(page) % pages.size) : page % pages.size);
				return;
			}
			pages.get(activePage).setVisible(false);
			activePage = page;
			pages.get(activePage).setVisible(true);
			setFocusTableToActive();
		}

		@Override
		public String toString(){
			return super.toString() + "§" + (id.isEmpty() ? "[noid]" : id);
		}
	}
	public static void addItemToBox(final MenuItem item, MenuBox box, Tenebrae game){
		MenuOption cat = null;
		for(MenuOption opt : box.list)
			if(opt.getText().equals(item.category)){
				cat = opt;
				break;
			}
		if(cat == null){
			MenuBox b = new MenuBox(game, item.category, box.skin);
			cat = new MenuOption(item.category, b, box.skin);
		}

		Log.debug(item);
		if(!(item instanceof MenuItem.GameItem)){
			cat.box.addOption(new MenuOption(item.name, null, box.skin){
					@Override
					public void open(){
						item.run("onUse");
					}
				});
		}else{
			MenuItem.GameItem gitem = (MenuItem.GameItem)item;
			MenuBox b = new MenuBox(game, gitem.name, box.skin);
			MenuOption opt = new MenuOption(gitem.name, b, box.skin);
			Log.verbose2("Putting item stuff in box!");
			while(gitem.toPutInBox.size > 0){
				Log.verbose("Got " + gitem.toPutInBox.get(0));
				opt.box.addOption(gitem.toPutInBox.removeIndex(0));
			}
			cat.box.addOption(opt);
		}

		box.addOption(cat);//down here bc box will detect as empty otherwise and get removed
	}

	public static class StatBox extends Table{
		protected MiniHealthBar health, mana;
		public StatBox(HealthBar health, HealthBar mana, Skin skin){
			super(skin);
			setDebug(Tenebrae.TABLEDEBUG);
			//setClip(true);
			this.health = new MiniHealthBar(health, skin);
			this.mana = new MiniHealthBar(mana, skin);
			setBackground("window-small");
			pad(Tenebrae.MARGIN * 0.25f);

			add(this.health).grow();
			row();
			add(this.mana).grow();
		}

		public void updateHP(){
			health.setHealth();
			mana.setHealth();
		}
	}
	public static class FadingStatBox extends StatBox{
		private float visTimer = 0;
		private static final float visMin = 2, visMax = 5;
		private static final Interpolation interp = Interpolation.fade;

		public FadingStatBox(HealthBar health, HealthBar mana, Skin skin){
			super(health, mana, skin);
		}
		public FadingStatBox(StatBox box){
			this(box.health, box.mana, box.getSkin());
			if(box.getStage() != null)
				box.getStage().addActor(this);
			setSize(box.getWidth(), box.getHeight());
		}

		@Override
		public void act(float delta){
			super.act(delta);

			visTimer += delta;
			if(visTimer < visMin){
				setColor(Color.WHITE);
			}else if(visTimer >= visMax){
				setColor(Color.CLEAR);
			}else{
				setColor(1, 1, 1, 1 - interp.apply((visTimer - visMin) / (visMax - visMin)));
			}
		}
		@Override
		public void setVisible(boolean visible){
			super.setVisible(visible);
			visTimer = visible ? 0 : visMax;
		}
		@Override
		public void updateHP(){
			super.updateHP();
			setVisible(true);
		}
	}

	public static class ButtonBox extends Table{//holds item and menu buttons
		MenuOption menu, items;
		ButtonBox(final Player player, Skin skin){
			super(skin);

			MenuBox menuBox = new MenuBox(player.game, "MenuBox", skin);
			//might want to delete these at some point
			addItemToBox(new MenuItem("Settings", "Main menu"){
					@Override
					public void run(String funcName){
						if(funcName.equals("onUse"))
							MyGdxGame.game.setScreen(new MainMenu());
						player.game.dispose();
					}
				}, menuBox, player.game);
			addItemToBox(new MenuItem("Settings", "Quit"){
					@Override
					public void run(String funcName){
						if(funcName.equals("onUse"))
							Gdx.app.exit();
					}
				}, menuBox, player.game);
			addItemToBox(new MenuItem.ScriptItem(player.game, "Player", "Kill", player, killScript), menuBox, player.game);
			addItemToBox(new MenuItem.ScriptItem(player.game, "Player", "Heal", player, healScript), menuBox, player.game);
			addItemToBox(new MenuItem.ScriptItem(player.game, "Player", "Tire", player, tireScript), menuBox, player.game);
			addItemToBox(new MenuItem.ScriptItem(player.game, "Player", "Invigor", player, invigorScript), menuBox, player.game);
			menu = new MenuOption("Menu", menuBox, Align.center, true, skin);
			ChangeListener click = new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor a){
					player.setUiDisabled(true);
				}
			};
			menu.addListener(click);
			this.add(menu).grow();
			this.row();

			MenuBox itemsBox = new MenuBox(player.game, "ItemsBox", skin);
			items = new MenuOption("Items", itemsBox, Align.center, true, skin);
			items.addListener(click);
			this.add(items).padTop(Tenebrae.MARGIN).grow();
		}
		public static final Prototype killScript, healScript, tireScript, invigorScript;
		static{
			try{
				killScript = Tenebrae.globals.compilePrototype(new StringReader("owner.affect(-player.hp,0)"), "kill");
				healScript = Tenebrae.globals.compilePrototype(new StringReader("owner.affect(player.maxhp-player.hp,0)"), "heal");
				tireScript = Tenebrae.globals.compilePrototype(new StringReader("owner.affect(0,-player.mp)"), "tire");
				invigorScript = Tenebrae.globals.compilePrototype(new StringReader("owner.affect(0,player.maxmp-player.mp)"), "invigor");
			}catch(IOException ex){throw new GdxRuntimeException("Couldn't load static script", ex);}
		}

		public MenuBox getActiveBox(){
			if(menu.box.isVisible())
				return menu.box;
			if(items.box.isVisible())
				return items.box;
			return null;
		}
	}
}
