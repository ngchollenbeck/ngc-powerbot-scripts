package scripts.agility_canifs_rooftop;


import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.GameObject;
import scripts.agility_canifs_rooftop.phases.RunRooftopsPhase;
import shared.action_config.ScriptConfig;
import shared.actions.LootAction;
import shared.constants.GameObjects;
import shared.constants.Items;
import shared.models.LootItem;
import shared.models.LootList;
import shared.templates.StructuredPhase;
import shared.tools.*;

import java.awt.*;
import java.util.concurrent.Callable;

import static org.powerbot.script.Condition.sleep;

@Script.Manifest(name = "Agility_Canifs_Rooftop", description = "Agility_Canifs_Rooftop", properties = "client=4; topic=051515; author=Bowman")
public class Agility_Canifs_Rooftop extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private ScriptConfig scriptConfig = new ScriptConfig(ctx);
    private StructuredPhase currentPhase;

    private Tile[] pathToStart_Fall1 = {new Tile(3482, 3497, 0), new Tile(3485, 3494, 0), new Tile(3488, 3491, 0), new Tile(3491, 3488, 0), new Tile(3495, 3488, 0), new Tile(3499, 3488, 0), new Tile(3503, 3488, 0), new Tile(3507, 3487, 0)};

    //region Antiban
    private long nextBreak;
    //endregion

    //region Actions
    LootList lootList = new LootList();
    LootAction lootAction;
    //endregion


    //region start
    @Override
    public void start() {
        this.scriptConfig.setPhase("Config");

        lootList.addLootItem(new LootItem(Items.MARK_OF_GRACE_11849, 1, -1));
        lootAction = new LootAction(ctx, "MOG", lootList, 10, true);

        // Phase
        RunRooftopsPhase runRooftopsPhase = new RunRooftopsPhase(ctx, "Roof");

        // Starting Location
        this.currentPhase = runRooftopsPhase;

        // Initial Status
        this.scriptConfig.setPhase(this.currentPhase.getName());
        this.scriptConfig.setStep(this.currentPhase.getStatus());

        nextBreak = (long) AntibanTools.getRandomInRange(4, 15) * 60000;

    }
    //endregion

    //region poll
    @Override
    public void poll() {
        // Pre State Check Action
        this.scriptConfig.prePollAction();

        if (getRuntime() > nextBreak) {
            if (GaussianTools.takeActionNormal()) {
                this.scriptConfig.setPhase("Antiban");
                AntibanTools.runAgilityAntiban(ctx);
            }
            this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(9, 13) * 60000);
        } else {

            if (ctx.game.floor() == 0) {
                GameObject tree = ctx.objects.select().id(GameObjects.CANIFS_TALL_TREE_14843).nearest().poll();

                // Check for run to start after a fall
                if (!CommonAreas.canifs_start().contains(ctx.players.local()) && !tree.inViewport()) {
                    this.scriptConfig.setPhase("Reset");
                    this.scriptConfig.setStep("Run");
                    pathToStart_Fall1[pathToStart_Fall1.length - 1] = CommonAreas.canifs_start().getRandomTile();
                    ctx.movement.newTilePath(pathToStart_Fall1).traverse();
                    sleep();
                } else {
                    sleep();
                    tree.interact("Climb");
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.game.floor() != 0;
                        }
                    }, 200, 60);
                }
            } else {
                // Check for mid-air glitch
                if (ctx.players.local().tile().x() == 3487 && ctx.players.local().tile().y() == 3476) {

                    // Confirm stuck in this position
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return CommonAreas.canifs_roof_6().contains(ctx.players.local());
                        }
                    }, 300, 5);

                    // If still in glitch position, move to rooftop
                    if (!CommonAreas.canifs_roof_6().contains(ctx.players.local()))
                        ctx.input.click(CommonAreas.canifs_roof_6().getRandomTile().matrix(ctx).nextPoint(), 1);
                }

                if (ctx.players.local().tile().x() == 3505 && ctx.players.local().tile().y() == 3489) {

                    // Confirm stuck in this position
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return CommonAreas.canifs_roof_1().contains(ctx.players.local());
                        }
                    }, 300, 5);

                    // If still in glitch position, move to rooftop
                    if (!CommonAreas.canifs_roof_1().contains(ctx.players.local()))
                        ctx.input.click(CommonAreas.canifs_roof_1().getRandomTile().matrix(ctx).nextPoint(), 1);
                }

                // Check for looting marks of grace
                if (lootAction.activate()) {
                    scriptConfig.setPhase("Loot");
                    scriptConfig.setStep(lootAction.getStatus());
                    lootAction.execute();
                }

                // Run next step in the Phase
                this.scriptConfig.setPhase(this.currentPhase.getName());
                this.scriptConfig.setStep(this.currentPhase.getStatus());
                this.currentPhase.activate();
            }
        }
    }
    //endregion

    //region messaged
    @Override
    public void messaged(MessageEvent e) {
        String msg = e.text();
    }
    //endregion

    //region repaint
    @Override
    public void repaint(Graphics g) {
        this.scriptConfig.paint(g);

        g.drawString("Phase : " + (this.scriptConfig.getPhase()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(1));
        g.drawString("Step  : " + (this.scriptConfig.getStep()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(2));
        g.drawString("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(3));

        g.setColor(GuiHelper.getTextColorImportant());
        g.drawString("Agility : " + ctx.skills.realLevel(Constants.SKILLS_AGILITY), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(4));

    }
    //endregion
}