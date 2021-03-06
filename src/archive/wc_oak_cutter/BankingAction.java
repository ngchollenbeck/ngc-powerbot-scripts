package scripts.wc_oak_cutter;


import shared.constants.Items;
import shared.templates.AbstractAction;
import shared.tools.CommonAreas;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;

import java.util.concurrent.Callable;

public class BankingAction extends AbstractAction<ClientContext> {
    private Area geArea = CommonAreas.getGeAreaEast();


    public BankingAction(ClientContext ctx) {
        super(ctx, "Banking");
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public void execute() {
        if( ctx.bank.inViewport() ) {
            ctx.bank.open();



            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.bank.opened();
                }
            }, 150, 4);

            if(ctx.bank.withdrawModeQuantity() != Bank.Amount.ALL){
                ctx.bank.withdrawModeQuantity(Bank.Amount.ALL);
            }
            ctx.bank.deposit(Items.OAK_LOGS_1521, Bank.Amount.ALL);


            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ctx.inventory.count() == 0;
                }
            }, 150, 4);


        }
    }
}
