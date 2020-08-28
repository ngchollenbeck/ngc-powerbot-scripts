package scripts.mining_allminer;

import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import shared.actions.ShiftDropInventory;
import shared.constants.GameObjects;
import shared.constants.Items;
import shared.templates.AbstractAction;
import shared.tools.AntibanTools;
import shared.tools.CommonActions;
import shared.tools.GaussianTools;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Script.Manifest(name = "All Miner", description = "High Alch on noted items", properties = "client=4")
public class _AllMiner extends PollingScript<ClientContext> implements PaintListener, MessageListener {

    public List<AbstractAction> tasks = new ArrayList<>();
    public String status;

    // Ore Config
    public int oreId;
    public int rockId;
    public int[] allOreIds;

    private boolean playerIsMining;

    // Xp Config
    public double expGained;
    public int currentLevel;
    public int levelsGained;

    private ShiftDropInventory dropInventory;

    // GUI Counters
    public int oreMined;


    @Override
    public void start() {

        // Startup Config
        startup();

        // Location
        loadLocationConfig();

        // Go!
        dropInventory = new ShiftDropInventory(ctx);

    }

    @Override
    public void poll() {
        if (ctx.inventory.isFull()) {
            dropInventory.execute();
        } else if (ctx.objects.select().id(rockId).nearest().poll().inViewport() && !playerIsMining && !dropInventory.activate()) {
            AntibanTools.sleepDelay(AntibanTools.getRandomInRange(0, 4));

            var rock = ctx.objects.select().id(rockId).nearest().poll(); //5k
            

            rock.interact("Mine");

            Condition.wait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return playerIsMining;
                }
            }, 200, 10);

            if (GaussianTools.takeActionNormal()) {
                AntibanTools.moveMouseOffScreen(ctx, false);

                AntibanTools.sleepDelay(AntibanTools.getRandomInRange(0, 4));
            }
        }


    }

    @Override
    public void messaged(MessageEvent messageEvent) {
        String msg = messageEvent.text().toLowerCase();

        if (msg.contains("you swing your pick")) {
            this.playerIsMining = true;
        }

        if (msg.contains("you manage to mine some")) {
            this.playerIsMining = false;
        }
    }

    @Override
    public void repaint(Graphics graphics) {

    }

    /*Start Private Methods*/
    public void startup() {
        status = "Startup";

        currentLevel = ctx.skills.realLevel(Constants.SKILLS_MINING);
        expGained = 0;
        levelsGained = 0;

    }

    public void loadLocationConfig() {
        String location = CommonActions.promptForSelection("Location", "Location?", new String[]{"Barbarian Village", "Mining Guild", "Other"});

        if (location.equalsIgnoreCase("barbarian village")) {
            barbVillageConfig(new String[]{"Tin", "Coal"});
        } else {
            miningGuildConfig(new String[]{"Silver", "Gold", "Coal", "Mithril", "Adamant"});
        }
    }


    public void barbVillageConfig(String[] oreNames) {
        orePrompt(oreNames);

        // Load Barb Tasks

    }

    public void miningGuildConfig(String[] oreNames) {
        orePrompt(oreNames);

        // Load Guild Tasks

    }

    public void orePrompt(String[] oreNames) {
        String oreName = CommonActions.promptForSelection("Ore Selection", "Ore Selection", oreNames);

        switch (oreName) {
            case "Tin":
                oreId = Items.TIN_ORE_438;
                rockId = 11361;
                break;
            case "Coal":
                oreId = Items.COAL_453;
                rockId = GameObjects.COALROCK;
                break;
            case "Mithril":
                oreId = Items.MITHRIL_ORE_447;
                break;
            case "Adamantite":
                oreId = Items.ADAMANTITE_ORE_449;
                break;
            default:
                oreId = 0;
                break;
        }


    }

}
