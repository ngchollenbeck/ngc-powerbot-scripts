package shared.action_config;

import org.powerbot.script.rt4.ClientContext;
import shared.actions.ToggleLevelUp;
import shared.actions.ToggleRunAction;
import shared.tools.GuiHelper;

import java.awt.*;

/**
 * Script config to manage default behavior for all bots.
 * Runs default actions
 * Handles default GUI
 * Setup for event handling and xp tracking
 */
public class ScriptConfig {

    // Powerbot
    ClientContext ctx;

    // GUI
    private String phase;
    private String step;
    private int levelsGained = 0;
    private Rectangle invRect = new Rectangle();


    // DEFAULT TASKS
    private final ToggleLevelUp toggleLevelUp;
    private final ToggleRunAction toggleRunAction;

    // CONSTRUCTOR
    public ScriptConfig(ClientContext ctx) {
        this.ctx = ctx;

        // Default Properties
        this.phase = "Start";
        this.step = "Config";
        this.ctx.properties.setProperty("randomevents.disable", "true");

        // Action Setup
        this.toggleLevelUp = new ToggleLevelUp(this.ctx);
        this.toggleRunAction = new ToggleRunAction(this.ctx, "Run", 30);

        this.invRect = ctx.widgets.component(162, 0).boundingRect();

    }


    // Pre-Poll Actions
    public void prePollAction() {

        // Reset any errant inventory selection
        if (ctx.inventory.selectedItem().valid()) {
            ctx.inventory.selectedItem().click(); // unselect itself
        }

        this.checkMiscState();
    }

    public void paint(Graphics g) {

        g.setColor(GuiHelper.getBaseColor());
        g.fillRoundRect(invRect.x, invRect.y, invRect.width, invRect.height, 4, 4);
        g.setColor(GuiHelper.getTextColorWhite());
        g.drawRoundRect(invRect.x, invRect.y, invRect.width, invRect.height, 4, 4);

        // Default Paint
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
    }

    // Check any non-phase STATE
    private void checkMiscState() {
        if (this.toggleLevelUp.activate()) {
            this.step = "Level Up";
            this.levelsGained++;
            this.toggleLevelUp.execute();
        }

        if (this.toggleRunAction.activate()) {
            this.step = "Running";
            this.toggleRunAction.execute();
        }
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public void incrementLevelsGained() {
        this.levelsGained++;
    }

    public int paintLineY(int line){
        return invRect.y + (line * 20);
    }
    public int paintLineX(){
        return invRect.x + 10;
    }
    public int paintLineXMiddle(){
        return invRect.x + (invRect.width/2) + 10;
    }

    public boolean levelUpPromptVisible(){
        return this.toggleLevelUp.activate();
    }
}
