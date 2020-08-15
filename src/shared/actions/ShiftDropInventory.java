package shared.actions;

import org.powerbot.script.Random;
import org.powerbot.script.rt4.ClientContext;
import shared.templates.AbstractAction;

import static org.powerbot.script.Condition.sleep;

/**
 * Shift drop all inventory for power leveling
 */
public class ShiftDropInventory extends AbstractAction<ClientContext> {

    public ShiftDropInventory(ClientContext ctx) {
        super(ctx, "Drop");
    }

    @Override
    public boolean activate() {
        return ctx.inventory.isFull();
    }


    @Override
    public void execute() {
        dropInventory();
        ctx.input.send("{VK_SHIFT up}");

    }

    private void dropInventory() {

        int pattern = Random.nextInt(0, 100);

        if (pattern > 80) {
            int[] i = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28};
            runDropPattern(i);
        }
        if (pattern > 60) {
            int[] i = {1, 5, 9, 13, 17, 21, 25, 2, 6, 10, 14, 18, 22, 26, 3, 7, 11, 15, 19, 23, 27, 4, 8, 12, 16, 20, 24, 28};
            runDropPattern(i);
        }
        if (pattern > 40) {
            int[] i = {28, 24, 20, 16, 12, 8, 4, 27, 23, 19, 15, 11, 7, 3, 26, 22, 18, 14, 10, 6, 2, 25, 21, 17, 13, 9, 5, 1};
            runDropPattern(i);
        }
        if (pattern > 20) {
            int[] i = {1, 2, 5, 3, 6, 9, 4, 7, 10, 13, 17, 14, 11, 8, 12, 15, 18, 21, 25, 22, 19, 16, 20, 23, 26, 24, 27, 28};
            runDropPattern(i);
        }
        if (pattern > 0) {
            int[] i = {28, 27, 26, 25, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
            runDropPattern(i);
        }


    }

    private void runDropPattern(int[] indexes) {
        for (int index : indexes) {
            ctx.input.send("{VK_SHIFT down}");
            int itemIndex = index - 1;
            ctx.inventory.items()[itemIndex].click();
            sleep(Random.nextInt(100, 250));
        }
    }

}
