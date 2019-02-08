package cazador.furnaceoverhaul.tile;

public class TileEntityEmeraldFurnace extends TileEntityIronFurnace {

	@Override
	protected int getEnergyUse() {
		return 300;
	}

	@Override
	protected int getDefaultCookTime() {
		return 60;
	}

	@Override
	protected int getEfficientCookTime() {
		return 30;
	}

}