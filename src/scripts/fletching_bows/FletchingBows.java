package scripts.fletching_bows;

import org.powerbot.script.*;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Item;
import shared.action_config.ScriptConfig;
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

@Script.Manifest(name = "FletchingBows", description = "FletchingBows", properties = "client=4; topic=051515; author=Bowman")
public class FletchingBows extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private final ScriptConfig scriptConfig = new ScriptConfig(ctx);
    private final int knifeId = Items.KNIFE_946;
    private int logId;
    private String actionKey;

    //region Antiban
    private long nextBreak;
    //endregion

    //region Phase
    private enum Phase {
        Start, Fletching, Banking
    }

    private Phase currentPhase;
    //endregion

    //region start
    @Override
    public void start() {
        this.scriptConfig.setPhase("Start");

        this.logId = ctx.inventory.select().select(new Filter<Item>() {
            @Override
            public boolean accept(Item item) {
                return item.name().toLowerCase().contains("log");
            }
        }).poll().id();

        nextBreak = (long) AntibanTools.getRandomInRange(4, 8) * 60000;
        this.currentPhase = Phase.Start;
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
        if (!ctx.controller.isSuspended()) {
            this.scriptConfig.paint(g);

            g.drawString("Phase  : " + (this.scriptConfig.getPhase()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(1));
            g.drawString("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(2));
            g.drawString("Level: " + ctx.skills.realLevel(Constants.SKILLS_FLETCHING), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(4));
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
                if (ctx.inventory.select().id(logId).count() > 0)
                    return Phase.Fletching;
                else {
                    return Phase.Banking;
                }
            case Fletching:
                if (ctx.inventory.select().id(logId).count() == 0) {
                    return Phase.Banking;
                }
                break;
            case Banking:
                if (ctx.inventory.select().id(logId).count() > 0) {
                    return Phase.Fletching;
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
            case Fletching:
                // Select Knife
                ctx.inventory.select().id(knifeId).poll().click();
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.inventory.selectedItem().valid();
                    }
                }, 100, 10);

                // Use knife on log
                ctx.inventory.select().id(logId).first().poll().click();
                // Send action key
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.widgets.widget(270).component(0).visible(); //Wait for prompt
                    }
                }, Random.nextInt(100, 500), 10);


                if (ctx.widgets.widget(270).component(0).visible()) {
                    sleep(Random.nextInt(300, 1000));
                    ctx.input.send(" ");

                    if (GaussianTools.takeActionNormal()) {
                        AntibanTools.moveMouseOffScreen(ctx, false);
                    }

                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.inventory.select().id(logId).count() == 0 || scriptConfig.levelUpPromptVisible();
                        }
                    }, Random.nextInt(1111, 3500), 60);
                }
                break;
            case Banking:
                if (CommonActions.openBank(ctx)) {
                    if (ctx.bank.select().id(logId).count() == 0) {
                        ctx.controller.stop();
                    }
                    if (CommonActions.depositAllExcept(ctx, new int[]{knifeId})) {
                        if (CommonActions.withdrawItem(ctx, logId, Bank.Amount.ALL, true)) {
                            CommonActions.closeBank(ctx);
                        }
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
            AntibanTools.runCommonAntiban(ctx);
        }
        this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(4, 17) * 60000);
    }

    private boolean activateAntiban() {
        return (getRuntime() > nextBreak);
    }
    //endregion


    //region Phase

    //endregion


    //region Helpers

    //endregion
}