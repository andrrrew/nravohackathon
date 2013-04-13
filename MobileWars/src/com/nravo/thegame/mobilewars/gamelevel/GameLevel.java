package com.nravo.thegame.mobilewars.gamelevel;

import com.nravo.thegame.mobilewars.entity.AndroidHeroPool;
import com.nravo.thegame.mobilewars.entity.Building;
import com.nravo.thegame.mobilewars.entity.Hero;
import com.nravo.thegame.mobilewars.gamelevel.handlers.DrawPointerUpdateHandler;
import com.nravo.thegame.mobilewars.gamelevel.handlers.UnitMovementHandler;
import com.nravo.thegame.mobilewars.layers.LevelWonLayer;
import com.nravo.thegame.mobilewars.managers.GameManager;
import com.nravo.thegame.mobilewars.managers.ResourceManager;
import com.nravo.thegame.mobilewars.managers.SceneManager;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;
import org.andengine.util.adt.pool.GenericPool;

import java.util.ArrayList;
import java.util.List;

public class GameLevel extends ManagedGameScene implements
        GameManager.GameLevelGoal, IOnSceneTouchListener {

    public static final int HEROES_POOL_SIZE = 50;
    public final Levels.LevelDefinition mLevelDefinition;
    public final int mNumberOfBuildingsInCurrentLevel;

    public int mNumberOfEnemiesLeft = 10;
    private int mNumberOfAlliesLeft = 10;

    public float mX = 0;
    public float mY = 0;

    // Moving units from building to building
    // These hold information about the touch event
    // to collect units from multiple buildings (buildingsFrom)
    public List<Building> buildingsFrom = null;
    public Building buildingTo;

    private boolean mHasCompletionTimerRun = false;

    // ============================================================
    // ===================== UPDATE HANDLERS=======================
    // ============================================================
    private IUpdateHandler onCompletionTimer = new IUpdateHandler() {
        final float COMPLETION_DELAY_SECONDS = 60f;
        private float mTotalElapsedTime = 0f;

        @Override
        public void onUpdate(float pSecondsElapsed) {
            this.mTotalElapsedTime += pSecondsElapsed;
            if (this.mTotalElapsedTime >= this.COMPLETION_DELAY_SECONDS) {
//                GameLevel.this.mHasCompletionTimerRun = true;
                if (GameLevel.this.isLevelCompleted()) {
                    GameLevel.this.onLevelCompleted();
                } else {
                    GameLevel.this.onLevelFailed();
                }
                GameLevel.this.unregisterUpdateHandler(this);
            }
        }

        @Override
        public void reset() {
        }
    };

    // Draws pointers when dragging your finger
    public DrawPointerUpdateHandler lineDrawingHandler;

    public GameLevel(final Levels.LevelDefinition levelDefinition) {
        this.mLevelDefinition = levelDefinition;
        mNumberOfBuildingsInCurrentLevel = levelDefinition.buildingsInLevel.length;
    }

    // ======================================
    // ============== METHODS ===============
    // ======================================
    @Override
    public boolean isLevelCompleted() {
        return this.mNumberOfEnemiesLeft <= 0;
    }

    @Override
    public boolean isLevelFailed() {
        return this.mNumberOfAlliesLeft <= 0;
    }

    @Override
    public void onLevelCompleted() {
        if (this.mHasCompletionTimerRun) {
            SceneManager.getInstance().showLayer(
                    LevelWonLayer.getInstance(this), false, false, false);
        } else {
            GameLevel.this.registerUpdateHandler(this.onCompletionTimer);
        }
    }

    @Override
    public void onLevelFailed() {
        if (this.mHasCompletionTimerRun) {
            // TODO restart level
        } else {
            GameLevel.this.registerUpdateHandler(this.onCompletionTimer);
        }
    }

    @Override
    public void onLoadLevel() {
        GameManager.setGameLevel(this);
        GameManager.setGameLevelGoal(this);

        final int numberOfBuildingsInLevel = mLevelDefinition.buildingsInLevel.length;
        buildingsFrom = new ArrayList<Building>(numberOfBuildingsInLevel);
        lineDrawingHandler = new DrawPointerUpdateHandler(GameLevel.this);

        GenericPool<Hero> heroPool = new AndroidHeroPool();
        heroPool.batchAllocatePoolItems(HEROES_POOL_SIZE);

        Rectangle rectangle = new Rectangle(0f, 0f, 120f, 120f,
                ResourceManager.getInstance().engine
                        .getVertexBufferObjectManager());
        rectangle.setColor(1, 0, 1);
        GameLevel.this.attachChild(rectangle);

        // Buildings
        for (Levels.BuildingDefinition currentBuilding : GameLevel.this.mLevelDefinition.buildingsInLevel) {
            new Building(GameLevel.this, currentBuilding.race,
                    currentBuilding.x, currentBuilding.y, currentBuilding.initialNumberOfUnits);
        }

        //new Hero(500f, 400f, GameLevel.this, Race.ANDROID).moveHero(100, 100, 900, 500);
        //new Hero(500f, 400f, GameLevel.this, Race.APPLE_IOS).moveHero(300, 300, 700, 400);

        GameLevel.this.setOnSceneTouchListener(this);
    }

    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {

        if (pSceneTouchEvent.isActionDown()) {
            int a = 1;
            return false;
        }

        if (pSceneTouchEvent.isActionMove()) {
            mX = pSceneTouchEvent.getX();
            mY = pSceneTouchEvent.getY();
            if (!lineDrawingHandler.isRegistered()) {
                GameLevel.this.registerUpdateHandler(lineDrawingHandler);
                lineDrawingHandler.setPointersVisible(true);
                lineDrawingHandler.setRegistered(true);
            }
        }

        if (pSceneTouchEvent.isActionUp()) {
            lineDrawingHandler.reset();
            GameLevel.this.unregisterUpdateHandler(lineDrawingHandler);
            // if our touch event of collecting houses is correct
            // perform moving units
            if (!buildingsFrom.isEmpty() && buildingTo != null) {
                performUnitMovement();
            }/* else {
            	new ApplePiePower(pSceneTouchEvent.getX(), pSceneTouchEvent.getY(),
            			ResourceManager.sApplePieTR, ResourceManager.sMagnetTR,
            			ResourceManager.getEngine(), this,
            			ResourceManager.getInstance().camera);
            }*/
            buildingsFrom.clear();
            buildingTo = null;
            return true;
        }
        return false;
    }

    private void performUnitMovement() {
        GameLevel.this.registerUpdateHandler(new UnitMovementHandler(this));

        for (Building building : buildingsFrom) {
            building.decrementNumberOfUnits(1);
        }
        buildingTo.incrementNumberOfUnits(1);
    }

    public void disposeLevel() {
        // TODO dispose HUD here
    }
}
