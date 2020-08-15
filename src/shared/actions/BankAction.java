package shared.actions;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Random;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;
import shared.templates.StructuredAction;

import java.util.concurrent.Callable;

import static org.powerbot.script.Condition.sleep;

/**
 * Withdrawal and Deposit actions within bank window context.
 */
public class BankAction extends StructuredAction {
    private int primaryDepositId;
    private int secondaryDepositId;

    private int primaryWithdrawId;
    private int primaryWithdrawQty;

    private int secondaryWithdrawId;
    private int secondaryWithdrawQty;

    private boolean bankOnInventoryFull; // Bank on full inventory
    private boolean bankOnWithdrawsEmpty; // Bank when withdrawn items empty (i.e. smelting or crafting one item into another)
    private boolean closeWhenDone;

    private Area bankArea;

    public BankAction(ClientContext ctx, String status) {
        super(ctx, status);

        this.primaryDepositId = 0;
        this.secondaryDepositId = 0;
        this.primaryWithdrawId = 0;
        this.primaryWithdrawQty = 0;
        this.secondaryWithdrawId = 0;
        this.secondaryWithdrawQty = 0;
        this.bankOnInventoryFull = false;
        this.bankOnWithdrawsEmpty = false;
        this.closeWhenDone = false;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    public BankAction(ClientContext ctx, String status, int primaryDepositId, int secondaryDepositId, int primaryWithdrawId, int primaryWithdrawQty, int secondaryWithdrawId, int secondaryWithdrawQty, boolean bankOnInventoryFull, boolean bankOnWithdrawsEmpty, boolean closeWhenDone, Area bankArea) {
        super(ctx, status);
        this.primaryDepositId = primaryDepositId;
        this.secondaryDepositId = secondaryDepositId;
        this.primaryWithdrawId = primaryWithdrawId;
        this.primaryWithdrawQty = primaryWithdrawQty;
        this.secondaryWithdrawId = secondaryWithdrawId;
        this.secondaryWithdrawQty = secondaryWithdrawQty;
        this.bankOnInventoryFull = bankOnInventoryFull;
        this.bankOnWithdrawsEmpty = bankOnWithdrawsEmpty;
        this.closeWhenDone = closeWhenDone;
        this.bankArea = bankArea;
    }

    @Override
    public boolean activate() {
        boolean invFull = (isBankOnInventoryFull() && ctx.inventory.isFull());
        boolean resourcesEmpty = (isBankOnWithdrawsEmpty() && ctx.inventory.select().id(getAllWithdrawIds()).count() == 0);
        return (invFull || resourcesEmpty) && (getBankArea() != null || ctx.bank.inViewport());
    }

    @Override
    public void execute() {
        if (ctx.bank.inViewport()) {
            if (!ctx.bank.opened()) {
                // Open Bank
                ctx.bank.open();
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ctx.bank.opened();
                    }
                }, 150, 20);
            }

            if (ctx.bank.opened()) {
                // Deposit if needed
                if (getPrimaryDepositId() > -1) {
                    if (getPrimaryDepositId() == 0) {
                        ctx.bank.depositInventory();
                    } else {
                        ctx.bank.deposit(getPrimaryDepositId(), Bank.Amount.ALL);
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.inventory.select().id(getPrimaryDepositId()).count() == 0;
                            }
                        }, 150, 100);
                        ctx.bank.deposit(getSecondaryDepositId(), Bank.Amount.ALL);
                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.inventory.select().id(getSecondaryDepositId()).count() == 0;
                            }
                        }, 150, 100);
                    }
                }

                // Withdraw
                if (getPrimaryWithdrawId() > 0) {

                    if (ctx.bank.select().id(this::getPrimaryWithdrawId).poll().valid()) {

                        // Check Quantity
                        if (getPrimaryWithdrawQty() == 28) {
                            ctx.bank.withdraw(getPrimaryWithdrawId(), Bank.Amount.ALL);
                        } else {
                            if (getPrimaryWithdrawQty() > 0) {
                                ctx.bank.withdraw(getPrimaryWithdrawId(), Bank.Amount.X);
                            } else {
                                ctx.bank.select().id(getPrimaryWithdrawId()).poll().click(); // Uses selected qty
                            }
                        }
                    }

                    if (ctx.bank.select().id(this::getSecondaryWithdrawId).poll().valid()) {

                        // Check secondary withdraw if primary quantity less than full inventory
                        if (getSecondaryWithdrawId() > 0) {
                            ctx.bank.withdraw(getSecondaryDepositId(), Bank.Amount.ALL);//.select().id(getSecondaryWithdrawId()).poll().click(); // Uses X qty
                        }
                    }
                }

                // Close if needed
                if (isCloseWhenDone() && ctx.bank.open()) {
                    ctx.input.send("{VK_ESCAPE  down}");
                    sleep(Random.nextInt(400, 1200));
                    ctx.input.send("{VK_ESCAPE up}");
                }
            }
        }
    }

    // G&S

    public int getPrimaryDepositId() {
        return primaryDepositId;
    }

    public void setPrimaryDepositId(int primaryDepositId) {
        this.primaryDepositId = primaryDepositId;
    }

    public int getSecondaryDepositId() {
        return secondaryDepositId;
    }

    public void setSecondaryDepositId(int secondaryDepositId) {
        this.secondaryDepositId = secondaryDepositId;
    }

    public int getPrimaryWithdrawId() {
        return primaryWithdrawId;
    }

    public void setPrimaryWithdrawId(int primaryWithdrawId) {
        this.primaryWithdrawId = primaryWithdrawId;
    }

    public int getPrimaryWithdrawQty() {
        return primaryWithdrawQty;
    }

    public void setPrimaryWithdrawQty(int primaryWithdrawQty) {
        this.primaryWithdrawQty = primaryWithdrawQty;
    }

    public int getSecondaryWithdrawId() {
        return secondaryWithdrawId;
    }

    public void setSecondaryWithdrawId(int secondaryWithdrawId) {
        this.secondaryWithdrawId = secondaryWithdrawId;
    }

    public int getSecondaryWithdrawQty() {
        return secondaryWithdrawQty;
    }

    public void setSecondaryWithdrawQty(int secondaryWithdrawQty) {
        this.secondaryWithdrawQty = secondaryWithdrawQty;
    }

    public boolean isBankOnInventoryFull() {
        return bankOnInventoryFull;
    }

    public void setBankOnInventoryFull(boolean bankOnInventoryFull) {
        this.bankOnInventoryFull = bankOnInventoryFull;
    }

    public boolean isBankOnWithdrawsEmpty() {
        return bankOnWithdrawsEmpty;
    }

    public void setBankOnWithdrawsEmpty(boolean bankOnWithdrawsEmpty) {
        this.bankOnWithdrawsEmpty = bankOnWithdrawsEmpty;
    }

    public boolean isCloseWhenDone() {
        return closeWhenDone;
    }

    public void setCloseWhenDone(boolean closeWhenDone) {
        this.closeWhenDone = closeWhenDone;
    }


    public int[] getAllDepositIds() {
        int[] ids = {primaryDepositId, secondaryDepositId};
        return ids;
    }

    public int[] getAllWithdrawIds() {
        int[] ids = {primaryWithdrawId, secondaryWithdrawId};
        return ids;
    }


    public Area getBankArea() {
        return bankArea;
    }

    public void setBankArea(Area bankArea) {
        this.bankArea = bankArea;
    }
}
