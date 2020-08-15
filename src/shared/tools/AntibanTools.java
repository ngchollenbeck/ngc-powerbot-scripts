package shared.tools;

import org.powerbot.script.Random;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Game;

import java.awt.*;

import static org.powerbot.script.Condition.sleep;

/**
 * Antiban tools involving various human actions to
 * create breaks in bot patterns.
 */
public class AntibanTools {

    /**
     * Common action replica of open tab behavior
     *
     * @param ctx Context
     * @param tab Game Tab
     */
    private static void openTab(ClientContext ctx, Game.Tab tab) {
        CommonActions.openTab(ctx, tab);
    }

    /**
     * Sleep delay of various length
     *
     * @param length number of iterations for ~300ms delays
     */
    public static void sleepDelay(int length) {
        for (int i = 0; i <= length; i++) {
            sleep();
        }
    }

    /**
     * Hover over random stat
     *
     * @param ctx Context
     */
    public static void checkStat(ClientContext ctx) {

        openTab(ctx, Game.Tab.STATS);

        // Hover Random Stat
        sleep();
        ctx.input.move(Random.nextInt(550, 735), Random.nextInt(210, 460));

        sleepDelay(Random.nextInt(1, 3));
    }

    public static int getRandomInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        java.util.Random r = new java.util.Random();
        return r.nextInt((max - min) + 1) + min;
    }

    /**
     * Hover over specific stat
     *
     * @param ctx     Context
     * @param skillId Skill ID
     */
    public static void checkStat(ClientContext ctx, int skillId) {
        openTab(ctx, Game.Tab.STATS);

        int skillsXOffset = 60;
        int skillsYOffset = 24;
        int skillsXStart = 550;
        int skillsYStart = 210;

        int col = 0;
        int row = 0;

        switch (skillId) {
            case Constants.SKILLS_AGILITY:
                col = 1;
                row = 1;
                break;
            case Constants.SKILLS_ATTACK:
                row = 0;
                break;
            case Constants.SKILLS_DEFENSE:
                col = 0;
                row = 2;
                break;
            case Constants.SKILLS_STRENGTH:
                col = 0;
                row = 1;
                break;
            case Constants.SKILLS_RANGE:
                col = 0;
                row = 3;
                break;
            case Constants.SKILLS_PRAYER:
                col = 0;
                row = 4;
                break;
            case Constants.SKILLS_MAGIC:
                col = 0;
                row = 5;
                break;
            case Constants.SKILLS_RUNECRAFTING:
                col = 0;
                row = 6;
                break;
            case Constants.SKILLS_CONSTRUCTION:
                col = 0;
                row = 7;
                break;
            case Constants.SKILLS_HITPOINTS:
                col = 1;
                row = 0;
                break;
            case Constants.SKILLS_HERBLORE:
                col = 1;
                row = 2;
                break;
            case Constants.SKILLS_THIEVING:
                col = 1;
                row = 3;
                break;
            case Constants.SKILLS_CRAFTING:
                col = 1;
                row = 4;
                break;
            case Constants.SKILLS_FLETCHING:
                col = 1;
                row = 5;
                break;
            case Constants.SKILLS_SLAYER:
                col = 1;
                row = 6;
                break;
            case Constants.SKILLS_HUNTER:
                col = 1;
                row = 7;
                break;
            case Constants.SKILLS_MINING:
                col = 2;
                break;
            case Constants.SKILLS_SMITHING:
                col = 2;
                row = 1;
                break;
            case Constants.SKILLS_FISHING:
                col = 2;
                row = 2;
                break;
            case Constants.SKILLS_COOKING:
                col = 2;
                row = 3;
                break;
            case Constants.SKILLS_FIREMAKING:
                col = 2;
                row = 4;
                break;
            case Constants.SKILLS_WOODCUTTING:
                col = 2;
                row = 5;
                break;
            case Constants.SKILLS_FARMING:
                col = 2;
                row = 6;
                break;
            default:
                col = 2;
                row = 7;
        }

        int x = skillsXStart + (col * skillsXOffset);
        int y = skillsYStart + (row * skillsYOffset);

        ctx.input.move(Random.nextInt(x, x + skillsXOffset), Random.nextInt(y, y + skillsYOffset));

        sleepDelay(Random.nextInt(1, 6));

        openTab(ctx, Game.Tab.INVENTORY);
    }

    /**
     * Open attack tab and hover skill id
     *
     * @param ctx Context
     */
    public static void checkCombatLevel(ClientContext ctx) {
        openTab(ctx, Game.Tab.ATTACK);

        sleepDelay(Random.nextInt(1, 6));
    }

    /**
     * Adjust the camera angle X value
     *
     * @param ctx Context
     */
    public static void setRandomCameraAngle(ClientContext ctx) {
        ctx.camera.angle(Random.nextInt(0, 359));
    }

    /**
     * Adjust the camera Y value
     *
     * @param ctx Context
     */
    public static void setRandomCameraPitch(ClientContext ctx) {
        ctx.camera.pitch(Random.nextInt(0, 99));
    }

    /**
     * Randomly move the mouse
     *
     * @param ctx Context
     */
    public static void jiggleMouse(ClientContext ctx) {
        int x = ctx.input.getLocation().x;
        int y = ctx.input.getLocation().y;

        int iterations = Random.nextInt(1, 4);

        for (int i = 0; i < iterations; i++) {
            ctx.input.move(new Point(x + Random.nextInt(-30, 30), y + Random.nextInt(-30, 30)));
        }
    }

    /**
     * Sleep for a random period of time
     */
    public static void doNothing() {
        sleepDelay(Random.nextInt(0, 10));
    }

    /**
     * Move mouse offscreen
     *
     * @param ctx      Context
     * @param leftSide Move to left, false for right
     */
    public static void moveMouseOffScreen(ClientContext ctx, boolean leftSide) {
        int x;
        if (leftSide) {
            x = -10;
        } else {
            // Right side (for inventory actions)
            x = 1000;
        }

        int y = Random.nextInt(-10, (int)(ctx.widgets.component(165, 30).height() * 1.25));

        ctx.input.move(new Point(x, y));
        sleepDelay(Random.nextInt(0,5));
        ctx.input.defocus();
    }

    /**
     * Hover any object within the viewport
     *
     * @param ctx Context
     */
    public static void hoverRandomObject(ClientContext ctx) {
        var objects = ctx.objects.get(5);

        ctx.input.move(objects.get(Random.nextInt(0, objects.size())).nextPoint());

        sleepDelay(2);
    }

    /**
     * Hover any NPC in the viewport
     *
     * @param ctx Context
     */
    public static void hoverRandomNPC(ClientContext ctx) {
        var npcs = ctx.npcs.get();

        ctx.input.move(npcs.get(Random.nextInt(0, npcs.size())).nextPoint());

        sleepDelay(2);
    }

    /**
     * Toggle the Run icon
     *
     * @param ctx Context
     */
    public static void toggleRun(ClientContext ctx) {
        if (ctx.movement.energyLevel() > 20 && !ctx.movement.running()) {
            ctx.movement.running(true);
        }
    }

    /**
     * Reset the camera by clicking the compass
     *
     * @param ctx Context
     */
    public static void resetCamera(ClientContext ctx) {
        ctx.widgets.widget(548).component(7).click();
        sleepDelay(1);
    }

    /**
     * Toggle the xp drops by clicking the widget
     *
     * @param ctx Context
     */
    public static void toggleXPDrops(ClientContext ctx) {
        ctx.widgets.widget(160).component(1).click();
    }

    public static void runCommonAntiban(ClientContext ctx){
        switch(Random.nextInt(0, 11)){
            case 1: toggleRun(ctx); break;
            case 2: moveMouseOffScreen(ctx, true); break;
          //  case 3: checkStat(ctx, getRandomInRange(0,23)); break;
            case 4: moveMouseOffScreen(ctx, false); break;
           // case 5: checkStat(ctx, getRandomInRange(0,4)); break;
            case 6: resetCamera(ctx); break;
            case 7: hoverRandomNPC(ctx); break;
            case 8: setRandomCameraAngle(ctx); break;
            case 9: setRandomCameraPitch(ctx); break;
            case 10: jiggleMouse(ctx); break;
            default:doNothing();
        }
    }

    public static void runAgilityAntiban(ClientContext ctx){
        switch(AntibanTools.getRandomInRange(1, 5)){
            case 1: moveMouseOffScreen(ctx, true); break;
            case 2: moveMouseOffScreen(ctx, false); break;
            case 3: hoverRandomNPC(ctx); break;
            case 4: jiggleMouse(ctx); break;
            default:doNothing();
        }
    }

    public static void runMiningAntiban(ClientContext ctx){
        switch(AntibanTools.getRandomInRange(1, 5)){
            case 1: moveMouseOffScreen(ctx, true); break;
            case 2: moveMouseOffScreen(ctx, false); break;
            case 3: hoverRandomNPC(ctx); break;
            case 4: jiggleMouse(ctx); break;
            default:doNothing();
        }
    }
}
