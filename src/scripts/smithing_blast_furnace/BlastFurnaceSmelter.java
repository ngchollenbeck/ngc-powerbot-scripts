package scripts.smithing_blast_furnace;


import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Game;
import org.powerbot.script.rt4.GameObject;
import shared.action_config.ScriptConfig;
import shared.actions.BankAction;
import shared.constants.Items;
import shared.tools.AntibanTools;
import shared.tools.CommonActions;
import shared.tools.GaussianTools;
import shared.tools.GuiHelper;

import java.awt.*;
import java.util.concurrent.Callable;

import static org.powerbot.script.Condition.sleep;

/**
 * Phases: Coal | Bars | Break
 * Steps:
 * Coal - Run till varpbit 547 value is > 200
 * -- Bank withdraw Coal till inv full
 * -- Fill Coal Bag till no more coal in inv
 * -- Bank withdraw Coal till inv full
 * -- Run to conveyer belt till belt in viewport
 * -- Deposit ore till no more coal in inventory
 * -- Shift click coal bag to remove stored coal
 * -- Deposit ore till no more coal in inv
 * <p>
 * Bars - Run till varpbit 547 value is < 60
 * -- Bank deposit coal bag and withdraw bars
 * -- Run to belt and deposit ore till inv empty
 * -- Run to bar stand till wtihin area/tile distance
 * -- hold space and withdraw bars till inv not empty
 * -- Bank deposit bars and withdraw ore
 * <p>
 * Break - When antiban is triggered
 * -- Run to stairs
 * -- Climb Stairs
 * -- Go AFK
 * ** IF not afk, run antiban in place
 */
@Script.Manifest(name = "Smithing - Blast Furnace", description = "Blast Furnace Bruh", properties = "client=4; topic=051515; author=Bowman")
public class BlastFurnaceSmelter extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private final ScriptConfig scriptConfig = new ScriptConfig(ctx);
    private int coalCount;
    private int oreId;
    private boolean isCoalBagFull;
    private boolean isWaitingForBars;

    private BankAction bankCoal;
    private BankAction bankOre;

    private Tile conveyorTile = new Tile(1943, 4967, 0);

    //region Antiban
    private long nextBreak;
    //endregion

    //region Phase
    private enum Phase {
        Start, Coal, Bars
    }

    private Phase currentPhase;
    //endregion

    //region start
    @Override
    public void start() {
        this.scriptConfig.setPhase("Coal");
        this.updateCoalCount();
        this.isCoalBagFull = false;
        this.isWaitingForBars = false;

        String barType = CommonActions.promptForSelection("Blast Furnace", "Bar Type?", new String[]{"Mithril", "Adamant", "Runite"});
        switch (barType) {
            case "Mithril":
                this.oreId = Items.MITHRIL_ORE_447;
            case "Adamant":
                this.oreId = Items.ADAMANTITE_ORE_449;
            case "Runite":
                this.oreId = Items.RUNITE_ORE_451;
            default:
                this.oreId = Items.MITHRIL_ORE_447;
        }

        this.bankCoal = new BankAction(ctx, "Coal", -1, -1, Items.COAL_BAG_12019, 28, Items.COAL_453, 28, false, true, true, null);
        this.bankOre = new BankAction(ctx, "Bars", Items.COAL_BAG_12019, -1, oreId, 28, -1, -1, false, true, false, null);

        nextBreak = (long) AntibanTools.getRandomInRange(4, 15) * 60000;
        this.currentPhase = Phase.Start;
    }
    //endregion

    //region messaged
    @Override
    public void messaged(MessageEvent e) {
        String msg = e.text();

        if (msg.contains("your ore goes onto the conveyor belt.")) {
            this.updateCoalCount();
        }
    }
    //endregion

    //region repaint
    @Override
    public void repaint(Graphics g) {
        if (!ctx.controller.isSuspended()) {
            this.scriptConfig.paint(g);

            g.drawString("Phase  : " + (this.scriptConfig.getPhase()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(1));
            g.drawString("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(2));
            g.drawString("Coal: " + this.coalCount, this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(3));
        }
    }
    //endregion

    //region poll
    @Override
    public void poll() {
        // Pre Phase Check Action
        this.scriptConfig.prePollAction();

        // Antiban Break
        if (this.activateAntiban()) {
            this.executeAntiban();
        } else {
            executePhase();
        }
    }
    //endregion

    //region Phase
    private Phase checkNextPhase() {
        switch (this.currentPhase) {
            case Start:
                return Phase.Coal;

            case Coal:
                if (this.coalCount > 200 && ctx.inventory.select().id(Items.COAL_453).count() == 0) {
                    return Phase.Bars;
                }
                break;

            case Bars:
                if (this.coalCount < 56 && ctx.inventory.select().id(oreId).count() == 0) {
                    return Phase.Coal;
                }
                break;
        }

        return this.currentPhase;
    }

    private void executePhase() {
        this.currentPhase = checkNextPhase();

        switch (this.currentPhase) {
            case Start:
                break;

            case Coal:
                this.scriptConfig.setPhase("Coal");

                if (this.isCoalBagFull && ctx.inventory.isFull()) {
                    this.useConveyorBelt();
                } else if (ctx.inventory.isFull() && !this.isCoalBagFull) {
                    this.fillCoalBag();
                    this.isCoalBagFull = true;
                    sleep();
                } else if (ctx.inventory.isEmpty() || ctx.inventory.select().name("Coal").count() == 0) {
                    bankCoal.execute();
                    sleep();
                }
                break;

            case Bars:
                this.scriptConfig.setPhase("Bars");

                if (this.isWaitingForBars) {
                    // move to bar deposit
                    this.retrieveBars();

                    if (!ctx.inventory.isEmpty()) {
                        this.isWaitingForBars = false;
                    }

                } else {
                    if (ctx.inventory.select().id(oreId).count() > 0) {
                        // Walk to conveyor and use ores
                        this.useConveyorBelt();

                        if (ctx.inventory.isEmpty()) {
                            this.isWaitingForBars = true;
                        }
                    } else {
                        this.bankOre.execute();
                    }
                }
                break;
        }
    }
    //endregion

    //region Antiban
    private void executeAntiban() {
        if (GaussianTools.takeActionNormal()) {
            this.scriptConfig.setPhase("Antiban");
            this.scriptConfig.setStep("Wait");
            AntibanTools.runAgilityAntiban(ctx);
        }
        this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(9, 13) * 60000);
    }

    private boolean activateAntiban() {
        return (getRuntime() > nextBreak);
    }
    //endregion

    //region Coal Operations
    private void fillCoalBag() {
        CommonActions.openTab(ctx, Game.Tab.INVENTORY);
        ctx.inventory.select().name("Coal bag").poll().interact("Fill");

        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.inventory.count() == 1;
            }
        }, 300, 10);
    }

    private void useConveyorBelt() {
        var conveyorBelt = ctx.objects.select().name("Conveyor belt").first().poll();

        if (!conveyorBelt.inViewport()) {
            ctx.movement.step(conveyorTile);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return conveyorBelt.inViewport();
                }
            }, 1000, 10);
        }

        ctx.input.click(conveyorTile.matrix(ctx).nextPoint(), 1);
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !ctx.inventory.isFull();
            }
        }, 200, 10);

        if (ctx.inventory.select().name("Coal bag").count() == 1) {
            this.emptyCoalBag();

            ctx.input.click(conveyorTile.matrix(ctx).nextPoint(), 1);
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return !ctx.inventory.isFull();
                }
            }, 200, 10);

        }
    }

    private void emptyCoalBag() {
        var bag = ctx.inventory.select().name("Coal bag").first().poll();

        if (bag.valid()) {
            ctx.input.send("{VK_SHIFT down}");
            sleep();
            bag.click();
            sleep();
            ctx.input.send("{VK_SHIFT up");

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.select().count() > 1;
                }
            }, 100, 10);

            if (ctx.inventory.select().count() > 1) {
                this.isCoalBagFull = false;
            }
        }
    }
    //endregion

    //region Bar Operations
    /*
     * -- Bank deposit coal bag and withdraw bars
     * -- Run to belt and deposit ore till inv empty
     * -- Run to bar stand till wtihin area/tile distance
     * -- hold space and withdraw bars till inv not empty
     * -- Bank deposit bars and withdraw ore
     */
    private void updateCoalCount() {
        this.coalCount = ctx.varpbits.varpbit(547);
    }

    private void retrieveBars() {
        GameObject barDeposit = ctx.objects.select().name("Bar dispenser").first().poll();

        if (barDeposit.valid()) {
            barDeposit.interact("Check");

            ctx.input.send("{VK_SPACE down}");

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return !ctx.inventory.isEmpty();
                }
            }, 1000, 10);
        }
    }

    //endregion
}