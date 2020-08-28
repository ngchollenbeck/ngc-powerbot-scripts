package scripts.magic_high_alch;

import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Item;
import shared.actions.HighAlch;
import shared.constants.Items;
import shared.tools.GuiHelper;

import java.awt.*;

@Script.Manifest(name = "Alchmate", description = "High Alch on noted items", properties = "client=4")
public class _HighAlch extends PollingScript<ClientContext> implements PaintListener {

    // Task List
    private HighAlch highAlch;

    // GUI Tracking
    private String status;
    private Rectangle invRect;


    @Override
    public void start() {

        int[] alchables = new int[ctx.inventory.size()];
        int index = 0;
        for( Item i : ctx.inventory.select() ) {
            if(i.id() != Items.NATURE_RUNE_561 && i.id() != Items.COINS_995){
                alchables[index] = i.id();
                index++;
            }
        }

        this.highAlch = new HighAlch(ctx, "Alch", alchables, true,false);
    }

    @Override
    public void poll() {
        this.invRect = ctx.widgets.component(7, 0).boundingRect();

            if( highAlch.activate() ) {
                status = highAlch.getStatus();
                highAlch.execute();
            }


        status = "Waiting";
    }

    @Override
    public void repaint(Graphics g) {

        g.setColor(GuiHelper.getBaseColor());
        g.fillRoundRect(this.invRect.x, this.invRect.y, this.invRect.width, this.invRect.height, 4, 4);
        g.setColor(GuiHelper.getTextColorWhite());
        g.drawRoundRect(this.invRect.x, this.invRect.y, this.invRect.width, this.invRect.height, 4, 4);

        // Default Paint
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        g.drawString("Status : " + (this.status), this.invRect.x + 15, this.invRect.y + 20);
        g.drawString("Runtime : " + GuiHelper.getReadableRuntime(getRuntime()), this.invRect.x + 15, this.invRect.y + 50);
    }
}
