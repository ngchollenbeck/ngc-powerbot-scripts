package scripts.nmz;

import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Equipment;
import org.powerbot.script.rt4.Game;
import shared.actions.GuzzleRockCake;
import shared.actions.InteractWithGameObject;
import shared.actions.UsePotion;
import shared.constants.Items;
import shared.tools.AntibanTools;
import shared.tools.GaussianTools;
import shared.tools.GuiHelper;

import java.awt.*;

import static org.powerbot.script.Condition.sleep;

@Script.Manifest(name = "Nightmare Zone", description = "Uses Absorption & Overloads at NMZ", properties = "client=4")
public class _NMZ extends PollingScript<ClientContext> implements PaintListener, MessageListener {

    // Task Tracking
    private UsePotion overloadPotion;
    private UseAbsorptionPotion absorptionPotion;
    private GuzzleRockCake guzzleRockCake;
    private InteractWithGameObject recurrentDamagePowerUp;
    private InteractWithGameObject zapperPowerUp;
    private boolean overloadRequired;
    private boolean guzzling;

//    private InteractWithGameObject powerSurgePowerUp;

    // Power Surge
    private boolean powerSurgeActive;
    private int mainHandWep;
    private int offhandWep;

    // GUI
    private String status;
    private Long lastOverloadTimestamp;
    private int lastGuzzleAttemptHP;
    private long lastGuzzleAttemptTimestamp;

    private Rectangle invRect;

    private long nextBreak;

    private final int[] overloadIds = new int[]{Items.OVERLOAD_1_11733, Items.OVERLOAD_2_11732, Items.OVERLOAD_3_11731, Items.OVERLOAD_4_11730};

    @Override
    public void start() {
        this.invRect = ctx.widgets.component(7, 0).boundingRect();

        this.overloadRequired = ctx.combat.health() > 50;

        // Overload Potion (might ignore for now)
        overloadPotion = new UsePotion(ctx, "Overload", overloadIds, Constants.SKILLS_HITPOINTS, 50, 52, false);

        // Absorption Potion
        absorptionPotion = new UseAbsorptionPotion(ctx);

        // Guzzle Rock Cake
        guzzleRockCake = new GuzzleRockCake(ctx, "Guzzle");

        //  Recurrent Damage Power up
        recurrentDamagePowerUp = new InteractWithGameObject(ctx, "Power Up", "Activate", 26265);

        // Zapper Power UP
        zapperPowerUp = new InteractWithGameObject(ctx, "Power Up", "Activate", 26256);

        // Power Surge
//        powerSurgePowerUp = new InteractWithGameObject(ctx, "Power Up", "Activate", 26264);

        lastOverloadTimestamp = 0L;
        lastGuzzleAttemptTimestamp = 0L;
        lastGuzzleAttemptHP = 1;

        nextBreak = (long) AntibanTools.getRandomInRange(0, 5) + 3600;

        mainHandWep = ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id();
        offhandWep = ctx.equipment.itemAt(Equipment.Slot.OFF_HAND).id();
    }

    @Override
    public void poll() {

        // Antiban Check
        switch (checkState()) {
            case GUZZLE:
                status = guzzleRockCake.getStatus();
//                sleep(Random.nextInt(1000, 50000));
                guzzleRockCake.execute();
                if(ctx.combat.health() == 1) {
                    lastGuzzleAttemptTimestamp = getRuntime();
                    this.lastGuzzleAttemptHP = 1;
                    this.guzzling = false;
                }
                break;
            case OVERLOAD:
                status = overloadPotion.getStatus();
                lastOverloadTimestamp = getRuntime();
                overloadPotion.execute();
                this.overloadRequired = false;
                break;
            case ABSORPTION:
                status = absorptionPotion.getStatus();
                absorptionPotion.execute();
                break;
            case RECURRENT_DAMAGE_POWER_UP:
                status = recurrentDamagePowerUp.getStatus();
                recurrentDamagePowerUp.execute();
                break;
            case ZAPPER_POWER_UP:
                status = zapperPowerUp.getStatus();
                zapperPowerUp.execute();
                break;
//            case POWER_SURGE_POWER_UP:
//                status = powerSurgePowerUp.getStatus();
//                equipPowerSurgeEquipment();
//                powerSurgePowerUp.execute();
//                break;
//            case POWER_SURGE_ACTIVE:
//                status = "Power Surge";
//                usePowerSurge();
//                break;
            default:
                if (getRuntime() > nextBreak) {
                    if (GaussianTools.takeActionNormal()) {
                        status = "Antiban";
                        AntibanTools.runCommonAntiban(ctx);
                    }
                    this.nextBreak = getRuntime() + (AntibanTools.getRandomInRange(4, 9) * 3600);
                } else {
                    status = "Waiting";
                }
        }
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
        g.drawString("Runtime: " + GuiHelper.getReadableRuntime(getRuntime()), this.invRect.x + 15, this.invRect.y + 40);

        g.setColor(GuiHelper.getTextColorInformation());
        g.drawString("Guzzled: " + GuiHelper.getReadableRuntime(this.lastGuzzleAttemptTimestamp), this.invRect.x + 15, this.invRect.y + 70);
        g.drawString("Overloaded: " + GuiHelper.getReadableRuntime(this.lastOverloadTimestamp), this.invRect.x + 15, this.invRect.y + 90);

        g.setColor(GuiHelper.getTextColorImportant());
        g.drawString("DEF: " + ctx.skills.level(Constants.SKILLS_DEFENSE), this.invRect.x + 15, this.invRect.y + 120);
//        g.drawString("Last Overload : " + GuiHelper.getReadableRuntime(getRuntime() - lastOverloadTimestamp), GuiHelper.getDialogStartX(), GuiHelper.getStartY(4));

    }

    private boolean getGuzzleChance() {
        switch (ctx.combat.health()) {
            case 1:
                return false; //Never Guzzle On 1HP
            case 2:
                return GaussianTools.takeActionNormal();
            case 3:
                return GaussianTools.takeActionLikely();
            case 4:
                return !GaussianTools.takeActionRarely();
            case 5:
                return !GaussianTools.takeActionNever();
            default:
                return true;
        }
    }

    @Override
    public void messaged(MessageEvent messageEvent) {
        String msg = messageEvent.text().toLowerCase();

        if (msg.contains("worn off")) {
            this.overloadRequired = true;
        }
        if (msg.contains("drink some of your overload potion")) {
            this.overloadRequired = false;
        }

        // Ensures this doesn't have to be checked too often since messaged fires on callback
        this.invRect = ctx.widgets.component(7, 0).boundingRect();


    }

    private enum State {
        OVERLOAD, ABSORPTION, GUZZLE, ZAPPER_POWER_UP, RECURRENT_DAMAGE_POWER_UP, POWER_SURGE_POWER_UP, POWER_SURGE_ACTIVE, WAITING
    }

    private State checkState() {

        // Overload
        if (overloadRequired && ctx.combat.health() > 50) {
            return State.OVERLOAD;
        }

        // Absorption
        if (absorptionPotion.activate() && ctx.inventory.select().id(this.overloadIds).count() > 0 && ctx.combat.health() < 10) {
            return State.ABSORPTION;
        }

        // Guzzle Rock Cake if HP in range && not overloading && time elapsed since last check decreasing as hp rises.
        if (!this.overloadRequired && (this.guzzling || (ctx.combat.health() != lastGuzzleAttemptHP) || (ctx.combat.health() > 4))) {
            if (guzzleRockCake.activate() && getGuzzleChance()) {
                sleep(Random.nextInt(100, 18000)); // immediately to 18 seconds in the future
                this.guzzling = true;
                return State.GUZZLE;
            } else {
                lastGuzzleAttemptHP = ctx.combat.health();
            }
        }

        // Recurrent Damage
        if (recurrentDamagePowerUp.activate()) {
            return State.RECURRENT_DAMAGE_POWER_UP;
        }

        // Zapper
        if (zapperPowerUp.activate()) {
            return State.ZAPPER_POWER_UP;
        }
//
//        // Power Surge power up
//        if( powerSurgePowerUp.activate() && ctx.inventory.select().id(Items.GRANITE_MAUL_4153).poll().valid() ) {
//            return State.POWER_SURGE_POWER_UP;
//        }

        // POwer surge spec
//        if( powerSurgeActive ) {
//            return State.POWER_SURGE_ACTIVE;
//        }

        return State.WAITING;
    }

    private void unequipPowerSurgeEqupment() {
        // Unequip on Power Surge done
        ctx.game.tab(Game.Tab.INVENTORY);
        ctx.inventory.select().id(mainHandWep).poll().interact("Wield");
        sleep();
        ctx.inventory.select().id(offhandWep).poll().interact("Wield");
    }

    private void equipPowerSurgeEquipment() {
        // Equip on Power Surge
        ctx.game.tab(Game.Tab.INVENTORY);
        ctx.inventory.select().id(Items.GRANITE_MAUL_4153).poll().interact("Wield");
        sleep();
    }

    private void usePowerSurge() {
        // Use Power Surge Spec
        if (ctx.combat.specialPercentage() >= 50) {
            ctx.widgets.component(160, 32).click(); // newly updated icon for spec attack
            sleep();
        }
    }
}
