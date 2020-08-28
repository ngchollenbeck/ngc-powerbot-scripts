package scripts.smithing_varrock_anvil;

import org.powerbot.script.*;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Item;
import shared.action_config.ScriptConfig;
import shared.actions.ToggleLevelUp;
import shared.constants.Items;
import shared.tools.AntibanTools;
import shared.tools.CommonActions;
import shared.tools.GaussianTools;
import shared.tools.GuiHelper;

import java.awt.*;
import java.util.concurrent.Callable;

import static org.powerbot.script.Condition.sleep;

/*
TODO:
- 
*/

@Script.Manifest(name = "VarrockSmither", description = "VarrockSmither", properties = "client=4; topic=051515; author=Bowman")
public class VarrockSmither extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private final ScriptConfig scriptConfig = new ScriptConfig(ctx);
    private int barId;
    private int targetWidget;
    private int targetComponent;
    private int barsUsed;
    private int barsToLevel;
    private int nextLevelXP;
    private long timeTillLevel;

    private Bank.Amount withdrawAmount;

    private int barsPerAction;
    private double xpPerBar;
    private String actionName;

    private final ToggleLevelUp toggleLevelUp = new ToggleLevelUp(ctx);

    //region Antiban
    private long nextBreak;
    //endregion

    //region Phase
    private enum Phase {
        Start, Smithing, Banking
    }

    private Phase currentPhase;
    //endregion


    //region start
    @Override
    public void start() {
        this.scriptConfig.setPhase("Start");

        this.targetWidget = 312;
        int subComponent = 2;
        this.barsUsed = 0;
        this.barsToLevel = 0;
        this.withdrawAmount = Bank.Amount.ALL;

        this.nextLevelXP = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_SMITHING) + 1);
        this.timeTillLevel = 0L;
        this.actionName = this.promptAnvilChoice();
        this.setAnvilWidget(actionName);

        this.barId = ctx.inventory.select().select(new Filter<Item>() {
            @Override
            public boolean accept(Item item) {
                return item.name().contains("bar");
            }
        }).first().poll().id();

        switch (barId) {
            case Items.STEEL_BAR_2353:
                this.xpPerBar = 37.5;
                break;
            case Items.MITHRIL_BAR_2359:
                this.xpPerBar = 50;
                break;
            default:
                this.xpPerBar = 40;
        }

        this.updateBarsToLevel();

        nextBreak = (long) AntibanTools.getRandomInRange(4, 15) * 60000;
        this.currentPhase = Phase.Start;
    }
    //endregion

    //region messaged
    @Override
    public void messaged(MessageEvent e) {
        String msg = e.text();

        if (msg.contains("You hammer the ")) {
            this.barsUsed++;

            updateBarsToLevel();
        }

        if (msg.contains("Congratulations, you've just advanced your")) {
            nextLevelXP = ctx.skills.experienceAt(ctx.skills.realLevel(Constants.SKILLS_SMITHING) + 1);
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

            g.setColor(GuiHelper.getTextColorImportant());
            g.drawString("Level  : " + ctx.skills.realLevel(Constants.SKILLS_SMITHING), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(4));
            g.drawString("Bars   : " + barsUsed, this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(5));
            g.drawString("TTL : " + barsToLevel + " | " + GuiHelper.getReadableRuntime(timeTillLevel), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(6));
            g.drawString("Action : " + actionName, this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(7));
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
                return Phase.Smithing;

            case Smithing:
                if (ctx.inventory.select().id(barId).count() == 0) {
                    return Phase.Banking;
                }
                break;
            case Banking:
                if (ctx.inventory.select().id(barId).count() > 0) {
                    return Phase.Smithing;
                }
        }

        return this.currentPhase;
    }

    private void executePhase() {
        this.currentPhase = checkNextPhase();

        switch (this.currentPhase) {
            case Start:
                break;

            case Smithing:
                this.scriptConfig.setPhase("Smithing");
                if (anvilInViewport()) {
                    smithAnvil();
                } else {
                    walkToAnvil();
                }
                break;

            case Banking:
                bankItemsWithdrawBars();
                break;
        }
    }
    //endregion

    //region Antiban
    private void executeAntiban() {
        if (GaussianTools.takeActionNormal()) {
            this.scriptConfig.setPhase("Antiban");
            this.scriptConfig.setStep("Wait");
            AntibanTools.runCommonAntiban(ctx);
        }
        this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(4, 17) * 60000);
    }

    private boolean activateAntiban() {
        return (getRuntime() > nextBreak);
    }
    //endregion


    //region Smithing
    public boolean anvilInViewport() {
        var anvil = ctx.objects.select().name("Anvil").nearest().poll();

        return (anvil.valid() && anvil.inViewport());
    }

    private void walkToAnvil() {
        var anvil = ctx.objects.select().name("Anvil").nearest().poll();

        ctx.movement.step(anvil);

        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return anvil.inViewport();
            }
        }, Random.nextInt(100, 200), 30);
    }

    public void smithAnvil() {
        var anvil = ctx.objects.select().name("Anvil").nearest().poll();

        if (anvil.valid()) {
            if (GaussianTools.takeActionUnlikely()) {
                // Misclick
                Tile t = anvil.tile();

                Point p = t.matrix(ctx).centerPoint();

                ctx.input.click(p.x + Random.nextInt(-250, 200), p.y + Random.nextInt(-200, 200), 1);

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !ctx.players.local().inMotion();
                    }
                }, 1000, 20);
                sleep();
            }
            if (!smithingWidgetVisible()) {
                anvil.interact("Smith");

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return smithingWidgetVisible();
                    }
                }, Random.nextInt(400, 900), 60);
            }

            if (smithingWidgetVisible()) {

                ctx.widgets.component(targetWidget, targetComponent).click();

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !smithingWidgetVisible();
                    }
                }, Random.nextInt(190, 390), 40);

                if (GaussianTools.takeActionLikely()) {
                    AntibanTools.moveMouseOffScreen(ctx, false);
                }

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.inventory.select().id(barId).count() == 0 || toggleLevelUp.activate();
                    }
                }, Random.nextInt(1900, 2500), 60);
            }
        }
    }

    private boolean smithingWidgetVisible() {
        return ctx.widgets.widget(targetWidget).component(targetComponent).visible();
    }

    public String promptAnvilChoice() {
        return CommonActions.promptForSelection("Smithing Choice", "What are ye smithin?", CommonActions.smithingOptions);
    }

    public void setAnvilWidget(String choice) {
        this.targetWidget = 312;

        switch (choice) {
            case "Dagger":
                this.targetComponent = 9;
                this.barsPerAction = 1;
                break;
            case "Sword":
                this.targetComponent = 10;
                this.barsPerAction = 1;
                break;
            case "Scimitar":
                this.targetComponent = 11;
                this.barsPerAction = 2;
                this.withdrawAmount = Bank.Amount.X;
                break;
            case "Long sword":
                this.targetComponent = 12;
                this.barsPerAction = 2;
                this.withdrawAmount = Bank.Amount.X;
                break;
            case "2-hand sword":
                this.targetComponent = 13;
                this.barsPerAction = 3;
                break;
            case "Axe":
                this.targetComponent = 14;
                this.barsPerAction = 1;
                break;
            case "Mace":
                this.targetComponent = 15;
                this.barsPerAction = 1;
                break;
            case "Warhammer":
                this.targetComponent = 16;
                this.barsPerAction = 3;
                break;
            case "Battleaxe":
                this.targetComponent = 17;
                this.barsPerAction = 3;
                break;
            case "Chain body":
                this.targetComponent = 19;
                this.barsPerAction = 3;
                break;
            case "Plate legs":
                this.targetComponent = 20;
                this.barsPerAction = 3;
                break;
            case "Plate skirt":
                this.targetComponent = 21;
                this.barsPerAction = 3;
                break;
            case "Plate body":
                this.targetComponent = 22;
                this.barsPerAction = 5;
                this.withdrawAmount = Bank.Amount.X;
                break;
            case "Nails":
                this.targetComponent = 23;
                this.barsPerAction = 1;
                break;
            case "Medium helm":
                this.targetComponent = 24;
                this.barsPerAction = 1;
                break;
            case "Full helm":
                this.targetComponent = 25;
                this.barsPerAction = 2;
                this.withdrawAmount = Bank.Amount.X;
                break;
            case "Square shield":
                this.targetComponent = 26;
                this.barsPerAction = 2;
                this.withdrawAmount = Bank.Amount.X;
                break;
            case "Kite shield":
                this.targetComponent = 27;
                this.barsPerAction = 1;
                break;
            case "Dart tips":
                this.targetComponent = 29;
                this.barsPerAction = 1;
                break;
            case "Arrowtips":
                this.targetComponent = 30;
                this.barsPerAction = 1;
                break;
            case "Knives":
                this.targetComponent = 31;
                this.barsPerAction = 1;
                break;
            case "Studs":
                this.targetComponent = 32;
                this.barsPerAction = 1;
                break;
            case "Javelin heads":
                this.targetComponent = 33;
                this.barsPerAction = 1;
                break;
            case "Bolts":
                this.targetComponent = 34;
                this.barsPerAction = 1;
                break;
            case "Limbs":
                this.targetComponent = 35;
                this.barsPerAction = 1;
                break;
        }

    }

    //endregion

    //region Banking
    public void bankItemsWithdrawBars() {
        if (CommonActions.openBank(ctx)) {
            if (ctx.bank.select().id(barId).count() == 0) {
                AntibanTools.moveMouseOffScreen(ctx, true);
                ctx.controller.stop();
            }

            if (ctx.inventory.select().id(barId).count() < barsPerAction) {
                if (CommonActions.depositAllExcept(ctx, new int[]{Items.HAMMER_2347})) {
                    CommonActions.withdrawItem(ctx, barId, withdrawAmount, true);
                }
            }
        }
    }


    //region Helpers
    private void updateBarsToLevel() {
        int currentXP = ctx.skills.experience(Constants.SKILLS_SMITHING);
        this.barsToLevel = (int) ((nextLevelXP - currentXP) / (barsPerAction * xpPerBar));
        this.timeTillLevel = barsToLevel * 3000;
    }
    //endregion
}