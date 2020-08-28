package scripts.fletching_bolt_tips;


import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import shared.action_config.ScriptConfig;
import shared.actions.BankAction;
import shared.actions.CombineInventoryItems;
import shared.actions.LootAction;
import shared.constants.Items;
import shared.models.LootList;
import shared.tools.AntibanTools;
import shared.tools.GaussianTools;
import shared.tools.GuiHelper;

import java.awt.*;

@Script.Manifest(name = "Fletching - Bolt Tips", description = "Fletches bolt tips at a bank", properties = "client=4; topic=051515; author=Bowman")
public class FletchingBoltTips extends PollingScript<ClientContext> implements MessageListener, PaintListener {

    // Config
    private ScriptConfig scriptConfig = new ScriptConfig(ctx);

    private int gemId;

    private CombineInventoryItems combineItems;
    private BankAction bankAction;

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

        this.gemId = ctx.inventory.itemAt(27).id();

        this.combineItems = new CombineInventoryItems(ctx, Items.CHISEL_1755, this.gemId,true,88);
        this.bankAction = new BankAction(ctx, "Bank", 46, 0, Items.OYSTER_PEARL_411,-1,-1,-1,false,true,true,null);

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
                this.scriptConfig.setStep("Wait");
                AntibanTools.runCommonAntiban(ctx);
            }
            this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(9, 13) * 60000);
        } else {

            // Bank
            if(this.bankAction.activate()){
                this.scriptConfig.setPhase("Bank");
                this.scriptConfig.setStep("Inventory");
                this.bankAction.execute();
            }

            // Processes
            if(this.combineItems.activate()){
                this.scriptConfig.setPhase("Fletching");
                this.scriptConfig.setStep(("Bolt Tips"));
                this.combineItems.execute();
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
        g.drawString("Fletching : " + ctx.skills.realLevel(Constants.SKILLS_FLETCHING), this.scriptConfig.paintLineX(), this.scriptConfig.paintLineY(4));

    }
    //endregion
}