package scripts.smithing_blast_furnace;

public class BlastTripConfig {
    private String name;

    //region Bank
    private int withdrawOreId;
    //endregion

    //region Bars
    private boolean collectBars;
    //endregion



    public BlastTripConfig(String name, int withdrawOreId, boolean collectBars) {
        this.name = name;
        this.withdrawOreId = withdrawOreId;
        this.collectBars = collectBars;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWithdrawOreId() {
        return withdrawOreId;
    }

    public void setWithdrawOreId(int withdrawOreId) {
        this.withdrawOreId = withdrawOreId;
    }

    public boolean isCollectBars() {
        return collectBars;
    }

    public void setCollectBars(boolean collectBars) {
        this.collectBars = collectBars;
    }
}
