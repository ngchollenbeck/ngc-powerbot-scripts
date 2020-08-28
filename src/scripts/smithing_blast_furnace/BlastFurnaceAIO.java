package scripts.smithing_blast_furnace;

import org.powerbot.script.*;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.GameObject;
import shared.action_config.ScriptConfig;
import shared.constants.Items;
import shared.tools.AntibanTools;
import shared.tools.CommonActions;
import shared.tools.GaussianTools;
import shared.tools.GuiHelper;

import java.awt.*;
import java.text.NumberFormat;
import java.util.concurrent.Callable;

import static org.powerbot.script.Condition.sleep;

/*
TODO:
- 
*/

@Script.Manifest(name = "BlastFurnaceAIO", description = "BlastFurnaceAIO", properties = "client=4; topic=051515; author=Bowman")
public class BlastFurnaceAIO extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private final ScriptConfig scriptConfig = new ScriptConfig(ctx);
    private int barId;
    private int primaryOreId;
    private int configIndex;
    private boolean coalBagged;
    private int barsMade;
    private int smithingXP;

    private int profitPerBar = 0;
    private String totalProfit = "$0";
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance();

    private final int inputSpeedMin = 60;
    private final int inputSpeedMax = 75;

    private int[] validOres;
    private int conveyorUseCount;

    //region Antiban
    private long nextBreak;
    //endregion

    //region Trips
    private BlastTripConfig[] tripConfigs;
    private BlastTripConfig currentConfig;
    //endregion

    //region Phase
    private enum Phase {
        Start, Banking, Conveyor, Bars, End
    }

    private Phase currentPhase;
    //endregion

    //region start
    @Override
    public void start() {
        ctx.input.speed(Random.nextInt(inputSpeedMin, inputSpeedMax));
        this.currentPhase = Phase.Start;
        this.scriptConfig.setPhase("Start");

        String barType = CommonActions.promptForSelection("Smelting", "Bar Type", new String[]{"Steel", "Mithril", "Adamant", "Rune"});

        switch (barType) {
            case "Steel":
                this.steelConfig();
                this.barId = Items.STEEL_BAR_2353;
                this.primaryOreId = Items.IRON_ORE_440;
                this.profitPerBar = 150;
                break;
            case "Mithril":
                this.mithConfig();
                this.barId = Items.MITHRIL_BAR_2359;
                this.primaryOreId = Items.MITHRIL_ORE_447;
                break;
            case "Adamant":
                this.addyConfig();
                this.barId = Items.ADAMANTITE_BAR_2361;
                this.primaryOreId = Items.ADAMANTITE_ORE_449;
                this.profitPerBar = 350;
                break;
            case "Rune":
                this.runeConfig();
                this.barId = Items.RUNITE_BAR_2363;
                this.primaryOreId = Items.RUNITE_ORE_451;
                break;
            default:
                ctx.controller.stop();
        }

        this.configIndex = 0;
        this.barsMade = 0;

        this.smithingXP = ctx.skills.experience(Constants.SKILLS_SMITHING);

        this.nextBreak = AntibanTools.setNextBreakTime(getRuntime(), 2, 10);
        this.currentConfig = tripConfigs[this.configIndex];
        this.validOres = new int[]{Items.COAL_453, this.primaryOreId};

        this.coalBagged = !CommonActions.promptForYesNo("Coal Bag", "Is it empty?");
        var ready = CommonActions.promptForYesNo("Are you ready?", "Shall we begin?");
        if (!ready) {
            ctx.controller.suspend();
        }
    }
    //endregion

    //region messaged
    @Override
    public void messaged(MessageEvent e) {
        String msg = e.text().toLowerCase();

        if (msg.contains("the coal bag contains 27 pieces of coal")) {
            this.coalBagged = true;
            this.conveyorUseCount = 0;
        }

        if (msg.equals("all your ore goes onto the conveyor belt.")) {
            this.conveyorUseCount++;
        }
    }
    //endregion

    //region repaint
    @Override
    public void repaint(Graphics g) {
        if (!ctx.controller.isSuspended()) {
            this.scriptConfig.paint(g);
            g.drawString("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(1));
            g.drawString("Phase  : " + (this.scriptConfig.getPhase()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(2));
            g.drawString("Step   : " + (this.scriptConfig.getStep()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(3));
            g.drawString("Break  : " + (GuiHelper.getReadableRuntime(nextBreak)), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(4));

            g.drawString("Bagged : " + (this.coalBagged), this.scriptConfig.paintLineXMiddle(), this.scriptConfig.paintLineY(1));
            g.drawString("Bars : " + (this.barsMade), this.scriptConfig.paintLineXMiddle(), this.scriptConfig.paintLineY(2));
            g.drawString("Profit : " + (this.totalProfit) + "K", this.scriptConfig.paintLineXMiddle(), this.scriptConfig.paintLineY(3));
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
                return Phase.Banking;

            case Banking:
                // Inventory full and no gems
                if (ctx.inventory.isFull() && coalBagged && ctx.inventory.select().id(this.currentConfig.getWithdrawOreId()).poll().valid()) {
                    this.conveyorUseCount = 0;
                    return Phase.Conveyor;
                }
                break;

            case Conveyor:
                // Invent
                if (ctx.inventory.select().count() == 1 && !coalBagged) {

                    if (this.currentConfig.isCollectBars()) {
                        return Phase.Bars;
                    } else {
                        this.updateTripConfig();
                        return Phase.Banking;
                    }
                }


                break;

            case Bars:
                if (ctx.inventory.select().id(barId).count() == 27) {
                    this.updateTripConfig();

                    return Phase.Banking;
                }
                break;
        }

        return this.currentPhase;
    }

    private void executePhase() {

        this.currentPhase = this.checkNextPhase();

        switch (this.currentPhase) {
            case Start:
                break;
            case Banking:
                this.scriptConfig.setStep("Bank");

                if (!coalBagged) {
                    this.bankAction(Items.COAL_453, true);
                } else {
                    // Bank - Withdraw Ores
                    this.bankAction(this.currentConfig.getWithdrawOreId(), false);
                }

                break;
            case Conveyor:
                // Conveyor Belt
                this.scriptConfig.setStep("Load Conveyor");

                this.coalBagged = (conveyorUseCount < 2);

                if (this.coalBagged && ctx.inventory.select().count() == 1) {
                    this.useCoalBag(true);
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.inventory.isFull();
                        }
                    }, 100, 10);
                }

                if (ctx.inventory.select().id(validOres).poll().valid()) {
                    this.conveyorAction();
                }
                break;

            case Bars:
                if (currentConfig.isCollectBars()) {
                    this.scriptConfig.setStep("Collect Bars");
                    this.collectBarsAction();
                }
                break;
            case End:
                if (ctx.bank.opened()) {
                    CommonActions.closeBank(ctx);
                } else if (ctx.game.logout()) {
                    ctx.widgets.widget(182).component(12).click();
                    System.out.println("------Fin-------");
                    System.out.println("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()));
                    System.out.println("Profit: " + this.totalProfit + "K");
                    System.out.println("Bars: " + this.barsMade);
                    ctx.controller.stop();
                }

        }

    }
    //endregion

    //region Antiban
    private void executeAntiban() {
        if (GaussianTools.takeActionNormal()) {
            this.scriptConfig.setStep("Antiban");
            AntibanTools.runMiningAntiban(ctx);
        }
        this.nextBreak = AntibanTools.setNextBreakTime(getRuntime(), 2, 10);

        CommonActions.adjustMouseSpeed(ctx, inputSpeedMin, inputSpeedMax);

    }

    private boolean activateAntiban() {
        return (getRuntime() > nextBreak);
    }
    //endregion

    //region Helpers
    private void useCoalBag(boolean emptyBag) {
        if (emptyBag) {
            ctx.input.send("{VK_SHIFT down}");
            sleep(Random.nextInt(200, 300));
            ctx.inventory.select().id(Items.COAL_BAG_12019).poll().click();
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.select().id(Items.COAL_453).count() > 0;
                }
            }, Random.nextInt(125, 220), 10);
            sleep(Random.nextInt(100, 200));
            ctx.input.send("{VK_SHIFT up}");

            this.coalBagged = ctx.inventory.select().id(Items.COAL_453).count() > 0; // Coal in inventory = not bagged
        } else {
            ctx.inventory.select().id(Items.COAL_BAG_12019).poll().interact("Fill");
            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.select().count() == 1 || ctx.widgets.widget(193).component(2).visible();
                }
            }, Random.nextInt(150, 288), 10);

            this.coalBagged = (ctx.inventory.count() == 1 || ctx.widgets.widget(193).component(2).visible());
        }
    }
//endregion

    //region Banking
    private void bankAction(int oreId, boolean fillCoalBag) {
        if (ctx.bank.opened() || CommonActions.openBank(ctx)) {

            CommonActions.depositAllOfItem_X(ctx, Items.VIAL_229, true);
            CommonActions.depositAllOfItem_X(ctx, Items.SUPER_ENERGY1_3022, true);
            CommonActions.depositAllOfItem_X(ctx, Items.STAMINA_POTION1_12631, true);
            CommonActions.depositAllOfItem_X(ctx, Items.COAL_453, true);
            CommonActions.depositAllOfItem_X(ctx, barId, true);

            if (ctx.movement.energyLevel() < 50) {
                boolean withdrawPots = (CommonActions.withdrawItem(ctx, Items.STAMINA_POTION1_12631, Bank.Amount.ONE, true) && CommonActions.withdrawItem(ctx, Items.SUPER_ENERGY1_3022, Bank.Amount.ONE, true));
                if (withdrawPots) {
                    if (CommonActions.closeBank(ctx)) {
                        ctx.inventory.select().id(Items.STAMINA_POTION1_12631).poll().interact("Drink");
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.inventory.select().id(Items.VIAL_229).poll().valid();
                            }
                        }, Random.nextInt(1000, 1200), 5);
                        sleep();
                        ctx.inventory.select().id(Items.SUPER_ENERGY1_3022).poll().interact("Drink");
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.movement.energyLevel() > 50;
                            }
                        }, 100, 10);
                    }
                }
            } else if (ctx.inventory.select().id(oreId).count() == 0)
                if (ctx.bank.select().id(oreId).count(true) < 27) {
                    this.currentPhase = Phase.End;
                } else if (CommonActions.withdrawItem(ctx, oreId, Bank.Amount.X, (oreId == Items.COAL_453))) {
                    if (fillCoalBag) {
                        CommonActions.hoverItem(ctx, Items.COAL_BAG_12019);
                        CommonActions.closeBank(ctx);
                        this.useCoalBag(false);

                        // Sanity Check
                        if (!coalBagged) {
                            sleep();
                            this.useCoalBag(false);
                        }
                        this.smithingXP = ctx.skills.experience(Constants.SKILLS_SMITHING);

                    }
                }
        }
    }

    //endregion
    //region Conveyor
    private void conveyorAction() {
        var conveyor = ctx.objects.select().id(9100).poll();
        if (!ctx.players.local().inMotion()) {

            if (conveyor.valid() && ctx.inventory.select().id(validOres).count() > 0) {
                conveyor.interact("Put-ore-on");

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.inventory.select().count() == 1;
                    }
                }, 100, 10);

                // Only hover the coal bag on the wy to the conveyor
                if (conveyor.tile().distanceTo(ctx.players.local()) > 1) {
                    sleep(Random.nextInt(500, 3100));
                    CommonActions.hoverItem(ctx, Items.COAL_BAG_12019);
                }

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !ctx.inventory.isFull() || (!ctx.players.local().inMotion() && conveyor.tile().distanceTo(ctx.players.local()) > 1);
                    }
                }, Random.nextInt(100, 200), 40);
            }
        }
    }

    //endregion
    //region Bars

    private void collectBarsAction() {
        GameObject dispenser = ctx.objects.select().name("Bar dispenser").poll();

        //todo: test this to ensure accuracy. Trying to prevent clicks on the top apparatus and more on the base
        ctx.input.move(dispenser.centerPoint().x + Random.nextInt(-15, 15), dispenser.centerPoint().y + Random.nextInt(-30, 5));
        if (dispenser.boundingModel().contains(ctx.input.getLocation())) {
            ctx.input.click(true);
        }

        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return ctx.widgets.widget(270).component(0).visible() || (dispenser.tile().distanceTo(ctx.players.local()) <= 2 && !ctx.players.local().inMotion());
            }
        }, Random.nextInt(100, 300), 30);

        if (ctx.widgets.widget(270).component(0).visible()) {

            ctx.input.send("{VK_SPACE down}");

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.isFull();
                }
            }, Random.nextInt(100, 300), 30);

            if (ctx.inventory.isFull()) {
                this.barsMade += 27;
                calProfit();
                this.smithingXP = ctx.skills.experience(Constants.SKILLS_SMITHING);
            }

        }
    }
    //endregion

    //region Profit
    private void calProfit() {
        var runtimeMinutes = getRuntime() / 60000;
        int blastFees = (int) (1200 * runtimeMinutes);
        var rawTotalProfit = ((this.profitPerBar * barsMade) - blastFees - (1750 * (runtimeMinutes / 2))) / 1000;
        this.totalProfit = formatter.format(rawTotalProfit);
    }
    //endregion


    //region Bar/Trip Configs
    private void steelConfig() {
        this.tripConfigs = new BlastTripConfig[]{
                new BlastTripConfig("Bars", Items.IRON_ORE_440, true)
        };
    }

    private void mithConfig() {
        this.tripConfigs = new BlastTripConfig[]{
                new BlastTripConfig("Coal Only", Items.COAL_453, false),
                new BlastTripConfig("Mith One", Items.MITHRIL_ORE_447, true),
                new BlastTripConfig("Mith Two", Items.MITHRIL_ORE_447, true)
        };
    }

    private void addyConfig() {
        this.tripConfigs = new BlastTripConfig[]{
                new BlastTripConfig("Coal Only", Items.COAL_453, false),
                new BlastTripConfig("Addy", Items.ADAMANTITE_ORE_449, true)
        };
    }

    private void runeConfig() {
        this.tripConfigs = new BlastTripConfig[]{
                new BlastTripConfig("Combo One", Items.RUNITE_ORE_451, false),
                new BlastTripConfig("Coal One", Items.COAL_453, false),
                new BlastTripConfig("Coal Two", Items.COAL_453, true),
                new BlastTripConfig("Combo Two", Items.RUNITE_ORE_451, false),
                new BlastTripConfig("Coal Three", Items.COAL_453, true)
        };
    }

    private void updateTripConfig() {
        if (tripConfigs.length > 1) {
            if (this.configIndex == tripConfigs.length - 1) {
                this.configIndex = 0;
            } else {
                this.configIndex++;
            }

            // Set next config routine
            this.currentConfig = this.tripConfigs[this.configIndex];
            this.scriptConfig.setPhase(this.currentConfig.getName());
        }
    }
    //endregion

}