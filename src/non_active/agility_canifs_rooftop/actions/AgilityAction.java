package scripts.agility_canifs_rooftop.actions;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Random;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import shared.templates.StructuredAction;
import shared.tools.AntibanTools;
import shared.tools.GaussianTools;

import java.awt.*;
import java.util.concurrent.Callable;

/**
 * Perform an action on a game object. Can be used to open doors, collect NMZ powerups, etc.
 */
public class AgilityAction extends StructuredAction {
    private int objectId;
    private String action;
    private Area startingArea;
    //    private Area startingArea;
    private GameObject target;

    public AgilityAction(ClientContext ctx, String status) {
        super(ctx, status);
    }

    /**
     * Establish action to be taken against the objectId which results in the player moving to the landing area
     *
     * @param ctx
     * @param status
     * @param action
     * @param objectId
     * @param startingArea
     */
    public AgilityAction(ClientContext ctx, String status, String action, int objectId, Area startingArea) {
        super(ctx, status);
        this.action = action;
        this.objectId = objectId;
        this.startingArea = startingArea;
    }

    @Override
    public boolean activate() {
        target = ctx.objects.select().id(objectId).nearest().poll();

        return target.valid() && startingArea.contains(ctx.players.local());// && target.tile().matrix(ctx).reachable();
    }


    @Override
    public void execute() {
        GameObject obj = ctx.objects.select().id(objectId).nearest().poll();

        if (obj.inViewport()) {
            if (!GaussianTools.takeActionUnlikely()) { //85% accuracy
                obj.interact(action);
                // Wait to start moving
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.players.local().inMotion() || ctx.players.local().animation() != -1;
                    }
                }, 1000, 3);

                // Wait to complete the interaction
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !startingArea.contains(ctx.players.local());
                    }
                }, 200, 50);

                if (GaussianTools.takeActionUnlikely()) {
                    AntibanTools.sleepDelay(AntibanTools.getRandomInRange(0, 2));
                }
            } else {
                // Misclick
                Point p = obj.nextPoint();
                int diff = 20;
                ctx.input.move(new Point(AntibanTools.getRandomInRange(p.x - diff, p.x + diff), AntibanTools.getRandomInRange(p.y - diff, p.y + diff)));
                ctx.input.click(true);
            }
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return !obj.valid() || !obj.tile().matrix(ctx).reachable();
                }
            }, 250, 10);
        } else {

            ctx.movement.step(new Tile(obj.tile().x() + Random.nextInt(-3, 3), obj.tile().y() + Random.nextInt(-3, 3)));

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return obj.inViewport();
                }
            }, 250, 20);

        }

    }

    @Override
    public boolean isComplete() {
        return !startingArea.contains(ctx.players.local()) && !ctx.players.local().inMotion() && ctx.players.local().animation() == -1;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public Area getStartingArea() {
        return startingArea;
    }

    public void setStartingArea(Area landingArea) {
        this.startingArea = landingArea;
    }
}
